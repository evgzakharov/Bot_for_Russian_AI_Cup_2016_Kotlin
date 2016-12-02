import MapHelper.attackLines
import MapHelper.friendBasePoint
import MapHelper.mapLines
import MapHelper.wizard
import model.*
import java.lang.StrictMath.abs

data class BonusTimeCatch(var lastCatchTick: Int = 0)

enum class GlobalStrateg {
    DEFENCE, ATTACK
}

class StrategyManager {
    lateinit var self: Wizard
    lateinit var world: World
    lateinit var game: Game
    lateinit var move: Move
    lateinit var findHelper: FindHelper
    lateinit var moveHelper: MoveHelper

    var actionMode: ActionMode = ActionMode.ATTACK
        private set
    var movingTarget: Any? = null
    val defaulActionMode: ActionMode = ActionMode.ATTACK

    var laneType: LaneType? = null
    var lastLaneChangeTick: Int = 0

    var globalStrateg = GlobalStrateg.ATTACK

    private var gameManagers: MutableMap<ActionMode, Action> = mutableMapOf()

    private var bonuses: Map<Point2D, BonusTimeCatch> = sortedMapOf(
            Point2D(1200.0, 1200.0) to BonusTimeCatch(),
            Point2D(2800.0, 2800.0) to BonusTimeCatch()
    )

    fun nextTick(self: Wizard, world: World, game: Game, move: Move) {
        this.self = self
        this.world = world
        this.game = game
        this.move = move
        this.findHelper = FindHelper(world, game, self)
        this.moveHelper = MoveHelper(self, world, game, move)
        MapHelper.nextTick(world, game, self)

        initializeDefault()

        makeDecision()
    }

    private fun makeDecision() {
        try {
            checkSkills()

            updateBonusInfo()
            actionDecision()

            val needDefence = laneDefenceDecision()
            if (!needDefence) {
                globalStrateg = GlobalStrateg.ATTACK

                val attack = laneAttackDecision()
                if (attack) updateInfo(attack)
            } else {
                globalStrateg = GlobalStrateg.DEFENCE
            }

            action()
        } catch (e: Throwable) {
            moveHelper.goTo(friendBasePoint)
        }
    }

    private fun checkSkills() {
        if (!game.isSkillsEnabled) return

        val selfSkils = self.getSkills().toList()

        val skillsToLearn = skillesToLearnFrost
                .filter { selfSkils.isEmpty() || !selfSkils.contains(it) }
                .first()

        move.skillToLearn = skillsToLearn
    }

    private fun updateInfo(laneDecision: Boolean) {
        if (laneDecision) lastLaneChangeTick = world.tickIndex
    }

    private fun action() {
        val moveSuccess = if (globalStrateg == GlobalStrateg.ATTACK) {
            movingTarget?.let { move(it) } ?: false
        } else false

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
                .filter { self.getDistanceTo(it) < TRY_TO_CATCH_ARTIFACT_DISTANCE }
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
                .filter { self.getDistanceTo(it) < TRY_TO_CATCH_ARTIFACT_DISTANCE2 }
                .firstOrNull()

        val wizardOnArtifactLine = MapHelper.getLinePositions(wizard, 1.0)
                .find { it.mapLine.laneType == null }?.let { true } ?: false

        //TODO: add more creterias
        if (mayCatchBonus != null && wizardOnArtifactLine) {
            val wizardInEnemyLine = MapHelper.getLinePositions(self, 1.0)
                    .filter { it.mapLine.enemy }
                    .firstOrNull()

            if (wizardInEnemyLine == null || (wizardInEnemyLine.position < wizardInEnemyLine.mapLine.lineLength * LINE_POSITION_MULTIPLIER)) {
                actionMode = ActionMode.MOVE_TO_POINT
                movingTarget = mayCatchBonus
                return
            }
        }

        actionMode = ActionMode.ATTACK
        movingTarget = null
    }

    private fun updateBonusInfo() {
        world.getWizards().forEach { wizard ->
            bonuses.forEach { bonus ->
                if (wizard.radius + game.bonusRadius >= wizard.getDistanceTo(bonus.key))
                    bonus.value.lastCatchTick = world.tickIndex
            }
        }
    }

    private fun laneDefenceDecision(): Boolean {
        //defence
        val friendMostKillingLine = mapLines.shouldChangeLine(sortByEnemyTowers = false) { line ->
            line.enemy == false &&
                    ((line.deadFriendTowerCount >= 1
                            && line.enemyWizardPositions.isNotEmpty()
                            && (line.friendWizardPositions.values.all { it < line.lineLength * LINE_MIN_FACTOR } || line.friendWizardPositions.isEmpty()))
                            || (line.deadFriendTowerCount == 2 && line.enemyWizardPositions.isNotEmpty()))
        }
        if (friendMostKillingLine)
            return true

        return true
    }

    private fun laneAttackDecision(): Boolean {
        if (world.tickIndex < MIN_START_CHANGE_TICK)
            return false

        if (world.tickIndex - lastLaneChangeTick <= MIN_CHANGE_TICK_LIMIT)
            return false

        //or attack on line without wizards
        val lineWithoutFriendWizards = attackLines
                .mapValues { attackLine -> attackLine.value.friendWizards() }
                .filter { it.value.isEmpty() }.keys
                .firstOrNull()
                ?.let { true } ?: false

        if (lineWithoutFriendWizards)
            return true

        //or attack line, there are friend wizards less when enemy wizards
        val enemyMostKillingLine = mapLines.shouldChangeLine { line ->
            line.enemy == true
                    && attackLines[laneType]!!.friendWizards().size < line.enemyWizardPositions.size
                    && line.deadEnemyTowerCount > 0
        }
        if (enemyMostKillingLine)
            return true

        return false
    }

    private fun AttackLine.friendWizards(): Set<Long> {
        return this.friend.friendWizardPositions.keys + this.enemy.friendWizardPositions.keys
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
        gameManagers.put(ActionMode.MOVE_TO_POINT, MoveToPointAction())
    }

    private fun BonusTimeCatch.tickDiff() = abs(world.tickIndex - this.lastCatchTick)

    companion object {
        const val MY_HP_MULTIPLIER: Double = 0.8
        const val TRY_TO_KILL_ENEMY_RADIUS: Double = 500.0

        const val TRY_TO_CATCH_ARTIFACT_DISTANCE: Double = 150.0
        const val TRY_TO_CATCH_ARTIFACT_DISTANCE2: Double = 1500.0

        const val BONUS_UPDATE_TICK: Int = 2500

        const val MIN_START_CHANGE_TICK: Int = 250
        const val MIN_CHANGE_TICK_LIMIT: Int = 1500

        const val LINE_POSITION_MULTIPLIER: Double = 0.2

        const val LINE_MIN_FACTOR: Double = 0.3

        val skillesToLearnFrost: List<SkillType> = listOf(
                SkillType.MAGICAL_DAMAGE_BONUS_PASSIVE_1,
                SkillType.MAGICAL_DAMAGE_BONUS_AURA_1,
                SkillType.MAGICAL_DAMAGE_BONUS_PASSIVE_2,
                SkillType.MAGICAL_DAMAGE_BONUS_AURA_2,
                SkillType.FROST_BOLT,
                SkillType.STAFF_DAMAGE_BONUS_PASSIVE_1,
                SkillType.STAFF_DAMAGE_BONUS_AURA_1,
                SkillType.STAFF_DAMAGE_BONUS_PASSIVE_2,
                SkillType.STAFF_DAMAGE_BONUS_AURA_2,
                SkillType.FIREBALL,
                SkillType.MAGICAL_DAMAGE_ABSORPTION_PASSIVE_1,
                SkillType.MAGICAL_DAMAGE_ABSORPTION_AURA_1,
                SkillType.MAGICAL_DAMAGE_ABSORPTION_PASSIVE_2,
                SkillType.MAGICAL_DAMAGE_ABSORPTION_AURA_2,
                SkillType.SHIELD,
                SkillType.MOVEMENT_BONUS_FACTOR_PASSIVE_1,
                SkillType.MOVEMENT_BONUS_FACTOR_AURA_1,
                SkillType.MOVEMENT_BONUS_FACTOR_PASSIVE_2,
                SkillType.MOVEMENT_BONUS_FACTOR_AURA_2,
                SkillType.RANGE_BONUS_PASSIVE_1,
                SkillType.RANGE_BONUS_AURA_1,
                SkillType.RANGE_BONUS_PASSIVE_2,
                SkillType.RANGE_BONUS_AURA_2,
                SkillType.ADVANCED_MAGIC_MISSILE,
                SkillType.HASTE
        )

        val skillesToLearnFire: List<SkillType> = listOf(
                SkillType.STAFF_DAMAGE_BONUS_PASSIVE_1,
                SkillType.STAFF_DAMAGE_BONUS_AURA_1,
                SkillType.STAFF_DAMAGE_BONUS_PASSIVE_2,
                SkillType.STAFF_DAMAGE_BONUS_AURA_2,
                SkillType.FIREBALL,
                SkillType.MAGICAL_DAMAGE_BONUS_PASSIVE_1,
                SkillType.MAGICAL_DAMAGE_BONUS_AURA_1,
                SkillType.MAGICAL_DAMAGE_BONUS_PASSIVE_2,
                SkillType.MAGICAL_DAMAGE_BONUS_AURA_2,
                SkillType.FROST_BOLT,
                SkillType.MAGICAL_DAMAGE_ABSORPTION_PASSIVE_1,
                SkillType.MAGICAL_DAMAGE_ABSORPTION_AURA_1,
                SkillType.MAGICAL_DAMAGE_ABSORPTION_PASSIVE_2,
                SkillType.MAGICAL_DAMAGE_ABSORPTION_AURA_2,
                SkillType.SHIELD,
                SkillType.MOVEMENT_BONUS_FACTOR_PASSIVE_1,
                SkillType.MOVEMENT_BONUS_FACTOR_AURA_1,
                SkillType.MOVEMENT_BONUS_FACTOR_PASSIVE_2,
                SkillType.MOVEMENT_BONUS_FACTOR_AURA_2,
                SkillType.RANGE_BONUS_PASSIVE_1,
                SkillType.RANGE_BONUS_AURA_1,
                SkillType.RANGE_BONUS_PASSIVE_2,
                SkillType.RANGE_BONUS_AURA_2,
                SkillType.ADVANCED_MAGIC_MISSILE,
                SkillType.HASTE
        )

        val skillesToLearnShield: List<SkillType> = listOf(
                SkillType.MAGICAL_DAMAGE_ABSORPTION_PASSIVE_1,
                SkillType.MAGICAL_DAMAGE_ABSORPTION_AURA_1,
                SkillType.MAGICAL_DAMAGE_ABSORPTION_PASSIVE_2,
                SkillType.MAGICAL_DAMAGE_ABSORPTION_AURA_2,
                SkillType.SHIELD,
                SkillType.STAFF_DAMAGE_BONUS_PASSIVE_1,
                SkillType.STAFF_DAMAGE_BONUS_AURA_1,
                SkillType.STAFF_DAMAGE_BONUS_PASSIVE_2,
                SkillType.STAFF_DAMAGE_BONUS_AURA_2,
                SkillType.FIREBALL,
                SkillType.MAGICAL_DAMAGE_BONUS_PASSIVE_1,
                SkillType.MAGICAL_DAMAGE_BONUS_AURA_1,
                SkillType.MAGICAL_DAMAGE_BONUS_PASSIVE_2,
                SkillType.MAGICAL_DAMAGE_BONUS_AURA_2,
                SkillType.FROST_BOLT,
                SkillType.MOVEMENT_BONUS_FACTOR_PASSIVE_1,
                SkillType.MOVEMENT_BONUS_FACTOR_AURA_1,
                SkillType.MOVEMENT_BONUS_FACTOR_PASSIVE_2,
                SkillType.MOVEMENT_BONUS_FACTOR_AURA_2,
                SkillType.RANGE_BONUS_PASSIVE_1,
                SkillType.RANGE_BONUS_AURA_1,
                SkillType.RANGE_BONUS_PASSIVE_2,
                SkillType.RANGE_BONUS_AURA_2,
                SkillType.ADVANCED_MAGIC_MISSILE,
                SkillType.HASTE
        )
    }
}


