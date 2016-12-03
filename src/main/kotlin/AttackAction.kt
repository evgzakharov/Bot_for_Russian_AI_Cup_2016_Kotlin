import model.ActionType
import model.LivingUnit
import model.Tree
import model.Wizard
import java.util.Optional


class AttackAction : Action() {
    override fun move(target: Any?): Boolean {
        var shootToTarget: Boolean = false

        val pointToMove = mapWayFinder.getPreviousWaypoint(strategyManager.currentLaneType!!)
        if (self.life < self.maxLife * Action.Companion.LOW_HP_FACTOR) {
            safeHelper.tryToSafeByShield(self)

            if (self.getDistanceTo(MapHelper.friendBasePoint) < MIN_CLOSE_DISTANCE_TO_BASE) {
                val nearestTarget = findHelper.getNearestTarget()

                nearestTarget?.let { livingUnit ->
                    shootToTarget = true
                    shootHelder.shootToTarget(livingUnit)
                }
            }

            if (!shootToTarget)
                moveHelper.goTo(pointToMove)

        } else {
            val nearestTarget = findHelper.getNearestTarget()

            val nextWaypoint: Point2D?
            if (isNeedToMoveBack()) {
                if (self.life < self.maxLife * 0.7) safeHelper.tryToSafeByShield(self)

                moveHelper.goWithoutTurn(mapWayFinder.getPreviousWaypoint(strategyManager.currentLaneType!!))

                nearestTarget?.let { livingUnit ->
                    shootToTarget = true
                    shootHelder.shootToTarget(livingUnit)
                }
            } else {
                nextWaypoint = mapWayFinder.getNextWaypoint(strategyManager.currentLaneType!!, strategyManager.globalStrateg)
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

    companion object {
        val MIN_CLOSE_DISTANCE_TO_BASE: Double = 300.0
    }
}
