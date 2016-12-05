import model.*
import java.util.Optional


class AttackAction : Action() {
    override fun move(target: Any?): Boolean {
        var shootToTarget: Boolean = false

        val selfStatuses = self.getStatuses().toList()

        if (!selfStatuses.isEmpty() && !selfStatuses.none { it.type == StatusType.SHIELDED }
                && self.life < self.maxLife * LOW_HP_FACTOR_WITHOUT_SHIELD)
            safeHelper.tryToSafeByShield(self)

        val nearestTarget = findHelper.getNearestTarget()

        val nextWaypoint: Point2D?
        if (isNeedToMoveBack()) {
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

        if (!shootToTarget)
            super.move(null)

        return true
    }

    companion object {
        val MIN_CLOSE_DISTANCE_TO_BASE: Double = 300.0

        val LOW_HP_FACTOR_WITHOUT_SHIELD: Double = 0.7
    }
}
