class MoveToPoint : Action() {

    override fun move(target: Any?): Boolean {
        if (target == null || target !is Point2D) return false

        if (isNeedToMoveBack()) return false

        if (self.getDistanceTo(target) <= self.radius + game.bonusRadius) return false

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
}
