import model.ActionType
import model.LivingUnit
import model.Tree
import model.Wizard
import java.util.Optional


class AttackAction : Action() {
    override fun move(target: Any?): Boolean {
        var shootToTarget: Boolean = false

        if (self.life < self.maxLife * Action.Companion.LOW_HP_FACTOR) {
            safeHelper.tryToSafeByShield(self)

            moveHelper.goTo(mapWayFinder.getPreviousWaypoint(strategyManager.currentLaneType!!))
        } else {
            val nearestTarget = findHelper.getNearestTarget()

            val nextWaypoint: Point2D?
            if (isNeedToMoveBack()) {
                moveHelper.goWithoutTurn(mapWayFinder.getPreviousWaypoint(strategyManager.currentLaneType!!))

                nearestTarget?.let { livingUnit ->
                    shootToTarget = true
                    shootHelder.shootToTarget(livingUnit)
                }
            } else {
                nextWaypoint = mapWayFinder.getNextWaypoint(strategyManager.currentLaneType!!)
                moveHelper.goWithoutTurn(nextWaypoint)

                if (nearestTarget != null) {
                    shootToTarget = true
                    shootHelder.shootToTarget(nearestTarget)
                } else
                    moveHelper.goTo(nextWaypoint)
            }
        }

        if (!shootToTarget)
            super.move(null)

        return true
    }
}
