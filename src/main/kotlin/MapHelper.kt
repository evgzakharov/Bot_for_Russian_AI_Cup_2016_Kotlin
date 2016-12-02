import MapHelper.HISTORY_TICK_COUNT
import model.*
import java.lang.StrictMath.*
import java.util.*

data class AttackLine(val friend: MapLine, val enemy: MapLine)

object MapHelper {
    lateinit var world: World
    lateinit var game: Game
    lateinit var wizard: Wizard
    lateinit var findHelper: FindHelper

    var lastCheckPointToMoveTick: Int = 0
    var lastPointToMove: Point2D? = null
    var lastLaneToMove: LaneType? = null

    fun nextTick(world: World, game: Game, wizard: Wizard) {
        this.game = game
        this.world = world
        this.wizard = wizard
        this.findHelper = FindHelper(world, game, wizard)

        updateLineInfo()
    }

    private fun updateLineInfo() {
        clearLinesInfo()

        updateTowerInfo()
        updateWizardPositions()
        updateMinionPositions()
    }

    private fun clearLinesInfo() {
        mapLines.forEach { mapLine ->
            mapLine.enemyWizardPositions.clear()
            mapLine.friendWizardPositions.clear()

            mapLine.historyEnemyWizardPositions.filterByTick(world.tickIndex)
            mapLine.historyFriendWizardPositions.filterByTick(world.tickIndex)

            if (mapLine.enemy) {
                mapLine.enemyPosition = mapLine.lineLength
                mapLine.friendPosition = null
            } else {
                mapLine.enemyPosition = null
                mapLine.friendPosition = 0.0
            }

            mapLine.deadEnemyTowerCount = 0
            mapLine.deadFriendTowerCount = 0
        }
    }

    private fun updateTowerInfo() {
        val allBuldings = findHelper.getAllBuldings(false)

        allBuldings
                .filter { tower -> tower.type == BuildingType.GUARDIAN_TOWER }
                .filter { tower -> tower.life < tower.maxLife * DEAD_TOWER_HP_FACTOR }
                .forEach { tower ->
                    deadGuardTowers.put(tower.id, tower)
                }

        deadGuardTowers.forEach { value ->
            val (poinn2D, tower) = value
            val isEnemy = findHelper.isEnemy(wizard.faction, tower)

            val linePosition = getLinePositions(tower, 5.0)
                    .filter { linePosition -> linePosition.mapLine.enemy == isEnemy }
                    .filter { linePosition -> linePosition.mapLine.laneType != null }
                    .filter { linePosition -> linePosition.position >= 0 && linePosition.position <= linePosition.mapLine.lineLength }
                    .filter { linePosition -> abs(linePosition.mapLine.getAngleTo(tower)) < PI / 5 }
                    .minBy { getDistanceFromLine(tower.toPoint(), it.mapLine).first }

            if (linePosition == null)
                throw RuntimeException("invalid tower")

            val towerLine = linePosition.mapLine

            if (isEnemy)
                towerLine.deadEnemyTowerCount += 1
            else
                towerLine.deadFriendTowerCount += 1

            if (towerLine.deadFriendTowerCount > 2 || towerLine.deadEnemyTowerCount > 2)
                throw RuntimeException("omg.. ")
        }
    }

    private fun updateWizardPositions() {
        val allWizards = (findHelper.getAllWizards(false, false) + wizard).toSet()

        allWizards.forEach { someWizard ->
            val linePositions = getLinePositions(someWizard, 1.0)

            linePositions.forEach { linePosition ->
                val wizardLine = linePosition.mapLine
                val wizardId = someWizard.id

                if (findHelper.isEnemy(wizard.faction, someWizard)) {
                    wizardLine.enemyWizardPositions.put(wizardId, linePosition.position)
                    wizardLine.historyEnemyWizardPositions.put(wizardId, HistoryValue(world.tickIndex, linePosition.position))
                } else {
                    wizardLine.friendWizardPositions.put(wizardId, linePosition.position)
                    wizardLine.historyFriendWizardPositions.put(wizardId, HistoryValue(world.tickIndex, linePosition.position))
                }
            }
        }
    }

    private fun updateMinionPositions() {
        clearMinionPosition()
        val allMinions = findHelper.getAllMinions(false, false)

        allMinions.forEach { minion ->
            val linePositions = getLinePositions(minion, 2.0)

            linePositions.forEach { linePosition ->
                val minionLine = linePosition.mapLine
                if (findHelper.isEnemy(wizard.faction, minion)) {
                    if (minionLine.enemyPosition == null || (minionLine.enemyPosition ?: minionLine.lineLength) < linePosition.position)
                        minionLine.enemyPosition = linePosition.position
                } else {
                    if (minionLine.friendPosition == null || (minionLine.friendPosition ?: 0.0) < linePosition.position)
                        minionLine.friendPosition = linePosition.position
                }
            }
        }
    }

    private fun clearMinionPosition() {
        mapLines.forEach { mapLine ->
            if (mapLine.enemy) {
                mapLine.enemyPosition = mapLine.lineLength
                mapLine.friendPosition = null
            } else {
                mapLine.enemyPosition = null
                mapLine.friendPosition = 0.0
            }
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
                .map { value -> value.get().second }
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
        val angleToPoint = line.getAngleTo(point)
        val distanceToPoint = point.getDistanceTo(line.startPoint)

        val distanceFromLine = abs(sin(angleToPoint) * distanceToPoint)
        val linePosition = cos(angleToPoint) * distanceToPoint

        return Pair(distanceFromLine, linePosition)
    }

    private fun MapLine.getAngleTo(point: LivingUnit): Double {
        return this.getAngleTo(Point2D(point.x, point.y))
    }

    private fun MapLine.getAngleTo(point: Point2D): Double {
        val startPoint = this.startPoint

        val absoluteAngleTo = atan2(point.y - startPoint.y, point.x - startPoint.x)
        var relativeAngleTo = absoluteAngleTo - this.angle

        while (relativeAngleTo > PI) {
            relativeAngleTo -= 2.0 * PI
        }

        while (relativeAngleTo < -PI) {
            relativeAngleTo += 2.0 * PI
        }

        return relativeAngleTo
    }

    fun getLinePointToBaseEnemy(laneType: LaneType): Point2D {
        if (world.tickIndex - lastCheckPointToMoveTick < CHANGE_POINT_TO_MOVE_MIN_TICK_DIFF
                && lastPointToMove != null && lastLaneToMove == laneType)
            return lastPointToMove!!

        val lane = attackLines[laneType]!!

        lastCheckPointToMoveTick = world.tickIndex
        lastLaneToMove = laneType

        lastPointToMove = if (lane.enemy.friendPosition != null)
            getPointInLine(lane.enemy, lane.enemy.friendPosition!! + MOVE_FORWARD)
        else if (lane.friend.friendPosition != 0.0)
            if (lane.friend.friendPosition!! < lane.friend.lineLength * LINE_BACK_FACTOR)
                getPointInLine(lane.friend, lane.friend.enemyPosition ?: lane.friend.friendPosition!!)
            else
                getPointInLine(lane.friend, max(lane.friend.friendPosition ?: 0.0, lane.friend.enemyPosition ?: 0.0) + MOVE_FORWARD)
        else {
            getPointInLine(lane.friend, lane.friend.lineLength - START_POINT_POSITON)
        }

        return lastPointToMove!!
    }


    val mapLines: MutableList<MapLine> = mutableListOf()

    val attackLines: MutableMap<LaneType, AttackLine> = mutableMapOf()

    val mapPoints: MutableList<Point2D> = mutableListOf()

    var mapSize = 4000.0

    val friendBasePoint = Point2D(150.0, mapSize - 150.0)
    var topPoint = Point2D(300.0, 300.0)
    var middlePoint = Point2D(2000.0, 2000.0)
    var bottomPoint = Point2D(mapSize - 300.0, mapSize - 300.0)
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

        attackLines.put(LaneType.TOP, AttackLine(topFriendLine, topEnemyLine))
        attackLines.put(LaneType.MIDDLE, AttackLine(middleFriendLine, middleEnemyLine))
        attackLines.put(LaneType.BOTTOM, AttackLine(bottomFriendLine, bottomEnemyLine))
    }

    val LINE_RESOLVING_POSITION = 250.0
    val LINE_RESOLVING_DISTANCE = 250.0

    val DEAD_TOWER_HP_FACTOR = 0.1

    val START_POINT_POSITON = 300.0

    val MOVE_FORWARD = 200.0

    val LINE_BACK_FACTOR: Double = 0.5

    val CHANGE_POINT_TO_MOVE_MIN_TICK_DIFF: Int = 50

    val HISTORY_TICK_COUNT: Int = 2000

    var deadGuardTowers: MutableMap<Long, Building> = HashMap()
}

fun MutableMap<Long, HistoryValue>.filterByTick(currentTick: Int) {
    val iterator = this.iterator()
    while(iterator.hasNext()){
        val currentValue = iterator.next()

        if (currentTick - currentValue.value.tick > HISTORY_TICK_COUNT)
            iterator.remove()
    }
}

fun Map<Long, HistoryValue>.toData(): Map<Long, Double> {
    return this.mapValues { it.value.value }
}
