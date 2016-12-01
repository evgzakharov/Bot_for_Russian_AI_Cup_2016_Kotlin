import MapHelper.friendBasePoint
import MapHelper.getLinePointToBaseEnemy
import MapHelper.getLinePositions
import MapHelper.getNearestPointInLine
import MapHelper.getPointInLine
import model.Game
import model.LaneType
import model.Wizard
import model.World
import java.util.*

class MapWayFinder(world: World, game: Game, private val wizard: Wizard) {

    fun getNextWaypoint(laneType: LaneType): Point2D {
        val wizardOnLine = getLinePositions(wizard, 1.0)
                .filter { linePosition -> linePosition.mapLine.laneType === laneType }

        val linePointToBaseEnemy = getLinePointToBaseEnemy(laneType)

        val pointToMove = getPointTo(linePointToBaseEnemy, wizardOnLine.isNotEmpty(), null)

        return if (wizard.getDistanceTo(pointToMove) > POINT_IS_CLOSE) {
            getPointTo(linePointToBaseEnemy, wizardOnLine.isEmpty(), pointToMove)
        } else
            pointToMove
    }

    fun getPreviousWaypoint(laneType: LaneType): Point2D {
        val pointToMove = getPointTo(friendBasePoint, true, null)

        return if (wizard.getDistanceTo(pointToMove) < POINT_IS_CLOSE) {
            getPointTo(friendBasePoint, true, pointToMove)
        } else
            pointToMove

    }

    fun getPointTo(point: Point2D, safeWay: Boolean, nextWizardPoint: Point2D?): Point2D {
        if (!safeWay)
            return getSafePointTo(point, safeWay, nextWizardPoint)!!

        return getSafePointTo(point, safeWay, nextWizardPoint)?: getSafePointTo(point, false, nextWizardPoint)!!
    }

    fun getSafePointTo(point: Point2D, safeWay: Boolean, nextWizardPoint: Point2D?): Point2D? {
        val wizardPositions = getLinePositions(nextWizardPoint?.let { it } ?: wizard.toPoint(), 1.0)

        val pointDistance = point.getDistanceTo(nextWizardPoint?.let { it } ?: wizard.toPoint())

        if (pointDistance < WAY_FINDER_DISTANCE)
            return point

        if (wizardPositions.isEmpty()) {
            return getNearestPointInLine(nextWizardPoint?.let { it } ?: wizard.toPoint())
        } else {
            val linePositions = getLinePositions(point.x, point.y, 1.0)

            for ((wizardLine, position) in wizardPositions) {
                for (linePosition in linePositions) {
                    if (linePositions.isEmpty())
                        throw RuntimeException("how??? how?????")

                    if (wizardLine == linePosition.mapLine) {
                        if (safeWay) {
                            if (wizardLine.enemyWizardPositions.isEmpty())
                                return getPointInLine(linePosition)

                            val isSafeWay = wizardLine.enemyWizardPositions.values
                                    .none { value -> isInRange(value, position, linePosition.position) }

                            if (isSafeWay)
                                return getPointInLine(linePosition)
                        } else
                            return getPointInLine(linePosition)
                    }
                }
            }

            return wizardPositions
                    .mapNotNull { wizardPosition -> findMapWayPoint(wizardPosition, linePositions, safeWay) }
                    .minBy { it.first }?.second
        }
    }

    private fun isInRange(checkedValue: Double, start: Double, end: Double): Boolean {
        val minValue = StrictMath.min(start, end)
        val maxValue = StrictMath.max(start, end)

        if (checkedValue < minValue || checkedValue > maxValue) return false
        return true
    }

    private fun findMapWayPoint(wizardLinePosition: LinePosition, pointLinePositions: List<LinePosition>, safeWay: Boolean): Pair<Double, Point2D>? {
        val wizardMapLine = wizardLinePosition.mapLine

        val checkWays = listOf(
                wizardMapLine.startPoint to wizardLinePosition.position,
                wizardMapLine.endPoint to (wizardMapLine.lineLength - wizardLinePosition.position)
        )

        return checkWays.mapNotNull { checkWay ->
            val wayParam = WayParams(
                    startMapLine = wizardMapLine,
                    checkedLines = listOf(wizardMapLine),
                    isSafeWay = safeWay,
                    pointLinePositions = pointLinePositions,
                    startDestination = checkWay.second,
                    startPoint = checkWay.first
            )

            findMapWayPoint(wayParam)
        }.minBy { it.first }?.let { value ->
            value.first to getPointFromMapLine(wizardLinePosition, value.second)
        }
    }

    private fun getPointFromMapLine(wizardLinePosition: LinePosition, line: MapLine): Point2D {
        var positionToLinePoint: Double? = wizardLinePosition.position
        var wayPoint = wizardLinePosition.mapLine.startPoint
        if (!wizardLinePosition.mapLine.startLines.contains(line)) {
            positionToLinePoint = wizardLinePosition.mapLine.lineLength - positionToLinePoint!!
            wayPoint = wizardLinePosition.mapLine.endPoint
        }

        if ((positionToLinePoint ?: 0.0) >= NEXT_LINE_DISTANCE) {
            return wayPoint
        } else {
            var linePosition = NEXT_LINE_DISTANCE * NEXT_LINE_DISTANCE_MULTIPLIER
            if (line.endPoint == wayPoint)
                linePosition = line.lineLength - linePosition

            return getPointInLine(LinePosition(line, linePosition))
        }
    }


    private fun findMapWayPoint(wayParams: WayParams): Pair<Double, MapLine>? {
        if (wayParams.checkedLines.size > 3) return null

        val mapLine = wayParams.startMapLine

        var furtherLines = mapLine.startLines
        if (wayParams.startPoint == mapLine.endPoint)
            furtherLines = mapLine.endLines

        val filteredFurtherLines: List<MapLine> = furtherLines
                .filter { furtherLine -> !wayParams.checkedLines.contains(furtherLine) }

        if (filteredFurtherLines.isEmpty()) return null

        val findWay: LinePosition? = wayParams.pointLinePositions
                .filter { linePosition ->
                    val checkedMapLine = linePosition.mapLine
                    if (filteredFurtherLines.contains(checkedMapLine)) {
                        if (!wayParams.isSafeWay) return@filter true

                        if (checkedMapLine.startPoint == wayParams.startPoint)
                            return@filter checkedMapLine.enemyWizardPositions.values
                                    .all { position ->
                                        linePosition.position < position
                                    }
                        else
                            return@filter checkedMapLine.enemyWizardPositions.values
                                    .all { position ->
                                        linePosition.position > position
                                    }
                    } else
                        false
                }.firstOrNull()

        if (findWay != null) {
            var newLength = wayParams.startDestination!!
            if (wayParams.startPoint == findWay.mapLine.startPoint)
                newLength += findWay.position
            else
                newLength += findWay.mapLine.lineLength - findWay.position

            return newLength to (wayParams.mapLine ?: findWay.mapLine)
        } else {
            return filteredFurtherLines
                    .mapNotNull { furtherLine ->
                        val newWayLines = ArrayList<MapLine>()
                        newWayLines.addAll(wayParams.checkedLines)
                        newWayLines.add(furtherLine)

                        var nextPoint = furtherLine.startPoint
                        if (furtherLine.startPoint != wayParams.startMapLine.endPoint)
                            nextPoint = furtherLine.endPoint

                        val newWayParams = WayParams(
                                startMapLine = furtherLine,
                                startPoint = nextPoint,
                                startDestination = wayParams.startDestination!! + furtherLine.lineLength,
                                pointLinePositions = wayParams.pointLinePositions,
                                mapLine = wayParams.mapLine ?: furtherLine,
                                isSafeWay = wayParams.isSafeWay,
                                checkedLines = newWayLines
                        )

                        findMapWayPoint(newWayParams)
                    }.minBy { it.first }
        }
    }

    companion object {

        val NEXT_LINE_DISTANCE: Double = 350.0
        val WAY_FINDER_DISTANCE = 150.0

        val NEXT_LINE_DISTANCE_MULTIPLIER = 1.1

        val POINT_IS_CLOSE: Double = 30.0
    }


}
