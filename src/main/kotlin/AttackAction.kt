import model.ActionType
import model.LivingUnit
import model.Tree
import model.Wizard
import java.util.Optional


class AttackAction : Action() {
    override fun move(target: Any?): Boolean {
        if (self.life < self.maxLife * Action.Companion.LOW_HP_FACTOR) {
            moveHelper.goTo(mapWayFinder.getPreviousWaypoint(strategyManager.laneType!!))
            return true
        }

        val nearestTarget = findHelper.nearestEnemy

        val nextWaypoint: Point2D?
        if (isNeedToMoveBack()) {
            moveHelper.goWithoutTurn(mapWayFinder.getPreviousWaypoint(strategyManager.laneType!!))

            nearestTarget?.let { livingUnit -> shootHelder.shootToTarget(livingUnit) }

            return true
        } else {
            nextWaypoint = mapWayFinder.getNextWaypoint(strategyManager.laneType!!)
            moveHelper.goWithoutTurn(nextWaypoint)

            if (nearestTarget != null) {
                shootHelder.shootToTarget(nearestTarget)
                return true
            }
        }

        moveHelper.goTo(nextWaypoint)

        val nearestTree = findHelper.getAllTrees()
                .filter { tree -> self.getAngleTo(tree) < game.staffSector }
                .filter { tree -> self.getDistanceTo(tree) < self.radius + tree.radius + Action.Companion.MIN_CLOSEST_DISTANCE }
                .firstOrNull()

        nearestTree?.let { tree -> move.action = ActionType.STAFF }

        return super.move(null)
    }
}
