import model.ActionType
import model.LivingUnit
import model.Tree
import model.Wizard
import java.util.Optional


class AttackActionManager : ActionManager() {
    override fun move(): ActionMode {
        if (self.life < self.maxLife * ActionManager.Companion.LOW_HP_FACTOR) {
            moveHelper.goTo(mapWayFinder.getPreviousWaypoint(strategyManager.laneType))
            return ActionMode.ATTACK
        }

        val nearestTarget = findHelper.nearestEnemy

        var nextWaypoint: Point2D? = null
        if (isNeedToMoveBack) {
            moveHelper.goWithoutTurn(mapWayFinder.getPreviousWaypoint(strategyManager.laneType))

            nearestTarget.let { livingUnit -> shootHelder.shootToTarget(livingUnit) }

            return ActionMode.ATTACK
        } else {
            nextWaypoint = mapWayFinder.getNextWaypoint(strategyManager.laneType)
            moveHelper.goWithoutTurn(nextWaypoint)

            if (nearestTarget != null) {
                shootHelder.shootToTarget(nearestTarget)
                return ActionMode.ATTACK
            }
        }

        moveHelper.goTo(nextWaypoint)

        val nearestTree = findHelper.getAllTrees()
                .filter { tree -> self.getAngleTo(tree) < game.staffSector }
                .filter { tree -> self.getDistanceTo(tree) < self.radius + tree.radius + ActionManager.Companion.MIN_CLOSEST_DISTANCE }
                .firstOrNull()

        nearestTree?.let { tree -> move.action = ActionType.STAFF }

        super.move()

        return ActionMode.ATTACK
    }

    private val isNeedToMoveBack: Boolean
        get() {
            val minionsCondition = minionConditions()
            if (minionsCondition) return true

            val enemyWizards = findHelper.getAllWizards(true, true)

            val multiEnemiesCondition = multiEnemiesCondition(enemyWizards)
            if (multiEnemiesCondition) return true

            val singleEnemyCondition = singleEnemyCondition(enemyWizards)
            if (singleEnemyCondition) return true

            val buldingCondition = buldingCondition()
            if (buldingCondition) return true

            return false
        }

    override val mode: ActionMode
        get() = ActionMode.ATTACK
}
