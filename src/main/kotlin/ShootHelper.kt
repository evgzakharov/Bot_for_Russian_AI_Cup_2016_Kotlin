import model.*
import java.lang.StrictMath.*

class ShootHelper(private val self: Wizard, private val game: Game, private val move: Move) {

    fun shootToTarget(nearestTarget: LivingUnit) {
        val distance = self.getDistanceTo(nearestTarget)

        val angle = self.getAngleTo(nearestTarget)

        move.turn = angle

        if (distance > self.castRange) return

        if (abs(angle) < game.staffSector / 2.0) {
            var missleRadius: Double = 0.0

            val isSkillesNotEmpty = self.getSkills().isNotEmpty()

            if (isSkillesNotEmpty && nearestTarget !is Tree) {
                if (canShootByFireboll(nearestTarget)) {
                    move.action = ActionType.FIREBALL
                    missleRadius = game.fireballRadius
                } else if (canShootByFrostBolt(nearestTarget)) {
                    move.action = ActionType.FROST_BOLT
                    missleRadius = game.frostBoltRadius
                } else {
                    if (self.getDistanceTo(nearestTarget) <= game.staffRange)
                        move.action = ActionType.STAFF
                    else
                        move.action = ActionType.MAGIC_MISSILE

                    missleRadius = game.magicMissileRadius
                }

            } else {
                if (self.getDistanceTo(nearestTarget) <= game.staffRange)
                    move.action = ActionType.STAFF
                else
                    move.action = ActionType.MAGIC_MISSILE

                missleRadius = game.magicMissileRadius
            }

            move.castAngle = angle
            move.minCastDistance = max(distance - nearestTarget.radius + missleRadius, self.radius + MIN_CAST_RANGE)
        }
    }

    fun canShootByFireboll(nearestTarget: LivingUnit): Boolean {
        if (!self.getSkills().contains(SkillType.FIREBALL)) return false

        if (self.mana < game.frostBoltManacost
                || self.getRemainingCooldownTicksByAction()[ActionType.FIREBALL.ordinal] != 0) return false

        if (self.getDistanceTo(nearestTarget) < game.fireballRadius) return false

        return when (nearestTarget) {
            is Tree -> false
            is Wizard, is Building -> true
            is Minion -> self.mana > self.maxMana * MIN_MANA_FACTOR
            else -> false
        }
    }


    fun canShootByFrostBolt(nearestTarget: LivingUnit): Boolean {
        if (!self.getSkills().contains(SkillType.FROST_BOLT)) return false

        if (self.mana < game.fireballManacost
                || self.getRemainingCooldownTicksByAction()[ActionType.FROST_BOLT.ordinal] != 0) return false

        return when (nearestTarget) {
            is Tree, is Building -> false
            is Wizard -> true
            is Minion -> self.mana > self.maxMana * MIN_MANA_FACTOR && nearestTarget.life >= game.frostBoltDirectDamage
            else -> false
        }
    }

    companion object {
        val MIN_MANA_FACTOR: Double = 0.5

        val MIN_CAST_RANGE: Double = 1.0
    }
}
