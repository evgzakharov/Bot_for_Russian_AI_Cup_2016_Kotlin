import model.*

import java.lang.StrictMath.abs

class ShootHelper(private val self: Wizard, private val game: Game, private val move: Move) {

    fun shootToTarget(nearestTarget: LivingUnit) {
        val distance = self.getDistanceTo(nearestTarget)

        val angle = self.getAngleTo(nearestTarget)

        move.turn = angle

        if (distance > self.castRange) return

        if (abs(angle) < game.staffSector / 2.0) {
            var missleRadius: Double = 0.0

            if (self.getSkills().isNotEmpty()
                    && nearestTarget is Wizard
                    && self.getSkills().contains(SkillType.FROST_BOLT)
                    && self.mana > game.frostBoltManacost
                    && self.getRemainingCooldownTicksByAction()[ActionType.FROST_BOLT.ordinal] == 0) {
                move.action = ActionType.FROST_BOLT
                missleRadius = game.frostBoltRadius
            } else if (self.getSkills().isNotEmpty()
                    && self.getSkills().contains(SkillType.FIREBALL)
                    && self.mana > game.fireballManacost
                    && self.getRemainingCooldownTicksByAction()[ActionType.FIREBALL.ordinal] == 0) {
                move.action = ActionType.FIREBALL
                missleRadius = game.fireballRadius
            } else {
                move.action = ActionType.MAGIC_MISSILE
                missleRadius = game.magicMissileRadius
            }

            move.castAngle = angle
            move.minCastDistance = distance - nearestTarget.radius + missleRadius
        }


    }

}
