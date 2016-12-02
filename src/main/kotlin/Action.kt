import model.*

import java.util.Comparator
import java.util.Optional
import java.util.stream.Collectors

import java.lang.StrictMath.PI
import java.lang.StrictMath.abs

abstract class Action {

    protected lateinit var self: Wizard
    protected lateinit var world: World
    protected lateinit var game: Game
    protected lateinit var move: Move
    protected lateinit var findHelper: FindHelper
    protected lateinit var shootHelder: ShootHelper
    protected lateinit var moveHelper: MoveHelper
    protected lateinit var mapWayFinder: MapWayFinder
    protected lateinit var strategyManager: StrategyManager
    protected lateinit var safeHelper: SafeHelper

    fun init(self: Wizard, world: World, game: Game, move: Move, strategyManager: StrategyManager) {
        this.self = self
        this.world = world
        this.game = game
        this.move = move
        this.findHelper = FindHelper(world, game, self)
        this.shootHelder = ShootHelper(self, game, move)
        this.moveHelper = MoveHelper(self, world, game, move)
        this.mapWayFinder = MapWayFinder(world, game, self)
        this.strategyManager = strategyManager
        this.safeHelper = SafeHelper(self, game, move)
    }

    open fun move(target: Any?): Boolean {
        val veryCloseTree = findHelper.getAllTrees()
                .filter { tree -> self.getDistanceTo(tree) <= self.radius + tree.radius + MIN_CLOSEST_DISTANCE }
                .minBy { abs(self.getAngleTo(it)) }

        veryCloseTree?.let { tree -> shootHelder.shootToTarget(veryCloseTree) }

        return true
    }

    open protected fun isNeedToMoveBack(): Boolean {
        val minionsCondition = minionConditions()
        if (minionsCondition) return true

        val enemyWizards = findHelper.getAllWizards(true, true)

        val multiEnemiesCondition = multiEnemiesCondition(enemyWizards)
        if (multiEnemiesCondition) return true

        val singleEnemyCondition = singleEnemyCondition(enemyWizards)
        if (singleEnemyCondition) return true

        val buldingCondition = buldingCondition()
        if (buldingCondition) return true

        return false
    }

    open protected fun buldingCondition(): Boolean {
        val nearestBuilding = findHelper.getAllBuldings(true)
                .minBy { self.getDistanceTo(it) }

        var buldingCondition = false
        if (nearestBuilding != null) {
            val nearestFriendToBuilding = findHelper.getAllMovingUnits(onlyEnemy = false, onlyNearest = true)
                    .filter { unit -> unit.life / unit.maxLife < self.life / self.maxLife && !findHelper.isEnemy(self.faction, unit) }
                    .minBy { nearestBuilding.getDistanceTo(it) }

            val distanceToBuilding = self.getDistanceTo(nearestBuilding)


            if (nearestBuilding.type === BuildingType.GUARDIAN_TOWER) {
                val demageRadius = game.guardianTowerAttackRange + MIN_CLOSEST_DISTANCE
                if (distanceToBuilding <= demageRadius) {
                    val noFriends = nearestFriendToBuilding
                            ?.let { livingUnit -> distanceToBuilding < livingUnit.getDistanceTo(nearestBuilding) } ?: true

                    val buldingIsToClose = distanceToBuilding <= demageRadius * MIN_DISTANCE_TO_TOWER_FACTOR

                    val hgIsLow = self.life < (1 - LOW_HP_FACTOR) * self.maxLife

                    val buldingWillShoot = nearestBuilding.remainingActionCooldownTicks < 100

                    val hgIsVeryLow = self.life < LOW_HP_NEAREST_TOWER_FACTOR * self.maxLife

                    if (noFriends && hgIsLow && buldingWillShoot || buldingIsToClose || hgIsVeryLow)
                        buldingCondition = true
                }
            } else if (nearestBuilding.type === BuildingType.FACTION_BASE) {
                val demageRadius = game.factionBaseAttackRange + MIN_CLOSEST_DISTANCE

                val buldingIsToClose = distanceToBuilding <= demageRadius * MIN_BASE_DISTANCE_FACTOR

                val hgIsLow = self.life < LOW_HP_NEAREST_BASE_FACTOR * self.maxLife

                if (buldingIsToClose || hgIsLow)
                    buldingCondition = true
            }
        }
        return buldingCondition
    }

    open protected fun singleEnemyCondition(enemyWizards: List<Wizard>): Boolean {
        val enemyWithSmallestHP = enemyWizards
                .filter { unit -> self.getDistanceTo(unit) < game.wizardCastRange }
                .minBy { it.life }

        var singleEnemyCondition = false
        if (enemyWithSmallestHP != null) {
            val enemyIsToClose = enemyWithSmallestHP.getDistanceTo(self) <= game.wizardCastRange * SINGLE_ENEMY_CLOSE_RANGE_FACTOR

            val hpIsToLow = self.life < SINGLE_ENEMY_LOG_HP_FACTOR * self.maxLife
                    && self.life * SINGLE_ENEMY_LOG_HP_ENEMY_FACTOR < enemyWithSmallestHP.life
                    && enemyWithSmallestHP.getAngleTo(self) <= game.staffSector * SINGLE_ENEMY_STAFF_FACTOR

            if (enemyIsToClose || hpIsToLow)
                singleEnemyCondition = true
        }
        return singleEnemyCondition
    }

    open protected fun multiEnemiesCondition(enemyWizards: List<Wizard>): Boolean {
        val enemiesLookingToMe = enemyWizards
                .filter { unit ->
                    val distanceTo = self.getDistanceTo(unit)
                    distanceTo < game.wizardCastRange * MULTI_ENEMY_CAST_RANGE_FACTOR && abs(unit.getAngleTo(self)) <= game.staffSector * MULTI_ENEMY_STAFF_SECTOR
                }

        var multiEnemiesCondition = false
        if (enemiesLookingToMe.size > 1) {
            val enemyWithBiggestHP = enemiesLookingToMe
                    .maxBy { it.life } ?: return false

            val hpIsLow = self.life < self.maxLife * MULTI_ENEMY_LOW_HP_FACTOR && self.life * MULTI_ENEMY_LOW_HP_BIGGER_HP_FACTOR < enemyWithBiggestHP.life

            if (hpIsLow)
                multiEnemiesCondition = true
        }
        return multiEnemiesCondition
    }

    open protected fun minionConditions(): Boolean {
        val toCloseMinions = findHelper.getAllMinions(true, true)
                .filter { minion ->
                    if (minion.type === MinionType.FETISH_BLOWDART)
                        findHelper.getAllMinions(true, true)
                                .filter { self.getDistanceTo(minion) <= game.fetishBlowdartAttackRange * FETISH_CLOSE_MULTIPLIER }.isNotEmpty()
                    else if (minion.type === MinionType.ORC_WOODCUTTER)
                        findHelper.getAllMinions(true, true)
                                .filter { self.getDistanceTo(minion) <= game.orcWoodcutterAttackRange * ORC_CLOSE_MULTIPLIER }.isNotEmpty()
                    else
                        false
                }.count()

        return toCloseMinions > 0
    }

    companion object {

        val LOW_HP_FACTOR = 0.25

        val LOW_HP_NEAREST_TOWER_FACTOR = 0.4
        val LOW_HP_NEAREST_BASE_FACTOR = 0.5

        val MIN_DISTANCE_TO_TOWER_FACTOR = 0.7
        val MIN_BASE_DISTANCE_FACTOR = 0.5

        val MIN_CLOSEST_DISTANCE = 5.0
        val MIN_CLOSEST_TREE_DISTANCE = 10.0

        val FETISH_CLOSE_MULTIPLIER = 1.2
        val ORC_CLOSE_MULTIPLIER = 3.0

        val MULTI_ENEMY_CAST_RANGE_FACTOR = 1.1
        val MULTI_ENEMY_STAFF_SECTOR = 1.7
        val MULTI_ENEMY_LOW_HP_FACTOR = 0.8
        val MULTI_ENEMY_LOW_HP_BIGGER_HP_FACTOR = 0.8

        val SINGLE_ENEMY_LOG_HP_FACTOR = 0.5
        val SINGLE_ENEMY_LOG_HP_ENEMY_FACTOR = 0.9
        val SINGLE_ENEMY_CLOSE_RANGE_FACTOR = 0.8
        val SINGLE_ENEMY_STAFF_FACTOR = 1.2

    }
}
