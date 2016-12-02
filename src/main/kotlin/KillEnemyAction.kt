import model.Wizard
import java.lang.Math.*

class KillEnemyAction() : Action() {

    override fun move(target: Any?): Boolean {
        if (target == null || target !is Wizard) return false

        if (isNeedToMoveBack() || wizardGoesAway(target) && wizardIsNotClose(target)) return false

        shootHelder.shootToTarget(target)
        moveToEnemy(target)

        return true
    }

    private fun wizardIsNotClose(target: Wizard): Boolean {
        return self.getDistanceTo(target) > game.wizardCastRange
    }

    private fun wizardGoesAway(target: Wizard): Boolean {
        return sqrt(pow(target.speedX, 2.0) + pow(target.speedY, 2.0)) >= game.wizardForwardSpeed * SPEED_FACTOR
                && abs(PI - target.getAngleTo(self)) <= game.staffSector * STAFF_FACTOR
    }


    override fun isNeedToMoveBack(): Boolean {
        val minionsCondition = minionConditions()
        if (minionsCondition && self.life < self.maxLife * MIN_HP_MINION_FACTOR)
            return true

        val lowHpFactor = self.life < self.maxLife * MIN_HP_FACTOR
        if (lowHpFactor)
            return true

        val enemyWizards = findHelper.getAllWizards(onlyEnemy = true, onlyNearest = true)

        val multiEnemiesCondition = multiEnemiesCondition(enemyWizards)
        if (multiEnemiesCondition) return true

        val singleEnemyCondition = singleEnemyCondition(enemyWizards)
        if (singleEnemyCondition) return true

        val buldingCondition = buldingCondition()
        if (buldingCondition) return true

        return false
    }

    private fun moveToEnemy(wizard: Wizard) {
        moveHelper.goTo(wizard)
    }

    companion object {
        const val SPEED_FACTOR: Double = 0.9
        const val STAFF_FACTOR: Double = 2.0

        const val MIN_HP_FACTOR: Double = 0.45
        const val MIN_HP_MINION_FACTOR: Double = 0.35
    }
}