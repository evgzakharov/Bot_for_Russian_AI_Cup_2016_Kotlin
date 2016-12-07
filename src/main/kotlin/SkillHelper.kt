import model.*

class SkillHelper(private val game: Game, private val self: Wizard) {

    fun hasFireboll(): Boolean {
        return self.getSkills().isNotEmpty()
                && self.getSkills().contains(SkillType.FIREBALL)
    }

    fun hasFrostboll(): Boolean {
        return self.getSkills().isNotEmpty()
                && self.getSkills().contains(SkillType.FROST_BOLT)
    }

    fun hasAdvancedMagicMissile(): Boolean {
        return self.getSkills().isNotEmpty()
                && self.getSkills().contains(SkillType.ADVANCED_MAGIC_MISSILE)
    }

    fun isHasSomeAttackSpell(): Boolean {
        return hasFireboll() || hasFrostboll() || hasAdvancedMagicMissile()
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