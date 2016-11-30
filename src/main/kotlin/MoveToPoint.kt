class MoveToPoint : Action() {

    override fun move(target: Any?): Boolean {
        if (target == null || target !is Point2D) return false

        if (isNeedToMoveBack()) return false

        if (self.getDistanceTo(target) <= self.radius + game.bonusRadius) return false

        moveHelper.goTo(target)

        return super.move(null)
    }
}
