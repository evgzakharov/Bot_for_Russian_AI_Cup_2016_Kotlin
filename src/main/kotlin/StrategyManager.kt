import model.*

import java.util.Comparator
import java.util.HashMap

class StrategyManager {

    lateinit var self: Wizard
    lateinit var world: World
    lateinit var game: Game
    lateinit var move: Move

    var laneType: LaneType? = null
    var actionMode: ActionMode? = null
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

        actionMode = actionManager.move()
    }

    private fun laneDecision() {
//        val friendMostKillingLine = MapHelper.mapLines
//                .filter { it.enemy ?: false && it.deadFriendTowerCount > 0 }
//                .firstOrNull()
//
//        if (friendMostKillingLine?.deadFriendTowerCount == 2 && friendMostKillingLine?.enemyWizardPositions?.isNotEmpty() ?: false) {
//            laneType = friendMostKillingLine!!.laneType
//        } else {
//            val enemyMostKillingLine = MapHelper.mapLines
//                    .filter { it.enemy ?: false }
//                    .sortedByDescending { it.deadEnemyTowerCount }
//                    .firstOrNull()
//
//
//        }
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
