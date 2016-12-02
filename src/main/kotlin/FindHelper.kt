import MapHelper.findHelper
import model.*

import java.util.*
import java.util.function.Predicate
import java.util.stream.Collectors

import java.lang.StrictMath.abs

class FindHelper(private val world: World, private val game: Game, private val wizard: Wizard) {

    fun getAllUnits(withTrees: Boolean, onlyEnemy: Boolean, onlyNearest: Boolean, withNeutrals: Boolean): List<LivingUnit> {
        val cacheKey = listOf(withTrees, onlyEnemy, onlyNearest, withNeutrals)
        if (allUnitsCache[cacheKey] != null) return allUnitsCache[cacheKey]!!

        val units = mutableListOf<LivingUnit>()

        units.addAll(getAllWizards(onlyEnemy, onlyNearest))
        units.addAll(getAllBuldings(onlyEnemy))
        units.addAll(getAllMinions(onlyEnemy, onlyNearest, withNeutrals))

        if (withTrees)
            units.addAll(Arrays.asList(*world.getTrees()))

        allUnitsCache.put(cacheKey, units)

        return units
    }

    fun getAllMovingUnits(onlyEnemy: Boolean, onlyNearest: Boolean, withNeutrals: Boolean = false): List<LivingUnit> {
        val cacheKey = listOf(onlyEnemy, onlyNearest, withNeutrals)
        if (allMovingUnitsCache[cacheKey] != null) return allMovingUnitsCache[cacheKey]!!

        val units = ArrayList<LivingUnit>()

        units.addAll(getAllWizards(onlyEnemy, onlyNearest))
        units.addAll(getAllMinions(onlyEnemy, onlyNearest, withNeutrals))

        allMovingUnitsCache.put(cacheKey, units)

        return units
    }

    fun getAllWizards(onlyEnemy: Boolean, onlyNearest: Boolean): List<Wizard> {
        val cacheKey = Arrays.asList(onlyEnemy, onlyNearest)

        if (allWizardsCache[cacheKey] != null) return allWizardsCache[cacheKey]!!

        val newUnins = world.getWizards()
                .filter { wizard -> !wizard.isMe }
                .filter { filterLivingUnits(it, onlyEnemy, onlyNearest) }

        allWizardsCache.put(cacheKey, newUnins)

        return newUnins
    }

    private fun filterLivingUnits(unit: LivingUnit, onlyEnemy: Boolean, onlyNearest: Boolean, withNeutrals: Boolean = false): Boolean {
        return !onlyEnemy || isEnemy(wizard.faction, unit) || (unit.faction == Faction.NEUTRAL && withNeutrals)
                && (!onlyNearest || abs(unit.x - wizard.x) < game.wizardCastRange * 2)
                && (!onlyNearest || abs(unit.y - wizard.y) < game.wizardCastRange * 2)
    }

    fun getAllBuldings(onlyEnemy: Boolean): List<Building> {
        val cacheKey = onlyEnemy

        if (allBuldings[cacheKey] != null) return allBuldings[cacheKey]!!

        val newUnits = world.getBuildings()
                .filter { filterLivingUnits(it, onlyEnemy, false) }

        allBuldings.put(cacheKey, newUnits)

        return newUnits
    }

    fun getAllMinions(onlyEnemy: Boolean, onlyNearest: Boolean, withNeutrals: Boolean = false): List<Minion> {
        val cacheKey = Arrays.asList(onlyEnemy, onlyNearest, withNeutrals)

        if (allMinions[cacheKey] != null) return allMinions[cacheKey]!!

        val newUnits = world.getMinions()
                .filter { filterLivingUnits(it, onlyEnemy, onlyNearest, withNeutrals) }

        allMinions.put(cacheKey, newUnits)

        return newUnits
    }

    fun getAllMovingNeutrals(): List<Minion> {
        if (allMovingNeutrals != null) return allMovingNeutrals!!

        val neutrals = world.getMinions()
                .filter { filterLivingUnits(it, onlyEnemy = false, onlyNearest = true, withNeutrals = true) }
                .filter { it.faction == Faction.NEUTRAL }
                .filter { it.speedX > 0 || it.speedY > 0 }

        allMovingNeutrals = neutrals

        return neutrals
    }

    fun getAllTrees(): List<Tree> {
        if (allTrees != null) return allTrees!!

        val newUnits = world.getTrees()
                .filter { filterLivingUnits(it, false, true) }

        allTrees = newUnits

        return newUnits
    }

    fun isEnemy(self: Faction, unit: LivingUnit): Boolean {
        return self !== unit.faction && unit.faction !== Faction.NEUTRAL
    }

    fun getNearestTarget(): LivingUnit? {
        val nearestWizard = getNearestTarget(findHelper.getAllWizards(onlyEnemy = true, onlyNearest = true))

        if (nearestWizard != null) return nearestWizard

        val nearestBuilding = getNearestTarget(findHelper.getAllBuldings(onlyEnemy = true))

        if (nearestBuilding != null) return nearestBuilding

        val nearestMinion = getNearestTarget(findHelper.getAllMinions(onlyEnemy = true, onlyNearest = true))

        if (nearestMinion != null) return nearestMinion

        val nearestMovingNeutrals = getNearestTarget(getAllMovingNeutrals())

        if (nearestMovingNeutrals!=null) return nearestMovingNeutrals

        return null
    }

    fun getNearestTarget(targets: List<LivingUnit>, minionType: MinionType? = null): LivingUnit? {
        val nearestTargets = ArrayList<LivingUnit>()

        for (target in targets) {
            if (minionType != null && target is Minion && target.type != minionType) continue

            if (abs(wizard.x - target.x) > game.wizardCastRange * CAST_RANGE_FACTOR) continue
            if (abs(wizard.y - target.y) > game.wizardCastRange * CAST_RANGE_FACTOR) continue

            val distance = wizard.getDistanceTo(target)

            if (distance <= wizard.castRange) {
                nearestTargets.add(target)
            }
        }

        return nearestTargets
                .minBy { it.life }
    }

    companion object {

        private var allUnitsCache: MutableMap<List<Boolean>, List<LivingUnit>> = HashMap()
        private var allMovingUnitsCache: MutableMap<List<Boolean>, List<LivingUnit>> = HashMap()
        private var allWizardsCache: MutableMap<List<Boolean>, List<Wizard>> = HashMap()
        private var allBuldings: MutableMap<Boolean, List<Building>> = HashMap()
        private var allMinions: MutableMap<List<Boolean>, List<Minion>> = HashMap()
        private var allMovingNeutrals: List<Minion>? = null
        private var allTrees: List<Tree>? = null

        fun clearCache() {
            allUnitsCache = HashMap<List<Boolean>, List<LivingUnit>>()
            allMovingUnitsCache = HashMap<List<Boolean>, List<LivingUnit>>()
            allWizardsCache = HashMap<List<Boolean>, List<Wizard>>()
            allBuldings = HashMap<Boolean, List<Building>>()
            allMinions = HashMap<List<Boolean>, List<Minion>>()
            allTrees = null
        }

        val CAST_RANGE_FACTOR: Double = 1.3
    }
}
