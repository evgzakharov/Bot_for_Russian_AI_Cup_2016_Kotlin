import model.Bonus

class CatchArtifactAction : Action() {

    override fun move(target: Any?): Boolean {
        if (target == null || target !is Point2D) return false

        if (isNeedToMoveBack()) return false

        val distanceToArtifact = self.getDistanceTo(target)
        val bonuses = world.getBonuses()
        val bonusCatchRadius = self.radius + game.bonusRadius

        if (distanceToArtifact <= bonusCatchRadius ||
                (distanceToArtifact <= self.visionRange && bonuses.none { target.getDistanceTo(it) <= ARTIFACT_CLOSEST_RANGE }))
            return false

        moveHelper.goTo(target)

        return super.move(null)
    }

    override fun isNeedToMoveBack(): Boolean {
        val enemyWizards = findHelper.getAllWizards(true, true)

        val multiEnemiesCondition = multiEnemiesCondition(enemyWizards)
        if (multiEnemiesCondition) return true

        val singleEnemyCondition = singleEnemyCondition(enemyWizards)
        if (singleEnemyCondition) return true

        val buldingCondition = buldingCondition()
        if (buldingCondition) return true

        return false
    }

    companion object {
        val ARTIFACT_CLOSEST_RANGE: Double = 50.0
    }
}
