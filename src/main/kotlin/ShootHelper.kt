import model.*

import java.lang.StrictMath.abs

class ShootHelper(private val self: Wizard, private val game: Game, private val move: Move) {

    fun shootToTarget(nearestTarget: LivingUnit) {
        val distance = self.getDistanceTo(nearestTarget)

        val angle = self.getAngleTo(nearestTarget)

        move.turn = angle

        if (distance > self.castRange) return

        if (abs(angle) < game.staffSector / 2.0) {
            move.action = ActionType.MAGIC_MISSILE
            move.castAngle = angle
            move.minCastDistance = distance - nearestTarget.radius + game.magicMissileRadius
        }
    }

}
