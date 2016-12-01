import model.*

class SafeHelper(val self: Wizard, val game: Game, val move: Move) {

    fun tryToSafeByShield(wizardToSafe: Wizard) {
        val skills = self.getSkills()
        if (skills.isEmpty()) return

        if (!skills.contains(SkillType.SHIELD)) return

        if (self.getRemainingCooldownTicksByAction()[ActionType.SHIELD.ordinal] != 0) return

        if (self.faction != wizardToSafe.faction) return

        if (wizardToSafe.life > wizardToSafe.maxLife * MIN_LIFE_FACTOR) return

        if (self.id == wizardToSafe.id || self.getDistanceTo(wizardToSafe) <= self.castRange) {
            move.action = ActionType.SHIELD
            move.statusTargetId = wizardToSafe.id
        }
    }

    companion object {
        val MIN_LIFE_FACTOR: Double = 0.7
    }

}
