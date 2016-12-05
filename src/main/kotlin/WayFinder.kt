import model.*

import java.lang.StrictMath.abs
import java.lang.StrictMath.max
import java.util.*

class WayFinder(private val wizard: Wizard, private val world: World, private val game: Game) {
    private val findHelper: FindHelper

    private val matrixStep: Double

    init {
        this.matrixStep = wizard.radius * WIZARD_RADIUS_FACTOR
        this.findHelper = FindHelper(world, game, wizard)
    }

    fun findWay(point: Point2D, increaseDistanceToMinion: Boolean): Point2D? {
        val matrixStart = Matrix(Point2D(wizard.x, wizard.y), MatrixPoint(0, 0), null)
        matrixStart.pathCount = 0.0

        val allMinions = findHelper.getAllMinions(onlyEnemy = false, onlyNearest = true, withNeutrals = true)
        val allWizards = findHelper.getAllWizards(onlyEnemy = false, onlyNearest = false)
        val allBuildings = findHelper.getAllBuldings(onlyEnemy = false)
        val allTrees = findHelper.getAllTrees().filter {
            max(abs(it.x - wizard.x), abs(it.y - wizard.y)) < MAX_RANGE
        }

        val findLine = growMatrix(listOf(matrixStart), point, allWizards, allMinions, allBuildings, allTrees, increaseDistanceToMinion)

        return findLine.firstOrNull()
    }

    private fun growMatrix(
            stepPoints: List<Matrix>,
            findingWayPoint: Point2D,
            allWizards: List<LivingUnit>,
            allMinions: List<LivingUnit>,
            allBuildings: List<LivingUnit>,
            allTrees: List<LivingUnit>,
            increaseDistanceToMinion: Boolean): List<Point2D> {
        val newStepPoints = ArrayList<Matrix>()

        var lastMatrix: Matrix? = null
        for (stepMatrix in stepPoints) {
            for (diffX in -1..1) {
                for (diffY in -1..1) {
                    if (diffX == 0 && diffY == 0) continue

                    val matrixPoint = MatrixPoint((diffX + stepMatrix.matrixPoint.diffX),
                            (diffY + stepMatrix.matrixPoint.diffY))

                    val notNearest = (abs(matrixPoint.diffX) > MULTIPLIER_FACTOR_DIFF
                            || abs(matrixPoint.diffY) > MULTIPLIER_FACTOR_DIFF)

                    val veryLargeDistance = (abs(matrixPoint.diffX) > MULTIPLIER_FACTOR_DIFF_V2
                            || abs(matrixPoint.diffY) > MULTIPLIER_FACTOR_DIFF_V2)

                    val distanceMultiplier = if (notNearest)
                        MULTIPLIER_FACTOR
                    else if (veryLargeDistance)
                        MULTIPLIER_FACTOR_V2
                    else 1.0

                    val newX = stepMatrix.point.x + matrixStep * diffX * distanceMultiplier
                    val newY = stepMatrix.point.y + matrixStep * diffY * distanceMultiplier
                    val newPoint = Point2D(newX, newY)

                    val freeLocation = freeLocation(newPoint, allTrees, distanceMultiplier)
                    val treeMultiplier = if (!freeLocation.second)
                        if (freeLocation.first!!.radius < MINIMUM_TREE_RADIUS) TREE_MULTIPLIER_FACTOR_SMALL_TREE
                        else TREE_MULTIPLIER_FACTOR_BIG_TREE
                    else 1.0

                    val step = 1.0 + abs(diffX * diffY) * 0.5

                    val newPathCount = (stepMatrix.pathCount + step * distanceMultiplier * treeMultiplier).toFloat()

                    if (newPathCount > TREE_MULTIPLIER_FACTOR_BIG_TREE * MAX_TREE_COUNT)
                        continue

                    if (stepMatrix.matrixPoints!!.keys.contains(matrixPoint)) {
                        val matrixAtPoint = stepMatrix.matrixPoints!![matrixPoint]
                        if (matrixAtPoint!!.pathCount < newPathCount)
                            continue
                    }

                    if (!checkPointPosition(newPoint)) continue

                    if (!freeLocation(newPoint, allMinions, distanceMultiplier, increaseDistanceToMinion).second) continue
                    if (!freeLocation(newPoint, allWizards, distanceMultiplier).second) continue
                    if (!freeLocation(newPoint, allBuildings, distanceMultiplier).second) continue

                    val newMatrix = Matrix(newPoint, matrixPoint, stepMatrix)

                    newStepPoints.add(newMatrix)
                    newMatrix.pathCount = newPathCount.toDouble()
                    newMatrix.matrixPoints!!.put(matrixPoint, newMatrix)
                    lastMatrix = newMatrix

                    if (newPoint.getDistanceTo(findingWayPoint) <= matrixStep)
                        return findLineFromMatrix(newMatrix)
                }
            }
        }

        if (!newStepPoints.isEmpty()) {
            val point2DS = growMatrix(newStepPoints, findingWayPoint, allWizards, allMinions, allBuildings, allTrees, increaseDistanceToMinion)
            if (point2DS.isEmpty()) {
                if (stepPoints.size == 1 && lastMatrix != null && lastMatrix.matrixPoints!!.isNotEmpty()) {
                    val nearestMatrix = lastMatrix.matrixPoints!!.values
                            .minBy { it.point.getDistanceTo(findingWayPoint) }

                    if (nearestMatrix != null)
                        return findLineFromMatrix(nearestMatrix)
                }
            }
        }

        return emptyList()
    }

    private fun freeLocation(newPoint: Point2D, units: List<LivingUnit>, multiply: Double, increaseDistance: Boolean = false): Pair<LivingUnit?, Boolean> {
        var closestUnit: LivingUnit? = null
        val wizardRadius = wizard.radius * multiply

        val closestDistance = if (increaseDistance)
            MIN_INCREASE_CLOSEST_RANGE_FOR_DISTANCE
        else
            MIN_CLOSEST_RANGE_FOR_DISTANCE

        val isFree = units
                .filter { unit -> isFractionBase(unit) || abs(unit.x - newPoint.x) <= unit.radius + wizardRadius + closestDistance }
                .filter { unit -> isFractionBase(unit) || abs(unit.x - newPoint.x) <= unit.radius + wizardRadius + closestDistance }
                .filter { unit -> abs(unit.x - newPoint.x) < MAX_RANGE }
                .filter { unit -> abs(unit.y - newPoint.y) < MAX_RANGE }
                .none { unit ->
                    closestUnit = unit
                    newPoint.getDistanceTo(unit) <= getUnitDistance(unit) + wizardRadius + closestDistance
                }

        return closestUnit to isFree
    }

    private fun getUnitDistance(unit: LivingUnit): Double {
        if (isFractionBase(unit)) {
            return unit.radius + MIN_CLOSEST_BASE_RANGE
        }

        return unit.radius
    }

    private fun isFractionBase(unit: LivingUnit): Boolean {
        if (unit is Building) {

            if (unit.type === BuildingType.FACTION_BASE)
                return true
        }
        return false
    }

    private fun findLineFromMatrix(stepMatrix: Matrix): List<Point2D> {
        val findPoints = ArrayList<Point2D>()

        var currentMatrix: Matrix? = stepMatrix
        while (currentMatrix != null) {
            findPoints.add(0, currentMatrix.point)
            currentMatrix = currentMatrix.previousMatrix

            if (currentMatrix!!.previousMatrix == null)
                break
        }

        return findPoints
    }

    private fun checkPointPosition(newPoint: Point2D): Boolean {
        return inRange(newPoint.x, wizard.x, world.width) && inRange(newPoint.y, wizard.y, world.height)
    }

    private fun inRange(newValue: Double, wizardValue: Double, limit: Double): Boolean {
        return abs(wizardValue - newValue) <= MAX_RANGE
                && newValue - wizard.radius - MIN_CLOSEST_RANGE >= 0
                && newValue + wizard.radius + MIN_CLOSEST_RANGE <= limit
    }

    companion object {

        private val MAX_RANGE = 180.0

        private val WIZARD_RADIUS_FACTOR: Double = 0.5

        private val MIN_CLOSEST_RANGE = 5.0

        private val MIN_CLOSEST_RANGE_FOR_DISTANCE = 5.0
        private val MIN_INCREASE_CLOSEST_RANGE_FOR_DISTANCE = 15.0

        private val MULTIPLIER_FACTOR_DIFF = 3
        private val MULTIPLIER_FACTOR_DIFF_V2 = 6

        private val MULTIPLIER_FACTOR = 2.0
        private val MULTIPLIER_FACTOR_V2 = 2.0

        private val MINIMUM_TREE_RADIUS: Double = 15.0
        private val TREE_MULTIPLIER_FACTOR_SMALL_TREE = 5.0
        private val TREE_MULTIPLIER_FACTOR_BIG_TREE = 15.0

        private val MAX_TREE_COUNT = 5.0

        private val MIN_CLOSEST_BASE_RANGE = 11.0
    }

}
