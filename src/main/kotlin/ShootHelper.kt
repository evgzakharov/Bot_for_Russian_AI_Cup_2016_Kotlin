import model.*
import java.lang.StrictMath.*

class ShootHelper(private val self: Wizard, private val game: Game, private val move: Move) {

    private val skillHelper: SkillHelper = SkillHelper(game, self)

    fun shootToTarget(nearestTarget: LivingUnit) {
        val distance = self.getDistanceTo(nearestTarget)

        val angle = self.getAngleTo(nearestTarget)

        move.turn = angle

        val shootDistance = distance <= self.castRange

        if (abs(angle) < game.staffSector / 2.0) {
            var missleRadius: Double = 0.0

            if (canShootByFireboll(nearestTarget)) {
                move.action = ActionType.FIREBALL

                if (isInRange(nearestTarget, game.fireballExplosionMaxDamageRange))
                    missleRadius = game.fireballExplosionMaxDamageRange
                else
                    missleRadius = game.fireballExplosionMinDamageRange

            } else if (canShootByFrostBolt(nearestTarget) && shootDistance) {
                move.action = ActionType.FROST_BOLT
                missleRadius = game.frostBoltRadius
            } else {
                if (self.getDistanceTo(nearestTarget) <= game.staffRange)
                    move.action = ActionType.STAFF
                else if (shootDistance)
                    move.action = ActionType.MAGIC_MISSILE

                missleRadius = game.magicMissileRadius
            }

            move.castAngle = angle
            move.minCastDistance = max(distance - nearestTarget.radius + missleRadius, self.radius + MIN_CAST_RANGE)
        }
    }

    fun canShootByFireboll(nearestTarget: LivingUnit): Boolean {
        if (!skillHelper.isFirebollActive()) return false

        if (self.castRange + game.fireballExplosionMinDamageRange <= self.getDistanceTo(nearestTarget) - nearestTarget.radius) return false

        return when (nearestTarget) {
            is Tree -> false
            is Wizard, is Building -> true
            is Minion -> self.mana > self.maxMana * MIN_MINION_MANA_FACTOR
            else -> false
        }
    }

    fun isInRange(nearestTarget: LivingUnit, range: Double): Boolean =
            self.castRange + range <= self.getDistanceTo(nearestTarget) - nearestTarget.radius

    fun canShootByFrostBolt(nearestTarget: LivingUnit): Boolean {
        if (!skillHelper.isFrostBoltActive()) return false

        return when (nearestTarget) {
            is Tree, is Building -> false
            is Wizard -> true
            is Minion -> self.mana > self.maxMana * MIN_MINION_MANA_FACTOR && nearestTarget.life >= game.frostBoltDirectDamage
            else -> false
        }
    }

    companion object {
        val MIN_MINION_MANA_FACTOR: Double = 0.6

        val MIN_CAST_RANGE: Double = 1.0
    }
}
