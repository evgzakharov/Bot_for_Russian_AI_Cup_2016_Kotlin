import MapHelper.Companion.mapLines
import model.*

import java.util.Comparator
import java.util.HashMap

class StrategyManager {

    lateinit var self: Wizard
    lateinit var world: World
    lateinit var game: Game
    lateinit var move: Move

    var laneType: LaneType? = null
    var actionMode: ActionMode = ActionMode.ATTACK
        private set

    private var gameManagers: MutableMap<ActionMode, ActionManager> = mutableMapOf()

    fun nextTick(self: Wizard, world: World, game: Game, move: Move) {
        this.self = self
        this.world = world
        this.game = game
        this.move = move

        initializeDefault()

        makeDecision()
    }

    private fun makeDecision() {
        val actionManager = gameManagers[actionMode]!!

        actionManager.init(self, world, game, move, this)

        laneDecision()

        actionManager.move()
    }

    private fun laneDecision() {
        //defence
        val friendMostKillingLine = mapLines.shouldChangeLine(sortByEnemyTowers = false) {
            it.enemy == false && it.deadFriendTowerCount == 2 && it.enemyWizardPositions.isNotEmpty()
        }
        if (friendMostKillingLine) return

        //or attack on line without wizards
        val enemyLineWithoutWizards = mapLines.shouldChangeLine {
            it.enemy == true && it.friendPosition > 0 && it.enemyWizardPositions.isEmpty()
        }
        if (enemyLineWithoutWizards) return

        //or attack line, there are friend wizards less when enemy wizards
        val enemyMostKillingLine = mapLines.shouldChangeLine { line ->
            line.enemy == true && line.friendWizardsOnLine() < line.enemyWizardPositions.size
        }
        if (enemyMostKillingLine) return
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

        if (actionMode == null)
            actionMode = ActionMode.ATTACK

        gameManagers.put(ActionMode.ATTACK, AttackActionManager())
    }
}
