import model.*

import java.util.Comparator
import java.util.Optional
import java.util.stream.Collectors

import java.lang.StrictMath.PI
import java.lang.StrictMath.abs

public abstract class ActionManager {

    protected lateinit var self: Wizard
    protected lateinit var world: World
    protected lateinit var game: Game
    protected lateinit var move: Move
    protected lateinit var findHelper: FindHelper
    protected lateinit var shootHelder: ShootHelper
    protected lateinit var moveHelper: MoveHelper
    protected lateinit var mapWayFinder: MapWayFinder
    protected lateinit var strategyManager: StrategyManager

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
    }

    open fun move(): ActionMode {
        val nearestTree = findHelper.getAllTrees()
                .filter { tree -> abs(self.getAngleTo(tree)) < PI / 2 }
                .filter { tree -> self.getDistanceTo(tree) < self.radius + tree.radius + MIN_CLOSEST_DISTANCE }
                .firstOrNull()

        nearestTree?.let { tree -> shootHelder.shootToTarget(tree) }

        return ActionMode.ATTACK
    }

    abstract val mode: ActionMode

    protected fun buldingCondition(): Boolean {
        val nearestBuilding = findHelper.getAllBuldings(true)
                .minBy { self.getDistanceTo(it) }

        var buldingCondition = false
        if (nearestBuilding != null) {
            var demageRadius = 0.0
            if (nearestBuilding.type === BuildingType.FACTION_BASE)
                demageRadius = game.factionBaseAttackRange
            if (nearestBuilding.type === BuildingType.GUARDIAN_TOWER)
                demageRadius = game.guardianTowerAttackRange + MIN_CLOSEST_DISTANCE

            val distanceToBuilding = self.getDistanceTo(nearestBuilding)
            if (distanceToBuilding < demageRadius) {

                val nearestFriendToBuilding = findHelper.getAllMovingUnits(true, true)
                        .filter { unit -> unit.life / unit.maxLife < self.life / self.maxLife }
                        .minBy { nearestBuilding.getDistanceTo(it) }

                val noFriends = nearestFriendToBuilding
                        ?.let { livingUnit -> distanceToBuilding < livingUnit.getDistanceTo(nearestBuilding) } ?: true

                val buldingIsToClose = demageRadius - distanceToBuilding >= game.wizardRadius * 4

                val hgIsLow = self.life < (1 - LOW_HP_FACTOR) * self.maxLife

                val buldingWillShoot = nearestBuilding.remainingActionCooldownTicks < 100

                if (noFriends && hgIsLow && buldingWillShoot || buldingIsToClose)
                    buldingCondition = true
            }
        }
        return buldingCondition
    }

    protected fun singleEnemyCondition(enemyWizards: List<Wizard>): Boolean {
        val enemyWithSmallestHP = enemyWizards
                .filter { unit -> self.getDistanceTo(unit) < game.wizardCastRange }
                .minBy { it.life }

        var singleEnemyCondition = false
        if (enemyWithSmallestHP != null) {
            val enemyIsToClose = enemyWithSmallestHP.getDistanceTo(self) <= game.wizardCastRange * 0.8

            val hpIsToLow = self.life < LOW_HP_FACTOR * 2 * self.maxLife
                    && self.life * (1 - LOW_HP_FACTOR / 2) < enemyWithSmallestHP.life
                    && enemyWithSmallestHP.getAngleTo(self) <= game.staffSector * 2

            if (enemyIsToClose || hpIsToLow)
                singleEnemyCondition = true
        }
        return singleEnemyCondition
    }

    protected fun multiEnemiesCondition(enemyWizards: List<Wizard>): Boolean {
        val enemiesLookingToMe = enemyWizards
                .filter { unit ->
                    val distanceTo = self.getDistanceTo(unit)
                    distanceTo < game.wizardCastRange * 1.1 && abs(unit.getAngleTo(self)) <= game.staffSector * 1.2
                }

        var multiEnemiesCondition = false
        if (enemiesLookingToMe.size > 1) {
            val enemyWithBiggestHP = enemiesLookingToMe
                    .maxBy { it.life } ?: return false

            val hpIsLow = self.life < self.maxLife * (LOW_HP_FACTOR * 3) && self.life * (1 - LOW_HP_FACTOR / 2) < enemyWithBiggestHP.life

            if (hpIsLow)
                multiEnemiesCondition = true
        }
        return multiEnemiesCondition
    }

    protected fun minionConditions(): Boolean {
        val toCloseMinions = findHelper.getAllMinions(true, true)
                .filter { minion ->
                    if (minion.type === MinionType.FETISH_BLOWDART)
                        findHelper.getAllMinions(true, true)
                                .filter { self.getDistanceTo(minion) <= game.fetishBlowdartAttackRange * 1.1 }.isNotEmpty()
                    else if (minion.type === MinionType.ORC_WOODCUTTER)
                        findHelper.getAllMinions(true, true)
                                .filter { self.getDistanceTo(minion) <= game.orcWoodcutterAttackRange * 3 }.isNotEmpty()
                    else
                        false
                }.count()

        return toCloseMinions > 0
    }

    companion object {

        val LOW_HP_FACTOR = 0.25
        val LOW_BUIDING_FACTOR = 0.1
        val LOW_MINION_FACTOR = 0.35

        val MIN_CLOSEST_DISTANCE = 20.0
    }
}
