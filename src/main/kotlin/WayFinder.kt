import model.*

import java.util.ArrayList
import java.util.Collections
import java.util.Optional

import java.lang.StrictMath.abs

class WayFinder(private val wizard: Wizard, private val world: World, private val game: Game) {
    private val findHelper: FindHelper

    private val matrixStep: Double

    init {
        this.matrixStep = wizard.radius / 2
        this.findHelper = FindHelper(world, game, wizard)
    }

    fun findWay(point: Point2D): List<Point2D> {
        val matrixStart = Matrix(Point2D(wizard.x, wizard.y), MatrixPoint(0, 0), null)
        matrixStart.pathCount = 0.0

        val allUnits = findHelper.getAllUnits(true, false, true)

        val findLine = growMatrix(listOf(matrixStart), point, allUnits)

        return findLine
    }

    private fun growMatrix(stepPoints: List<Matrix>, findingWayPoint: Point2D, allUnits: List<LivingUnit>): List<Point2D> {
        val newStepPoints = ArrayList<Matrix>()

        var lastMatrix: Matrix? = null
        for (stepMatrix in stepPoints) {
            for (diffX in -1..1) {
                for (diffY in -1..1) {
                    if (diffX == 0 && diffY == 0) continue

                    val matrixPoint = MatrixPoint((diffX + stepMatrix.matrixPoint.diffX).toShort(),
                            (diffY + stepMatrix.matrixPoint.diffY).toShort())

                    val newPathCount = (stepMatrix.pathCount + 1.0 + abs(diffX * diffY) * 0.5).toFloat()

                    if (stepMatrix.matrixPoints!!.keys.contains(matrixPoint)) {
                        val matrixAtPoint = stepMatrix.matrixPoints!![matrixPoint]
                        if (matrixAtPoint!!.pathCount < newPathCount)
                            continue
                    }

                    val newX = stepMatrix.point.x + matrixStep * diffX
                    val newY = stepMatrix.point.y + matrixStep * diffY
                    val newPoint = Point2D(newX, newY)

                    if (!checkPointPosition(newPoint)) continue

                    val newMatrix = Matrix(newPoint, matrixPoint, stepMatrix)
                    newMatrix.pathCount = newPathCount.toDouble()

                    if (!freeLocation(newPoint, allUnits)) continue

                    newStepPoints.add(newMatrix)
                    newMatrix.matrixPoints!!.put(matrixPoint, newMatrix)
                    lastMatrix = newMatrix

                    if (newPoint.getDistanceTo(findingWayPoint) <= matrixStep)
                        return findLineFromMatrix(newMatrix)
                }
            }
        }

        if (!newStepPoints.isEmpty()) {
            val point2DS = growMatrix(newStepPoints, findingWayPoint, allUnits)
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

    private fun freeLocation(newPoint: Point2D, allUnits: List<LivingUnit>): Boolean {
        return allUnits
                .filter { unit -> abs(unit.x - wizard.x) < game.wizardCastRange }
                .filter { unit -> abs(unit.x - wizard.x) < game.wizardCastRange }
                .filter { unit -> isFractionBase(unit) || abs(unit.x - newPoint.x) < MAX_RANGE }
                .filter { unit -> isFractionBase(unit) || abs(unit.y - newPoint.y) < MAX_RANGE }
                .any { unit -> newPoint.getDistanceTo(unit) <= getUnitDistance(unit) + wizard.radius + MIN_CLOSEST_RANGE }
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

        private val MAX_RANGE = 90.0
        private val MIN_CLOSEST_RANGE = 5.0
    }

}
