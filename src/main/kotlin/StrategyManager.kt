import MapHelper.attackLines
import MapHelper.friendBasePoint
import MapHelper.mapLines
import MapHelper.wizard
import model.*
import java.lang.StrictMath.abs

data class BonusTimeCatch(var lastCatchTick: Int = 0)

enum class GlobalStrateg {
    DEFENCE, ATTACK
}

class StrategyManager {
    lateinit var self: Wizard
    lateinit var world: World
    lateinit var game: Game
    lateinit var move: Move
    lateinit var findHelper: FindHelper
    lateinit var moveHelper: MoveHelper
    lateinit var skillsHelper: SkillHelper

    var actionMode: ActionMode = ActionMode.ATTACK
        private set
    var movingTarget: Any? = null
    val defaulActionMode: ActionMode = ActionMode.ATTACK

    var currentLaneType: LaneType? = null
    var lastLaneDefenceChangeTick: Int = -MIN_CHANGE_DEFENCE_TICK_LIMIT
    var lastLaneAttackChangeTick: Int = -MIN_CHANGE_ATTACK_TICK_LIMIT

    var globalStrateg = GlobalStrateg.ATTACK

    private var gameManagers: MutableMap<ActionMode, Action> = mutableMapOf()

    private var bonuses: Map<Point2D, BonusTimeCatch> = sortedMapOf(
            Point2D(1200.0, 1200.0) to BonusTimeCatch(),
            Point2D(2800.0, 2800.0) to BonusTimeCatch()
    )

    fun nextTick(self: Wizard, world: World, game: Game, move: Move) {
        this.self = self
        this.world = world
        this.game = game
        this.move = move
        this.findHelper = FindHelper(world, game, self)
        this.moveHelper = MoveHelper(self, world, game, move)
        this.skillsHelper = SkillHelper(game, self)
        MapHelper.nextTick(world, game, self)

        initializeDefault()

        makeDecision()
    }

    private fun makeDecision() {
        try {
            checkSkills()

            updateBonusInfo()

            globalStrategDecision()

            actionDecision()

            action()
        } catch (e: Throwable) {
            moveHelper.goTo(friendBasePoint)
        }
    }

    private fun globalStrategDecision() {
        val needDefenceLine = laneDefenceDecision()
        if (needDefenceLine == null) {
            globalStrateg = GlobalStrateg.ATTACK

            val laneChange = laneAttackDecision()

            if (laneChange != null) {
                lastLaneAttackChangeTick = world.tickIndex
                this.currentLaneType = laneChange
            }
        } else {
            globalStrateg = GlobalStrateg.DEFENCE

            if (world.tickIndex < MIN_START_CHANGE_TICK) return

            if (world.tickIndex - lastLaneDefenceChangeTick <= MIN_CHANGE_DEFENCE_TICK_LIMIT) return

            lastLaneDefenceChangeTick = world.tickIndex
            currentLaneType = needDefenceLine
        }
    }

    var learningSkills: List<SkillType>? = null

    private fun checkSkills() {
        if (!game.isSkillsEnabled) return

        if (world.tickIndex < MIN_START_CHANGE_TICK) return

        if (learningSkills == null) {
            learningSkills = when (currentLaneType) {
                LaneType.TOP, LaneType.BOTTOM -> skillesToLearnFrostV3
                LaneType.MIDDLE -> skillesToLearnFrostV3
                null -> throw RuntimeException("omg")
            }
        }

        val selfSkils = self.getSkills().toList()

        val skillsToLearn = learningSkills!!
                .filter { selfSkils.isEmpty() || !selfSkils.contains(it) }
                .first()

        move.skillToLearn = skillsToLearn
    }

    private fun action() {
        val moveSuccess = movingTarget?.let { move(it) } ?: false

        if (!moveSuccess) {
            actionMode = defaulActionMode
            movingTarget = null
            move(movingTarget)
        }
    }

    private fun move(point: Any?): Boolean {
        val actionManager = gameManagers[actionMode]!!
        actionManager.init(self, world, game, move, this)
        return actionManager.move(point)
    }

    private fun actionDecision() {
        val enemyToKill = findHelper.getAllWizards(onlyEnemy = true, onlyNearest = true)
                .filter { self.life * MY_HP_MULTIPLIER >= it.life }
                .filter { self.getDistanceTo(it) < TRY_TO_KILL_ENEMY_RADIUS }
                .sortedByDescending { it.life }
                .firstOrNull()

        if (enemyToKill != null) {
            actionMode = ActionMode.KILL_ENEMY
            movingTarget = enemyToKill
            return
        }

        val nearestArtifact = world.getBonuses()
                .filter { self.getDistanceTo(it) < TRY_TO_CATCH_ARTIFACT_DISTANCE }
                .sortedByDescending { self.getDistanceTo(it) }
                .firstOrNull()

        if (nearestArtifact != null) {
            actionMode = ActionMode.CATCH_ARTIFACT
            movingTarget = nearestArtifact.toPoint()
            return
        }

        val mayCatchBonus = bonuses
                .filter { it.value.tickDiff() >= BONUS_UPDATE_TICK }
                .keys
                .filter { self.getDistanceTo(it) < TRY_TO_CATCH_ARTIFACT_DISTANCE2 }
                .firstOrNull()

        val wizardOnArtifactLine = MapHelper.getLinePositions(wizard, 1.0)
                .find { it.mapLine.laneType == null }?.let { true } ?: false

        //TODO: add more creterias
        if (mayCatchBonus != null && wizardOnArtifactLine && globalStrateg == GlobalStrateg.ATTACK) {
            val wizardInEnemyLine = MapHelper.getLinePositions(self, 1.0)
                    .filter { it.mapLine.enemy }
                    .firstOrNull()

            if (wizardInEnemyLine == null || (wizardInEnemyLine.position < wizardInEnemyLine.mapLine.lineLength * LINE_POSITION_MULTIPLIER)) {
                actionMode = ActionMode.CATCH_ARTIFACT
                movingTarget = mayCatchBonus
                return
            }
        }

        actionMode = ActionMode.ATTACK
        movingTarget = null
    }

    private fun updateBonusInfo() {
        world.getWizards().forEach { wizard ->
            bonuses.forEach { bonus ->
                if (wizard.radius + game.bonusRadius >= wizard.getDistanceTo(bonus.key))
                    bonus.value.lastCatchTick = world.tickIndex

                val distanceToArtifact = wizard.getDistanceTo(bonus.key)
                val worldBonuses = world.getBonuses()

                if (!findHelper.isEnemy(self.faction, wizard) &&
                        (distanceToArtifact <= wizard.visionRange && worldBonuses.none { bonus.key.getDistanceTo(it) <= game.bonusRadius }))
                    bonus.value.lastCatchTick = world.tickIndex
            }
        }
    }

    private fun laneDefenceDecision(): LaneType? {
        val defenceLine = mapLines
                .filter { line ->
                    line.enemy == false &&
                            (line.deadFriendTowerCount >= 1
                                    && line.enemyWizardPositions.isNotEmpty()
                                    && line.enemyPosition ?: Double.MAX_VALUE <= line.lineLength * LINE_MIN_DEFENCE_FACTOR)
                }
                .minBy { line -> line.enemyWizardPositions.values.min() ?: line.lineLength }
                ?.let { it.laneType }

        return defenceLine
    }

    private fun laneAttackDecision(): LaneType? {
        if (world.tickIndex < MIN_START_CHANGE_TICK)
            return null

        if (world.tickIndex - lastLaneAttackChangeTick <= MIN_CHANGE_ATTACK_TICK_LIMIT)
            return null

        val lineWithoutFriendWizards = attackLines
                .filter { it.key != LaneType.MIDDLE }
                .mapValues { attackLine -> attackLine.value.friendWizards() }
                .filter { it.value.isEmpty() }.keys

        if (lineWithoutFriendWizards.isNotEmpty() && attackLines[currentLaneType!!]!!.friendWizards().size > 1)
            return lineWithoutFriendWizards.firstOrNull()

        return null
    }

    private fun AttackLine.friendWizards(): Set<Long> {
        return this.friend.historyFriendWizardPositions.toData()
                .filter { it.value >= this@friendWizards.friend.lineLength * LINE_WIZARD_MIN_FACTOR }
                .keys + this.enemy.historyFriendWizardPositions.toData().keys
    }

    private fun List<MapLine>.shouldChangeLine(sortByEnemyTowers: Boolean = true, criteria: (MapLine) -> Boolean): LaneType? {
        return this.filter { criteria(it) }
                .sortedByDescending { if (sortByEnemyTowers) it.deadEnemyTowerCount else it.deadFriendTowerCount }
                .firstOrNull()?.laneType
    }

    private fun initializeDefault() {
        if (currentLaneType == null) {
            when (self.id.toInt()) {
                1, 2, 6, 7 -> currentLaneType = LaneType.TOP
                3, 8 -> currentLaneType = LaneType.MIDDLE
                4, 5, 9, 10 -> currentLaneType = LaneType.BOTTOM
            }
        }

        gameManagers.put(ActionMode.ATTACK, AttackAction())
        gameManagers.put(ActionMode.KILL_ENEMY, KillEnemyAction())
        gameManagers.put(ActionMode.CATCH_ARTIFACT, CatchArtifactAction())
    }

    private fun BonusTimeCatch.tickDiff() = abs(world.tickIndex - this.lastCatchTick)

    companion object {
        const val MY_HP_MULTIPLIER: Double = 0.8
        const val TRY_TO_KILL_ENEMY_RADIUS: Double = 500.0

        const val TRY_TO_CATCH_ARTIFACT_DISTANCE: Double = 150.0
        const val TRY_TO_CATCH_ARTIFACT_DISTANCE2: Double = 1000.0

        const val BONUS_UPDATE_TICK: Int = 2500

        const val MIN_START_CHANGE_TICK: Int = 500

        const val MIN_CHANGE_DEFENCE_TICK_LIMIT: Int = 500
        const val MIN_CHANGE_ATTACK_TICK_LIMIT: Int = 3500
        const val MIN__ATTACK_TICK_LIMIT: Int = 3500

        const val LINE_POSITION_MULTIPLIER: Double = 0.2

        const val LINE_MIN_DEFENCE_FACTOR: Double = 0.3

        const val LINE_WIZARD_MIN_FACTOR: Double = 0.2

        val skillesToLearnFrostV3: List<SkillType> = listOf(
                SkillType.MAGICAL_DAMAGE_BONUS_PASSIVE_1,
                SkillType.MAGICAL_DAMAGE_BONUS_AURA_1,
                SkillType.MAGICAL_DAMAGE_BONUS_PASSIVE_2,
                SkillType.MAGICAL_DAMAGE_BONUS_AURA_2,
                SkillType.FROST_BOLT,
                SkillType.MAGICAL_DAMAGE_ABSORPTION_PASSIVE_1,
                SkillType.MAGICAL_DAMAGE_ABSORPTION_AURA_1,
                SkillType.MAGICAL_DAMAGE_ABSORPTION_PASSIVE_2,
                SkillType.MAGICAL_DAMAGE_ABSORPTION_AURA_2,
                SkillType.SHIELD,
                SkillType.RANGE_BONUS_PASSIVE_1,
                SkillType.RANGE_BONUS_AURA_1,
                SkillType.RANGE_BONUS_PASSIVE_2,
                SkillType.RANGE_BONUS_AURA_2,
                SkillType.ADVANCED_MAGIC_MISSILE,
                SkillType.STAFF_DAMAGE_BONUS_PASSIVE_1,
                SkillType.STAFF_DAMAGE_BONUS_AURA_1,
                SkillType.STAFF_DAMAGE_BONUS_PASSIVE_2,
                SkillType.STAFF_DAMAGE_BONUS_AURA_2,
                SkillType.FIREBALL,
                SkillType.MOVEMENT_BONUS_FACTOR_PASSIVE_1,
                SkillType.MOVEMENT_BONUS_FACTOR_AURA_1,
                SkillType.MOVEMENT_BONUS_FACTOR_PASSIVE_2,
                SkillType.MOVEMENT_BONUS_FACTOR_AURA_2,
                SkillType.HASTE
        )

        val skillesToLearnFire: List<SkillType> = listOf(
                SkillType.STAFF_DAMAGE_BONUS_PASSIVE_1,
                SkillType.STAFF_DAMAGE_BONUS_AURA_1,
                SkillType.STAFF_DAMAGE_BONUS_PASSIVE_2,
                SkillType.STAFF_DAMAGE_BONUS_AURA_2,
                SkillType.FIREBALL,
                SkillType.MAGICAL_DAMAGE_BONUS_PASSIVE_1,
                SkillType.MAGICAL_DAMAGE_BONUS_AURA_1,
                SkillType.MAGICAL_DAMAGE_BONUS_PASSIVE_2,
                SkillType.MAGICAL_DAMAGE_BONUS_AURA_2,
                SkillType.FROST_BOLT,
                SkillType.RANGE_BONUS_PASSIVE_1,
                SkillType.RANGE_BONUS_AURA_1,
                SkillType.RANGE_BONUS_PASSIVE_2,
                SkillType.RANGE_BONUS_AURA_2,
                SkillType.ADVANCED_MAGIC_MISSILE,
                SkillType.MAGICAL_DAMAGE_ABSORPTION_PASSIVE_1,
                SkillType.MAGICAL_DAMAGE_ABSORPTION_AURA_1,
                SkillType.MAGICAL_DAMAGE_ABSORPTION_PASSIVE_2,
                SkillType.MAGICAL_DAMAGE_ABSORPTION_AURA_2,
                SkillType.SHIELD,
                SkillType.MOVEMENT_BONUS_FACTOR_PASSIVE_1,
                SkillType.MOVEMENT_BONUS_FACTOR_AURA_1,
                SkillType.MOVEMENT_BONUS_FACTOR_PASSIVE_2,
                SkillType.MOVEMENT_BONUS_FACTOR_AURA_2,
                SkillType.HASTE
        )

        val skillesToLearnShield: List<SkillType> = listOf(
                SkillType.MAGICAL_DAMAGE_ABSORPTION_PASSIVE_1,
                SkillType.MAGICAL_DAMAGE_ABSORPTION_AURA_1,
                SkillType.MAGICAL_DAMAGE_ABSORPTION_PASSIVE_2,
                SkillType.MAGICAL_DAMAGE_ABSORPTION_AURA_2,
                SkillType.SHIELD,
                SkillType.STAFF_DAMAGE_BONUS_PASSIVE_1,
                SkillType.STAFF_DAMAGE_BONUS_AURA_1,
                SkillType.STAFF_DAMAGE_BONUS_PASSIVE_2,
                SkillType.STAFF_DAMAGE_BONUS_AURA_2,
                SkillType.FIREBALL,
                SkillType.MAGICAL_DAMAGE_BONUS_PASSIVE_1,
                SkillType.MAGICAL_DAMAGE_BONUS_AURA_1,
                SkillType.MAGICAL_DAMAGE_BONUS_PASSIVE_2,
                SkillType.MAGICAL_DAMAGE_BONUS_AURA_2,
                SkillType.FROST_BOLT,
                SkillType.MOVEMENT_BONUS_FACTOR_PASSIVE_1,
                SkillType.MOVEMENT_BONUS_FACTOR_AURA_1,
                SkillType.MOVEMENT_BONUS_FACTOR_PASSIVE_2,
                SkillType.MOVEMENT_BONUS_FACTOR_AURA_2,
                SkillType.RANGE_BONUS_PASSIVE_1,
                SkillType.RANGE_BONUS_AURA_1,
                SkillType.RANGE_BONUS_PASSIVE_2,
                SkillType.RANGE_BONUS_AURA_2,
                SkillType.ADVANCED_MAGIC_MISSILE,
                SkillType.HASTE
        )

        val skillesToLearnMagicMissle: List<SkillType> = listOf(
                SkillType.RANGE_BONUS_PASSIVE_1,
                SkillType.RANGE_BONUS_AURA_1,
                SkillType.RANGE_BONUS_PASSIVE_2,
                SkillType.RANGE_BONUS_AURA_2,
                SkillType.ADVANCED_MAGIC_MISSILE,
                SkillType.MAGICAL_DAMAGE_BONUS_PASSIVE_1,
                SkillType.MAGICAL_DAMAGE_BONUS_AURA_1,
                SkillType.MAGICAL_DAMAGE_BONUS_PASSIVE_2,
                SkillType.MAGICAL_DAMAGE_BONUS_AURA_2,
                SkillType.FROST_BOLT,
                SkillType.MAGICAL_DAMAGE_ABSORPTION_PASSIVE_1,
                SkillType.MAGICAL_DAMAGE_ABSORPTION_AURA_1,
                SkillType.MAGICAL_DAMAGE_ABSORPTION_PASSIVE_2,
                SkillType.MAGICAL_DAMAGE_ABSORPTION_AURA_2,
                SkillType.SHIELD,
                SkillType.STAFF_DAMAGE_BONUS_PASSIVE_1,
                SkillType.STAFF_DAMAGE_BONUS_AURA_1,
                SkillType.STAFF_DAMAGE_BONUS_PASSIVE_2,
                SkillType.STAFF_DAMAGE_BONUS_AURA_2,
                SkillType.FIREBALL,
                SkillType.MOVEMENT_BONUS_FACTOR_PASSIVE_1,
                SkillType.MOVEMENT_BONUS_FACTOR_AURA_1,
                SkillType.MOVEMENT_BONUS_FACTOR_PASSIVE_2,
                SkillType.MOVEMENT_BONUS_FACTOR_AURA_2,
                SkillType.HASTE
        )
    }
}


