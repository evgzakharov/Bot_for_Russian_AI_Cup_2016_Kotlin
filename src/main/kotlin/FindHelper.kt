import model.*

import java.util.*
import java.util.function.Predicate
import java.util.stream.Collectors

import java.lang.StrictMath.abs

class FindHelper(private val world: World, private val game: Game, private val wizard: Wizard) {

    fun getAllUnits(withTrees: Boolean, onlyEnemy: Boolean, onlyNearest: Boolean): List<LivingUnit> {
        val cacheKey = listOf(withTrees, onlyEnemy, onlyNearest)
        if (allUnitsCache[cacheKey] != null) return allUnitsCache[cacheKey]!!

        val units = mutableListOf<LivingUnit>()

        units.addAll(getAllWizards(onlyEnemy, onlyNearest))
        units.addAll(getAllBuldings(onlyEnemy))
        units.addAll(getAllMinions(onlyEnemy, onlyNearest))

        if (withTrees)
            units.addAll(Arrays.asList(*world.getTrees()))

        allUnitsCache.put(cacheKey, units)

        return units
    }

    fun getAllMovingUnits(onlyEnemy: Boolean, onlyNearest: Boolean): List<LivingUnit> {
        val cacheKey = listOf(onlyEnemy, onlyNearest)
        if (allMovingUnitsCache[cacheKey] != null) return allMovingUnitsCache[cacheKey]!!

        val units = ArrayList<LivingUnit>()

        units.addAll(getAllWizards(onlyEnemy, onlyNearest))
        units.addAll(getAllMinions(onlyEnemy, onlyNearest))

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

    private fun filterLivingUnits(unit: LivingUnit, onlyEnemy: Boolean, onlyNearest: Boolean): Boolean {
        return !onlyEnemy || isEnemy(wizard.faction, unit)
                && (!onlyNearest || abs(unit.x - wizard.x) < game.wizardCastRange * 3)
                && (!onlyNearest || abs(unit.y - wizard.y) < game.wizardCastRange * 3)
    }

    fun getAllBuldings(onlyEnemy: Boolean): List<Building> {
        val cacheKey = onlyEnemy

        if (allBuldings[cacheKey] != null) return allBuldings[cacheKey]!!

        val newUnits = world.getBuildings()
                .filter { filterLivingUnits(it, onlyEnemy, false) }

        allBuldings.put(cacheKey, newUnits)

        return newUnits
    }

    fun getAllMinions(onlyEnemy: Boolean, onlyNearest: Boolean): List<Minion> {
        val cacheKey = Arrays.asList(onlyEnemy, onlyNearest)

        if (allMinions[cacheKey] != null) return allMinions[cacheKey]!!

        val newUnits = world.getMinions()
                .filter { filterLivingUnits(it, onlyEnemy, onlyNearest) }

        allMinions.put(cacheKey, newUnits)

        return newUnits
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

    val nearestEnemy: LivingUnit?
        get() {
            val nearestWizard = getNearestTarget(world.getWizards())

            if (nearestWizard != null) return nearestWizard

            val nearestBuilding = getNearestTarget(world.getBuildings())

            if (nearestBuilding != null)
                return nearestBuilding
            else
                return getNearestTarget(world.getMinions())
        }

    fun getNearestTarget(targets: Array<out LivingUnit>): LivingUnit? {
        val nearestTargets = ArrayList<LivingUnit>()

        for (target in targets) {
            if (!isEnemy(wizard.faction, target)) {
                continue
            }

            if (abs(wizard.x - target.x) > game.wizardCastRange * 2) continue
            if (abs(wizard.y - target.y) > game.wizardCastRange * 2) continue

            val distance = wizard.getDistanceTo(target)

            if (distance < wizard.castRange) {
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
        private var allTrees: List<Tree>? = null

        fun clearCache() {
            allUnitsCache = HashMap<List<Boolean>, List<LivingUnit>>()
            allMovingUnitsCache = HashMap<List<Boolean>, List<LivingUnit>>()
            allWizardsCache = HashMap<List<Boolean>, List<Wizard>>()
            allBuldings = HashMap<Boolean, List<Building>>()
            allMinions = HashMap<List<Boolean>, List<Minion>>()
            allTrees = null
        }
    }
}
