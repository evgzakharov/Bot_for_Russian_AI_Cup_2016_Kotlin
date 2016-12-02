import model.*

import java.lang.StrictMath.abs
import java.util.*

class WayFinder(private val wizard: Wizard, private val world: World, private val game: Game) {
    private val findHelper: FindHelper

    private val matrixStep: Double

    init {
        this.matrixStep = wizard.radius * WIZARD_RADIUS_FACTOR
        this.findHelper = FindHelper(world, game, wizard)
    }

    fun findWay(point: Point2D): List<Point2D> {
        val matrixStart = Matrix(Point2D(wizard.x, wizard.y), MatrixPoint(0, 0), null)
        matrixStart.pathCount = 0.0

        val allMovingUnits = findHelper.getAllMovingUnits(onlyEnemy = false, onlyNearest = true, withNeutrals = true)
        val allBuildings = findHelper.getAllBuldings(onlyEnemy = false)
        val allTrees = findHelper.getAllTrees()

        val findLine = growMatrix(listOf(matrixStart), point, allMovingUnits, allBuildings, allTrees)

        return findLine
    }

    private fun growMatrix(stepPoints: List<Matrix>, findingWayPoint: Point2D, allMovingUnits: List<LivingUnit>, allBuildings: List<LivingUnit>, allTrees: List<LivingUnit>): List<Point2D> {
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

                    val freeLocation = freeLocation(newPoint, allTrees)
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

                    if (!freeLocation(newPoint, allMovingUnits).second) continue
                    if (!freeLocation(newPoint, allBuildings).second) continue

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
            val point2DS = growMatrix(newStepPoints, findingWayPoint, allMovingUnits, allBuildings, allTrees)
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

    private fun freeLocation(newPoint: Point2D, units: List<LivingUnit>): Pair<LivingUnit?, Boolean> {
        var closestUnit: LivingUnit? = null

        val isFree = units
                .filter { unit -> isFractionBase(unit) || abs(unit.x - newPoint.x) <= unit.radius + wizard.radius + MIN_CLOSEST_RANGE_FOR_DISTANCE }
                .filter { unit -> isFractionBase(unit) || abs(unit.x - newPoint.x) <= unit.radius + wizard.radius + MIN_CLOSEST_RANGE_FOR_DISTANCE }
                .filter { unit -> abs(unit.x - newPoint.x) < MAX_RANGE }
                .filter { unit -> abs(unit.y - newPoint.y) < MAX_RANGE }
                .none { unit ->
                    closestUnit = unit
                    newPoint.getDistanceTo(unit) <= getUnitDistance(unit) + wizard.radius + MIN_CLOSEST_RANGE
                }

        return closestUnit to isFree
    }

    private fun getUnitDistance(unit: LivingUnit): Double {
        if (isFractionBase(unit)) {
            return unit.radius + MIN_CLOSEST_RANGE * 5
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

        val MAX_RANGE = 250.0

        private val WIZARD_RADIUS_FACTOR: Double = 0.9

        private val MIN_CLOSEST_RANGE = 5.0

        private val MIN_CLOSEST_RANGE_FOR_DISTANCE = 5.0

        private val MULTIPLIER_FACTOR_DIFF = 4
        private val MULTIPLIER_FACTOR_DIFF_V2 = 8

        private val MULTIPLIER_FACTOR = 4.0
        private val MULTIPLIER_FACTOR_V2 = 10.0

        private val MINIMUM_TREE_RADIUS: Double = 28.0
        private val TREE_MULTIPLIER_FACTOR_SMALL_TREE = 5.0
        private val TREE_MULTIPLIER_FACTOR_BIG_TREE = 15.0

        private val MAX_TREE_COUNT = 2.0
    }

}
