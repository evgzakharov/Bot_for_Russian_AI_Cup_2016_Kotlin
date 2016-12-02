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

class MapWayFinder(val world: World, val game: Game, private val wizard: Wizard) {

    fun getNextWaypoint(laneType: LaneType, globalStrateg: GlobalStrateg): Point2D {
        val wizardOnLine = getLinePositions(wizard, 1.0)
                .filter { linePosition -> linePosition.mapLine.laneType === laneType }

        val linePointToBaseEnemy = getLinePointToBaseEnemy(laneType, globalStrateg)

        val safeWay = wizardOnLine.isEmpty() || wizard.getDistanceTo(linePointToBaseEnemy) > MAX_SAFE_DISTANCE
                || globalStrateg == GlobalStrateg.DEFENCE

        val pointToMove = getPointTo(linePointToBaseEnemy, safeWay)

        return if (wizard.getDistanceTo(pointToMove) > MAX_SELF_WAY_RANGE) {
            getPointTo(correctPoint(pointToMove), safeWay)
        } else
            pointToMove
    }

    private fun correctPoint(point: Point2D): Point2D {
        if (world.tickIndex - lastNextCheck >= MIN_TICK_DIFF_TO_FIND_POINT || previousNextPoint == null) {
            lastNextCheck = world.tickIndex
            previousNextPoint = point
        }

        return previousNextPoint!!
    }

    fun getPreviousWaypoint(laneType: LaneType): Point2D {
        return if (wizard.getDistanceTo(friendBasePoint) > MAX_SELF_WAY_RANGE) {
            getPointTo(friendBasePoint, true)
        } else
            friendBasePoint

    }

    fun getPointTo(point: Point2D, safeWay: Boolean): Point2D {
        if (!safeWay)
            return getSafePointTo(point, safeWay)!!

        return getSafePointTo(point, safeWay) ?: getSafePointTo(point, false)!!
    }

    fun getSafePointTo(point: Point2D, safeWay: Boolean): Point2D? {
        val wizardPositions = getLinePositions(wizard.toPoint(), 1.0)

        val pointDistance = wizard.getDistanceTo(point)

        if (pointDistance < WAY_FINDER_DISTANCE)
            return point

        if (wizardPositions.isEmpty()) {
            return getNearestPointInLine(wizard.toPoint())
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

                            if (wizardLine.startPoint == friendBasePoint && wizardLine.enemyWizardPositions
                                    .all { it.value < wizardLine.lineLength * BASE_POINT_POSITION_FACTOR })
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
                                    } || ((checkedMapLine.startPoint == friendBasePoint) && checkedMapLine.enemyWizardPositions.values
                                    .all { position ->
                                        linePosition.position < position || position < checkedMapLine.lineLength * BASE_POINT_POSITION_FACTOR
                                    })
                        else
                            return@filter checkedMapLine.enemyWizardPositions.values
                                    .all { position ->
                                        linePosition.position > position
                                    } || ((checkedMapLine.startPoint == friendBasePoint) && checkedMapLine.enemyWizardPositions.values
                                    .all { position ->
                                        linePosition.position > position || position < checkedMapLine.lineLength * BASE_POINT_POSITION_FACTOR
                                    })
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

        var previousNextPoint: Point2D? = null
        var lastNextCheck: Int = 0

        val MAX_SELF_WAY_RANGE = 150

        val MIN_TICK_DIFF_TO_FIND_POINT = 50

        val NEXT_LINE_DISTANCE: Double = 450.0

        val WAY_FINDER_DISTANCE = 150.0

        val NEXT_LINE_DISTANCE_MULTIPLIER = 1.1

        val BASE_POINT_POSITION_FACTOR: Double = 0.3

        val MAX_SAFE_DISTANCE: Double = 1000.0
    }


}
