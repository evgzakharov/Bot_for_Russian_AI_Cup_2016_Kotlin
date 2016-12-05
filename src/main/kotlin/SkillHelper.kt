import model.*

class SkillHelper(private val game: Game, private val self: Wizard) {

    fun isHasFireboll(): Boolean {
        return self.getSkills().isNotEmpty()
                && self.getSkills().contains(SkillType.FIREBALL)
    }

    fun isFirebollActive(): Boolean {
        return self.getSkills().isNotEmpty()
                && self.getSkills().contains(SkillType.FIREBALL)
                && self.mana >= game.fireballManacost
                && self.getRemainingCooldownTicksByAction()[ActionType.FIREBALL.ordinal] == 0
                && self.remainingActionCooldownTicks == 0
    }

    fun isFrostBoltActive(): Boolean {
        return self.getSkills().isNotEmpty()
                && self.getSkills().contains(SkillType.FROST_BOLT)
                && self.mana >= game.frostBoltManacost
                && self.getRemainingCooldownTicksByAction()[ActionType.FROST_BOLT.ordinal] == 0
                && self.remainingActionCooldownTicks == 0
    }
}