import model.*
import java.lang.StrictMath.*
import java.util.*

class MapHelper(world: World, game: Game, var wizard: Wizard) {
    var findHelper: FindHelper

    init {
        this.findHelper = FindHelper(world, game, wizard)

        updateLineInfo()
    }

    private fun updateLineInfo() {
        clearLinesInfo()

        updateTowerInfo()
        updateWizardPositions()
        updateMinionPositions()
        updateStatuses()
    }

    private fun clearLinesInfo() {
        mapLines.forEach { mapLine ->
            mapLine.enemyWizardPositions.clear()
            mapLine.friendWizardPositions.clear()

            if (mapLine.enemy!!) {
                mapLine.enemyPosition = 0.0
                mapLine.friendPosition = 0.0
            } else {
                mapLine.enemyPosition = mapLine.lineLength
                mapLine.friendPosition = mapLine.lineLength
            }

            mapLine.deadEnemyTowerCount = 0
            mapLine.deadFriendTowerCount = 0
        }
    }

    private fun updateStatuses() {
        mapLines.forEach { line ->
            if (line.enemyPosition > 0) {
                line.mapLineStatus = MapLineStatus.YELLOW
                return@forEach
            }

            if (line.enemyWizardPositions.isNotEmpty()) {
                line.mapLineStatus = MapLineStatus.RED
                return@forEach
            }

            line.mapLineStatus = MapLineStatus.GREEN
        }
    }

    private fun updateTowerInfo() {
        val allBuldings = findHelper.getAllBuldings(false)

        allBuldings
                .filter { tower -> tower.type == BuildingType.GUARDIAN_TOWER }
                .filter { tower -> tower.life < tower.maxLife * DEAD_TOWER_HP_FACTOR }
                .forEach { tower ->
                    val towerPoint = Point2D(tower.x, tower.y)

                    deadGuardTowers.put(towerPoint, tower)
                }

        deadGuardTowers.forEach { value ->
            val (poinn2D, tower) = value
            val isEnemy = findHelper.isEnemy(wizard.faction, tower)

            val linePositions = getLinePositions(tower, 1.5)
                    .filter { linePosition -> linePosition.mapLine.enemy == isEnemy }
                    .filter { linePosition -> linePosition.mapLine.laneType != null }
                    .filter { linePosition -> abs(getAngleTo(linePosition.mapLine, tower)) < PI / 6 }

            val towerLine = linePositions[0].mapLine

            if (isEnemy)
                towerLine.deadEnemyTowerCount += 1
            else
                towerLine.deadFriendTowerCount += 1
        }
    }

    private fun updateWizardPositions() {
        val allWizards = findHelper.getAllWizards(false, false)

        allWizards.forEach { someWizard ->
            val linePositions = getLinePositions(someWizard, 1.0)

            linePositions.forEach { linePosition ->
                val wizardLine = linePosition.mapLine
                val wizardPoint = Point2D(someWizard.x, someWizard.y)

                if (findHelper.isEnemy(wizard.faction, someWizard))
                    wizardLine.enemyWizardPositions.put(wizardPoint, linePosition.position)
                else
                    wizardLine.friendWizardPositions.put(wizardPoint, linePosition.position)
            }
        }
    }

    private fun updateMinionPositions() {
        clearMinionPosition()
        val allMinions = findHelper.getAllMinions(false, false)

        allMinions.forEach { minion ->
            val linePositions = getLinePositions(minion, 1.0)

            linePositions.forEach { linePosition ->
                val minionLine = linePosition.mapLine
                if (findHelper.isEnemy(wizard.faction, minion)) {
                    if (minionLine.enemyPosition > linePosition.position)
                        minionLine.enemyPosition = linePosition.position
                } else {
                    if (minionLine.friendPosition < linePosition.position)
                        minionLine.friendPosition = linePosition.position
                }
            }
        }
    }

    private fun clearMinionPosition() {
        mapLines.forEach { mapLine ->
            mapLine.enemyPosition = 0.0
            mapLine.enemyPosition = 0.0
        }
    }

    fun getLinePositions(x: Double, y: Double, radiusMultiplier: Double): List<LinePosition> {
        val searchingPoint = Point2D(x, y)

        return mapLines
                .map { line ->
                    val distance = getDistanceFromLine(searchingPoint, line)

                    val distanceFromLine = distance.first
                    val linePosition = distance.second

                    val resultLine: Optional<Pair<Double, LinePosition>>
                    if (distanceFromLine <= LINE_RESOLVING_DISTANCE * radiusMultiplier
                            && linePosition >= -LINE_RESOLVING_POSITION * radiusMultiplier && linePosition <= line.lineLength + LINE_RESOLVING_POSITION * radiusMultiplier) {
                        resultLine = Optional.of(Pair(distanceFromLine, LinePosition(line, linePosition)))
                    } else
                        resultLine = Optional.empty<Pair<Double, LinePosition>>()

                    resultLine
                }
                .filter { it.isPresent }
                .map({ value -> value.get().second })
    }

    fun getLinePositions(unit: LivingUnit, radiusMultiplier: Double): List<LinePosition> {
        return getLinePositions(unit.x, unit.y, radiusMultiplier)
    }

    fun getLinePositions(unit: Point2D, radiusMultiplier: Double): List<LinePosition> {
        return getLinePositions(unit.x, unit.y, radiusMultiplier)
    }

    fun getPointInLine(linePosition: LinePosition): Point2D {
        return getPointInLine(linePosition.mapLine, linePosition.position)
    }

    fun getPointInLine(line: MapLine, nextLineDistance: Double): Point2D {
        val newX = line.startPoint.x + cos(line.angle) * nextLineDistance
        val newY = line.startPoint.y + sin(line.angle) * nextLineDistance

        return Point2D(newX, newY)
    }

    fun getNearestPointInLine(unit: LivingUnit): Point2D {
        return getNearestPointInLine(Point2D(unit.x, unit.y))
    }

    fun getNearestPointInLine(point: Point2D): Point2D {
        val nearestPoint = mapPoints
                .minBy { pointInLine -> pointInLine.getDistanceTo(point) }

        //TODO fix safe calculating
        val nearestLines = mapLines
                .filter { line -> line.startPoint == nearestPoint || line.endPoint == nearestPoint }

        val mapLine = nearestLines
                .minBy { line -> getDistanceFromLine(point, line).first }

        return getPointInLine(mapLine!!, getDistanceFromLine(point, mapLine).second)
    }

    private fun getDistanceFromLine(point: Point2D, line: MapLine): Pair<Double, Double> {
        val angleToPoint = getAngleTo(line, point)
        val distanceToPoint = point.getDistanceTo(line.startPoint)

        val distanceFromLine = abs(sin(angleToPoint) * distanceToPoint)
        val linePosition = cos(angleToPoint) * distanceToPoint

        return Pair(distanceFromLine, linePosition)
    }

    private fun getAngleTo(mapLine: MapLine, point: LivingUnit): Double {
        return getAngleTo(mapLine, Point2D(point.x, point.y))
    }

    private fun getAngleTo(mapLine: MapLine, point: Point2D): Double {
        val startPoint = mapLine.startPoint

        val absoluteAngleTo = atan2(point.y - startPoint.y, point.x - startPoint.x)
        var relativeAngleTo = absoluteAngleTo - mapLine.angle

        while (relativeAngleTo > PI) {
            relativeAngleTo -= 2.0 * PI
        }

        while (relativeAngleTo < -PI) {
            relativeAngleTo += 2.0 * PI
        }

        return relativeAngleTo
    }

    fun getLinePointToBaseEnemy(laneType: LaneType): Point2D {
        val enemyLine = mapLines
                .filter { mapLine -> mapLine.laneType === laneType }
                .filter { mapLine -> mapLine.endPoint == enemyBasePoint }
                .first()

        val isInLine = getLinePositions(wizard, 1.0)
                .any { linePosition -> linePosition.mapLine == enemyLine }

        if (isInLine)
            return enemyLine.endPoint
        else
            return enemyLine.startPoint
    }

    companion object {

        val mapLines: MutableList<MapLine> = ArrayList()
        val mapPoints: MutableList<Point2D> = ArrayList()

        var mapSize = 4000.0

        val friendBasePoint = Point2D(100.0, mapSize - 100.0)
        var topPoint = Point2D(300.0, 300.0)
        var middlePoint = Point2D(1659.0, 1887.0)
        var bottomPoint = Point2D(mapSize - 100.0, mapSize - 100.0)
        var enemyBasePoint = Point2D(mapSize - 100.0, 100.0)

        var topFriendLine = MapLine(friendBasePoint, topPoint, LaneType.TOP, false)
        var topEnemyLine = MapLine(topPoint, enemyBasePoint, LaneType.TOP, true)

        var middleFriendLine = MapLine(friendBasePoint, middlePoint, LaneType.MIDDLE, false)
        var middleEnemyLine = MapLine(middlePoint, enemyBasePoint, LaneType.MIDDLE, true)

        var bottomFriendLine = MapLine(friendBasePoint, bottomPoint, LaneType.BOTTOM, false)
        var bottomEnemyLine = MapLine(bottomPoint, enemyBasePoint, LaneType.BOTTOM, true)

        var artifactTopLine = MapLine(topPoint, middlePoint, null)
        var artifactBottomLine = MapLine(middlePoint, bottomPoint, null)

        init {
            topFriendLine.startLines.addAll(Arrays.asList(middleFriendLine, bottomFriendLine))
            topFriendLine.endLines.addAll(Arrays.asList(topEnemyLine, artifactTopLine))

            topEnemyLine.startLines.addAll(Arrays.asList(topFriendLine, artifactTopLine))
            topEnemyLine.endLines.addAll(Arrays.asList(middleEnemyLine, bottomEnemyLine))

            middleFriendLine.startLines.addAll(Arrays.asList(topFriendLine, bottomFriendLine))
            middleFriendLine.endLines.addAll(Arrays.asList(topEnemyLine, artifactBottomLine, artifactTopLine))

            middleEnemyLine.startLines.addAll(Arrays.asList(topFriendLine, artifactBottomLine, artifactTopLine))
            middleEnemyLine.endLines.addAll(Arrays.asList(topEnemyLine, bottomEnemyLine))

            bottomFriendLine.startLines.addAll(Arrays.asList(topFriendLine, middleFriendLine))
            bottomFriendLine.endLines.addAll(Arrays.asList(artifactBottomLine, bottomEnemyLine))

            bottomEnemyLine.startLines.addAll(Arrays.asList(artifactBottomLine, bottomFriendLine))
            bottomEnemyLine.endLines.addAll(Arrays.asList(topEnemyLine, middleEnemyLine))

            artifactTopLine.startLines.addAll(Arrays.asList(topFriendLine, topEnemyLine))
            artifactTopLine.endLines.addAll(Arrays.asList(middleFriendLine, middleEnemyLine, artifactBottomLine))

            artifactBottomLine.startLines.addAll(Arrays.asList(middleFriendLine, middleEnemyLine, artifactTopLine))
            artifactBottomLine.endLines.addAll(Arrays.asList(bottomFriendLine, bottomEnemyLine))

            mapLines.addAll(Arrays.asList(
                    topFriendLine, topEnemyLine, middleFriendLine, middleEnemyLine, bottomFriendLine, bottomEnemyLine, artifactTopLine, artifactBottomLine
            ))

            mapPoints.addAll(Arrays.asList(
                    friendBasePoint, topPoint, middlePoint, bottomPoint, enemyBasePoint
            ))
        }

        val LINE_RESOLVING_POSITION = 200.0
        val LINE_RESOLVING_DISTANCE = 200.0

        val DEAD_TOWER_HP_FACTOR = 0.1

        var deadGuardTowers: MutableMap<Point2D, Building> = HashMap()
    }


}
