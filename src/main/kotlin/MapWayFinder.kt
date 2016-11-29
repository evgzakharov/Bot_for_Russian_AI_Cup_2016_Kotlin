import model.Game
import model.LaneType
import model.Wizard
import model.World
import java.util.*

class MapWayFinder(world: World, game: Game, private val wizard: Wizard) {

    private val mapHelper: MapHelper

    init {
        this.mapHelper = MapHelper(world, game, wizard)
    }

    fun getNextWaypoint(laneType: LaneType): Point2D {
        val wizardOnLine = mapHelper.getLinePositions(wizard, 1.0)
                .filter { linePosition -> linePosition.mapLine.laneType === laneType }

        val linePointToBaseEnemy = mapHelper.getLinePointToBaseEnemy(laneType)
        if (wizardOnLine.isNotEmpty()) {
            return getPointTo(linePointToBaseEnemy, false)
        } else
            return getPointTo(linePointToBaseEnemy, true)

    }

    fun getPreviousWaypoint(laneType: LaneType): Point2D {
        return getPointTo(MapHelper.Companion.friendBasePoint, true)
    }

    fun getPointTo(point: Point2D, safeWay: Boolean): Point2D {
        val wizardPositions = mapHelper.getLinePositions(wizard, 1.0)

        val pointDistance = point.getDistanceTo(wizard)

        if (pointDistance < WAY_FINDER_DISTANCE)
            return point

        if (wizardPositions.isEmpty()) {
            return mapHelper.getNearestPointInLine(wizard)
        } else {
            val linePositions = mapHelper.getLinePositions(point.x, point.y, 1.0)

            for ((wizardLine, position) in wizardPositions) {
                for (linePosition in linePositions) {
                    if (linePositions.isEmpty())
                        throw RuntimeException("asfd")

                    if (wizardLine == linePosition.mapLine) {
                        if (safeWay) {
                            if (wizardLine.enemyWizardPositions.isEmpty())
                                return mapHelper.getPointInLine(linePosition)

                            val isSafeWay = wizardLine.enemyWizardPositions.values
                                    .none { value -> isInRange(value!!, position, linePosition.position) }

                            if (isSafeWay)
                                return mapHelper.getPointInLine(linePosition)
                        } else
                            return mapHelper.getPointInLine(linePosition)
                    }
                }
            }

            val findWayPoint = wizardPositions
                    .mapNotNull { wizardPosition -> findMapWayPoint(wizardPosition, linePositions, safeWay) }
                    .minBy { it.first }

            if (findWayPoint != null && safeWay)
                return wizardPositions
                        .mapNotNull { wizardPosition -> findMapWayPoint(wizardPosition, linePositions, false) }
                        .minBy { it.first }?.second!!

            return findWayPoint!!.second
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

        val startLines = wizardMapLine.startLines
        val endLines = wizardMapLine.endLines

        if (safeWay) {
            val enemyWizardPositions = wizardMapLine.enemyWizardPositions

            if (enemyWizardPositions.isNotEmpty()) {
                val isStartSafe = enemyWizardPositions.values
                        .any { enemyWizardsLocation -> enemyWizardsLocation >= wizardLinePosition.position }

                if (!isStartSafe)
                    startLines.clear()

                val isEndSafe = enemyWizardPositions.values
                        .any { enemyWizardsLocation -> enemyWizardsLocation <= wizardLinePosition.position }

                if (!isEndSafe)
                    endLines.clear()
            }
        }

        val allLines: List<MapLine> = startLines + endLines

        val searchingLines: List<MapLine> = pointLinePositions.map { it.mapLine }

        var findWays: List<Pair<Double, MapLine>> = allLines
                .filter { searchingLines.contains(it) }
                .map { mapLine ->
                    val pointPosition = pointLinePositions
                            .filter { linePosition -> linePosition.mapLine == mapLine }
                            .first()

                    mapLine to pointPosition
                }
                .map { mapLine ->
                    val ifStartLine = startLines.contains(mapLine.first)
                    var startDistance = wizardLinePosition.position
                    if (!ifStartLine)
                        startDistance = wizardMapLine.lineLength - wizardLinePosition.position

                    if (mapLine.first.startPoint == wizardMapLine.startPoint || mapLine.first.startPoint == wizardMapLine.endPoint)
                        (startDistance + mapLine.second.position) to mapLine.first
                    else
                        (startDistance + (mapLine.first.lineLength - mapLine.second.position)) to mapLine.first
                }

        findWays = if (findWays.isEmpty()) {
            val startWays = if (startLines.isNotEmpty()) {
                val startPoint = wizardMapLine.startPoint
                val startPosition = wizardLinePosition.position

                getWays(pointLinePositions, safeWay, startLines, startPoint, startPosition, wizardMapLine)
            } else
                emptyList()

            val endWays = if (endLines.isNotEmpty()) {
                getWays(
                        pointLinePositions, safeWay, endLines, wizardMapLine.endPoint,
                        wizardMapLine.lineLength - wizardLinePosition.position, wizardMapLine)
            } else
                emptyList()

            startWays + endWays
        } else
            findWays


        val findValue = findWays
                .minBy { it.first }

        return findValue?.let { findValue.first to getPointFromMapLine(wizardLinePosition, findValue.second) }
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

            return mapHelper.getPointInLine(LinePosition(line, linePosition))
        }
    }


    private fun getWays(
            pointLinePositions: List<LinePosition>,
            safeWay: Boolean,
            startLines: List<MapLine>,
            startPoint: Point2D,
            startPosition: Double,
            wizardLine: MapLine): List<Pair<Double, MapLine>> {
        return startLines
                .mapNotNull { mapLine ->
                    val checkedLines = ArrayList<MapLine>()
                    checkedLines.add(wizardLine)

                    val wayParams = WayParams(
                            mapLine,
                            mapLine,
                            startPoint,
                            startPosition,
                            pointLinePositions,
                            safeWay,
                            checkedLines
                    )

                    findMapWayPoint(wayParams)
                }
    }

    private fun findMapWayPoint(wayParams: WayParams): Pair<Double, MapLine>? {
        if (wayParams.checkedLines.size > 3) return null

        val mapLine = wayParams.mapLine

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

                        if (checkedMapLine.mapLineStatus == MapLineStatus.GREEN) return@filter true

                        if (checkedMapLine.startPoint == wayParams.startPoint)
                            return@filter checkedMapLine.enemyWizardPositions.values
                                    .all { position -> linePosition.position < position }
                        else
                            return@filter checkedMapLine.enemyWizardPositions.values
                                    .all { position -> linePosition.position < position }
                    } else
                        false
                }.firstOrNull()

        if (findWay != null) {
            var newLength = wayParams.startDestination!!
            if (wayParams.startPoint == findWay.mapLine.startPoint)
                newLength += findWay.position
            else
                newLength += findWay.mapLine.lineLength - findWay.position

            return newLength to wayParams.startMapLine
        } else {
            return filteredFurtherLines
                    .mapNotNull { furtherLine ->
                        val newWayLines = ArrayList<MapLine>()
                        newWayLines.addAll(wayParams.checkedLines)
                        newWayLines.add(furtherLine)

                        var nextPoint = furtherLine.startPoint
                        if (furtherLine.startPoint != wayParams.mapLine.endPoint)
                            nextPoint = furtherLine.endPoint

                        val newWayParams = WayParams(
                                wayParams.startMapLine,
                                furtherLine,
                                nextPoint,
                                wayParams.startDestination!! + wayParams.mapLine.lineLength,
                                wayParams.pointLinePositions,
                                wayParams.isSafeWay,
                                newWayLines
                        )

                        findMapWayPoint(newWayParams)
                    }.minBy { it.first }
        }
    }

    companion object {

        val NEXT_LINE_DISTANCE: Double = 300.0
        val WAY_FINDER_DISTANCE = 250.0

        val NEXT_LINE_DISTANCE_MULTIPLIER = 1.1
    }


}
