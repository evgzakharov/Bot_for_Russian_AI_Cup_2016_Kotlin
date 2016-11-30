import MapHelper.Companion.attackLines
import MapHelper.Companion.enemyBasePoint
import MapHelper.Companion.friendBasePoint
import MapHelper.Companion.mapLines
import model.*
import java.lang.StrictMath.abs

import java.util.Comparator
import java.util.HashMap

data class BonusTimeCatch(var lastCatchTick: Int = 0)

class StrategyManager {

    lateinit var self: Wizard
    lateinit var world: World
    lateinit var game: Game
    lateinit var move: Move
    lateinit var findHelper: FindHelper

    var actionMode: ActionMode = ActionMode.ATTACK
        private set
    var movingTarget: Any? = null
    val defaulActionMode: ActionMode = ActionMode.ATTACK

    var laneType: LaneType? = null
    var lastLaneChangeTick: Int = 0

    private var gameManagers: MutableMap<ActionMode, Action> = mutableMapOf()

    private var bonuses: Map<Point2D, BonusTimeCatch> = mapOf(
            Point2D(1200.0, 1200.0) to BonusTimeCatch(),
            Point2D(2800.0, 2800.0) to BonusTimeCatch()
    )

    fun nextTick(self: Wizard, world: World, game: Game, move: Move) {
        this.self = self
        this.world = world
        this.game = game
        this.move = move
        this.findHelper = FindHelper(world, game, self)

        initializeDefault()

        makeDecision()
    }

    private fun makeDecision() {
        updateBonusInfo()
        actionDecision()

        val laneDecision = laneDecision()
        updateInfo(laneDecision)

        action()
    }

    private fun updateInfo(laneDecision: Boolean) {
        if (laneDecision) lastLaneChangeTick = game.tickCount
    }

    private fun action() {
        val moveSuccess = movingTarget?.let { move(it) } ?: false
        if (!moveSuccess) {
            actionMode = defaulActionMode
            movingTarget = null
            move(movingTarget)
        }
    }

    private fun move(point: Any?): Boolean {
        val actionManager = gameManagers[actionMode]!!
        actionManager.init(self, world, game, move, this)
        return actionManager.move(point)
    }

    private fun actionDecision() {
        val enemyToKill = findHelper.getAllWizards(onlyEnemy = true, onlyNearest = true)
                .filter { self.life * MY_HP_MULTIPLIER >= it.life }
                .filter { self.getDistanceTo(it) < TRY_TO_KILL_ENEMY_RADIUS }
                .sortedByDescending { it.life }
                .firstOrNull()

        if (enemyToKill != null) {
            actionMode = ActionMode.KILL_ENEMY
            movingTarget = enemyToKill
            return
        }

        val nearestArtifact = world.getBonuses()
                .filter { self.getDistanceTo(it) < TRY_TO_CATCH_ARTIFACT }
                .sortedByDescending { self.getDistanceTo(it) }
                .firstOrNull()

        if (nearestArtifact != null) {
            actionMode = ActionMode.MOVE_TO_POINT
            movingTarget = nearestArtifact.toPoint()
            return
        }

        val mayCatchBonus = bonuses
                .filter { it.value.tickDiff() >= BONUS_UPDATE_TICK }
                .keys
                .sortedByDescending { self.getDistanceTo(it) }
                .firstOrNull()

        if (mayCatchBonus != null) {
            actionMode = ActionMode.MOVE_TO_POINT
            movingTarget = mayCatchBonus
            return
        }
    }

    private fun updateBonusInfo() {
        world.getWizards().forEach { wizard ->
            bonuses.forEach { bonus ->
                if (wizard.radius + game.bonusRadius <= wizard.getDistanceTo(bonus.key))
                    bonus.value.lastCatchTick = game.tickCount
            }
        }
    }

    private fun laneDecision(): Boolean {
        if (game.tickCount < MIN_CHANGE_TICK_LIMIT) return false

        //defence
        val friendMostKillingLine = mapLines.shouldChangeLine(sortByEnemyTowers = false) {
            it.enemy == false && it.deadFriendTowerCount == 2 && it.enemyWizardPositions.isNotEmpty()
        }
        if (friendMostKillingLine)
            return true

        if (game.tickCount - lastLaneChangeTick <= MIN_CHANGE_TICK_LIMIT)
            return false

        //or attack on line without wizards
        val lineWithoutFriendWizards = attackLines.mapValues { attackLine ->
            attackLine.value.fold(0) { sum, value -> sum + value.friendWizardPositions.size}
        }.filter { it.value == 0 }.keys.firstOrNull()?.let { true } ?: false

        if (lineWithoutFriendWizards)
            return true

        //or attack line, there are friend wizards less when enemy wizards
        val enemyMostKillingLine = mapLines.shouldChangeLine { line ->
            line.enemy == true && line.friendWizardsOnLine() < line.enemyWizardPositions.size && line.deadEnemyTowerCount > 0
        }
        if (enemyMostKillingLine)
            return true

        return false
    }

    private fun List<MapLine>.shouldChangeLine(sortByEnemyTowers: Boolean = true, criteria: (MapLine) -> Boolean): Boolean {
        return this.filter { criteria(it) }
                .sortedByDescending { if (sortByEnemyTowers) it.deadEnemyTowerCount else it.deadFriendTowerCount }
                .firstOrNull()?.run { this@StrategyManager.laneType = this.laneType; true } ?: false
    }


    private fun MapLine.friendWizardsOnLine(): Int {
        if (laneType == this.laneType)
            return this.friendWizardPositions.count() + 1
        else
            return this.friendWizardPositions.count()
    }

    private fun initializeDefault() {
        if (laneType == null) {
            when (self.id.toInt()) {
                1, 2, 6, 7 -> laneType = LaneType.TOP
                3, 8 -> laneType = LaneType.MIDDLE
                4, 5, 9, 10 -> laneType = LaneType.BOTTOM
            }
        }

        gameManagers.put(ActionMode.ATTACK, AttackAction())
        gameManagers.put(ActionMode.KILL_ENEMY, KillEnemyAction())
        gameManagers.put(ActionMode.MOVE_TO_POINT, MoveToPoint())
    }

    private fun BonusTimeCatch.tickDiff() = abs(game.tickCount - this.lastCatchTick)

    companion object {
        const val MY_HP_MULTIPLIER: Double = 0.8
        const val TRY_TO_KILL_ENEMY_RADIUS: Double = 500.0
        const val TRY_TO_CATCH_ARTIFACT: Double = 700.0

        const val BONUS_UPDATE_TICK: Int = 2500

        const val MIN_CHANGE_TICK_LIMIT: Int = 2500
    }
}


