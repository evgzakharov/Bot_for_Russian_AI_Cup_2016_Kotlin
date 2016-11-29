import model.*;

import java.util.*;
import java.util.stream.Collectors;

import static java.lang.StrictMath.*;

public class MapHelper {

    public static final List<MapLine> mapLines = new ArrayList<>();
    public static final List<Point2D> mapPoints = new ArrayList<>();

    public static double mapSize = 4000D;

    public static final Point2D friendBasePoint = new Point2D(100.0D, mapSize - 100.0D);
    public static Point2D topPoint = new Point2D(300.0D, 300.0D);
    public static Point2D middlePoint = new Point2D(1659.0D, 1887);
    public static Point2D bottomPoint = new Point2D(mapSize - 100.0D, mapSize - 100.0D);
    public static Point2D enemyBasePoint = new Point2D(mapSize - 100.0D, 100.0D);

    public static MapLine topFriendLine = new MapLine(friendBasePoint, topPoint, LaneType.TOP, false);
    public static MapLine topEnemyLine = new MapLine(topPoint, enemyBasePoint, LaneType.TOP, true);

    public static MapLine middleFriendLine = new MapLine(friendBasePoint, middlePoint, LaneType.MIDDLE, false);
    public static MapLine middleEnemyLine = new MapLine(middlePoint, enemyBasePoint, LaneType.MIDDLE, true);

    public static MapLine bottomFriendLine = new MapLine(friendBasePoint, bottomPoint, LaneType.BOTTOM, false);
    public static MapLine bottomEnemyLine = new MapLine(bottomPoint, enemyBasePoint, LaneType.BOTTOM, true);

    public static MapLine artifactTopLine = new MapLine(topPoint, middlePoint, null);
    public static MapLine artifactBottomLine = new MapLine(middlePoint, bottomPoint, null);

    static {
        topFriendLine.getStartLines().addAll(Arrays.asList(middleFriendLine, bottomFriendLine));
        topFriendLine.getEndLines().addAll(Arrays.asList(topEnemyLine, artifactTopLine));

        topEnemyLine.getStartLines().addAll(Arrays.asList(topFriendLine, artifactTopLine));
        topEnemyLine.getEndLines().addAll(Arrays.asList(middleEnemyLine, bottomEnemyLine));

        middleFriendLine.getStartLines().addAll(Arrays.asList(topFriendLine, bottomFriendLine));
        middleFriendLine.getEndLines().addAll(Arrays.asList(topEnemyLine, artifactBottomLine, artifactTopLine));

        middleEnemyLine.getStartLines().addAll(Arrays.asList(topFriendLine, artifactBottomLine, artifactTopLine));
        middleEnemyLine.getEndLines().addAll(Arrays.asList(topEnemyLine, bottomEnemyLine));

        bottomFriendLine.getStartLines().addAll(Arrays.asList(topFriendLine, middleFriendLine));
        bottomFriendLine.getEndLines().addAll(Arrays.asList(artifactBottomLine, bottomEnemyLine));

        bottomEnemyLine.getStartLines().addAll(Arrays.asList(artifactBottomLine, bottomFriendLine));
        bottomEnemyLine.getEndLines().addAll(Arrays.asList(topEnemyLine, middleEnemyLine));

        artifactTopLine.getStartLines().addAll(Arrays.asList(topFriendLine, topEnemyLine));
        artifactTopLine.getEndLines().addAll(Arrays.asList(middleFriendLine, middleEnemyLine, artifactBottomLine));

        artifactBottomLine.getStartLines().addAll(Arrays.asList(middleFriendLine, middleEnemyLine, artifactTopLine));
        artifactBottomLine.getEndLines().addAll(Arrays.asList(bottomFriendLine, bottomEnemyLine));

        mapLines.addAll(Arrays.asList(
                topFriendLine, topEnemyLine, middleFriendLine, middleEnemyLine, bottomFriendLine, bottomEnemyLine, artifactTopLine, artifactBottomLine
        ));

        mapPoints.addAll(Arrays.asList(
                friendBasePoint, topPoint, middlePoint, bottomPoint, enemyBasePoint
        ));
    }

    public static final double LINE_RESOLVING_POSITION = 200D;
    public static final double LINE_RESOLVING_DISTANCE = 200D;

    public static final double DEAD_TOWER_HP_FACTOR = 0.1D;

    public static Map<Point2D, Building> deadGuardTowers = new HashMap<>();

    public Wizard wizard;
    public FindHelper findHelper;

    public MapHelper(World world, Game game, Wizard wizard) {
        this.wizard = wizard;
        this.findHelper = new FindHelper(world, game, wizard);

        updateLineInfo();
    }

    private void updateLineInfo() {
        clearLinesInfo();

        updateTowerInfo();
        updateWizardPositions();
        updateMinionPositions();
        updateStatuses();
    }

    private void clearLinesInfo() {
        mapLines.forEach(mapLine -> {
            mapLine.getEnemyWizardPositions().clear();
            mapLine.getFriendWizardPositions().clear();

            if (mapLine.getEnemy()) {
                mapLine.setEnemyPosition(0);
                mapLine.setFriendPosition(0);
            } else {
                mapLine.setEnemyPosition(mapLine.getLineLength());
                mapLine.setFriendPosition(mapLine.getLineLength());
            }

            mapLine.setDeadEnemyTowerCount(0);
            mapLine.setDeadFriendTowerCount(0);
        });
    }

    private void updateStatuses() {
        mapLines.forEach(line -> {
            if (line.getEnemyPosition() > 0) {
                line.setMapLineStatus(MapLineStatus.YELLOW);
                return;
            }

            if (line.getEnemyWizardPositions().size() > 0) {
                line.setMapLineStatus(MapLineStatus.RED);
                return;
            }

            line.setMapLineStatus(MapLineStatus.GREEN);
        });
    }

    private void updateTowerInfo() {
        List<Building> allBuldings = findHelper.getAllBuldings(false);

        allBuldings.stream()
                .filter(tower -> tower.getType().equals(BuildingType.GUARDIAN_TOWER))
                .filter(tower -> tower.getLife() < tower.getMaxLife() * DEAD_TOWER_HP_FACTOR)
                .forEach(tower -> {
                    Point2D towerPoint = new Point2D(tower.getX(), tower.getY());

                    deadGuardTowers.put(towerPoint, tower);
                });

        deadGuardTowers.forEach((point2D, tower) -> {
            boolean isEnemy = findHelper.isEnemy(wizard.getFaction(), tower);

            List<LinePosition> linePositions = getLinePositions(tower, 1.5).stream()
                    .filter(linePosition -> linePosition.getMapLine().getEnemy() == isEnemy)
                    .filter(linePosition -> linePosition.getMapLine().getLaneType() != null)
                    .filter(linePosition -> abs(getAngleTo(linePosition.getMapLine(), tower)) < PI / 6)
                    .collect(Collectors.toList());


            MapLine towerLine = linePositions.get(0).getMapLine();

            if (isEnemy)
                towerLine.setDeadEnemyTowerCount(towerLine.getDeadEnemyTowerCount() + 1);
            else
                towerLine.setDeadFriendTowerCount(towerLine.getDeadFriendTowerCount() + 1);
        });
    }

    private void updateWizardPositions() {
        List<Wizard> allWizards = findHelper.getAllWizards(false, false);

        allWizards.forEach(someWizard -> {
            List<LinePosition> linePositions = getLinePositions(someWizard, 1);

            linePositions.forEach(linePosition -> {
                MapLine wizardLine = linePosition.getMapLine();
                Point2D wizardPoint = new Point2D(someWizard.getX(), someWizard.getY());

                if (findHelper.isEnemy(wizard.getFaction(), someWizard))
                    wizardLine.getEnemyWizardPositions().put(wizardPoint, linePosition.getPosition());
                else
                    wizardLine.getFriendWizardPositions().put(wizardPoint, linePosition.getPosition());
            });
        });
    }

    private void updateMinionPositions() {
        clearMinionPosition();
        List<Minion> allMinions = findHelper.getAllMinions(false, false);

        allMinions.forEach(minion -> {
            List<LinePosition> linePositions = getLinePositions(minion, 1);

            linePositions.forEach(linePosition -> {
                MapLine minionLine = linePosition.getMapLine();
                if (findHelper.isEnemy(wizard.getFaction(), minion)) {
                    if (minionLine.getEnemyPosition() > linePosition.getPosition())
                        minionLine.setEnemyPosition(linePosition.getPosition());
                } else {
                    if (minionLine.getFriendPosition() < linePosition.getPosition())
                        minionLine.setFriendPosition(linePosition.getPosition());
                }
            });
        });
    }

    private void clearMinionPosition() {
        mapLines.forEach(mapLine -> {
            mapLine.setEnemyPosition(0);
            mapLine.setEnemyPosition(0);
        });
    }

    public List<LinePosition> getLinePositions(double x, double y, double radiusMultiplier) {
        Point2D searchingPoint = new Point2D(x, y);

        return mapLines.stream()
                .map(line -> {
                    Pair<Double, Double> distance = getDistanceFromLine(searchingPoint, line);

                    double distanceFromLine = distance.getFirst();
                    double linePosition = distance.getSecond();

                    Optional<Pair<Double, LinePosition>> resultLine;
                    if (distanceFromLine <= LINE_RESOLVING_DISTANCE * radiusMultiplier
                            && linePosition >= -LINE_RESOLVING_POSITION * radiusMultiplier && linePosition <= (line.getLineLength() + LINE_RESOLVING_POSITION * radiusMultiplier)) {
                        resultLine = Optional.of(new Pair<>(distanceFromLine, new LinePosition(line, linePosition)));
                    } else
                        resultLine = Optional.empty();

                    return resultLine;
                })
                .filter(Optional::isPresent)
                .map(value -> value.get().getSecond())
                .collect(Collectors.toList());
    }

    public List<LinePosition> getLinePositions(LivingUnit unit, double radiusMultiplier) {
        return getLinePositions(unit.getX(), unit.getY(), radiusMultiplier);
    }

    public List<LinePosition> getLinePositions(Point2D unit, double radiusMultiplier) {
        return getLinePositions(unit.getX(), unit.getY(), radiusMultiplier);
    }

    public Point2D getPointInLine(LinePosition linePosition) {
        return getPointInLine(linePosition.getMapLine(), linePosition.getPosition());
    }

    public Point2D getPointInLine(MapLine line, double nextLineDistance) {
        double newX = line.getStartPoint().getX() + cos(line.getAngle()) * nextLineDistance;
        double newY = line.getStartPoint().getY() + sin(line.getAngle()) * nextLineDistance;

        return new Point2D(newX, newY);
    }

    public Point2D getNearestPointInLine(LivingUnit unit) {
        return getNearestPointInLine(new Point2D(unit.getX(), unit.getY()));
    }

    public Point2D getNearestPointInLine(Point2D point) {
        Point2D nearestPoint = mapPoints.stream()
                .min(Comparator.comparing(pointInLine -> pointInLine.getDistanceTo(point)))
                .get();

        //TODO fix safe calculating
        List<MapLine> nearestLines = mapLines.stream()
                .filter(line -> line.getStartPoint().equals(nearestPoint) || line.getEndPoint().equals(nearestPoint))
                .collect(Collectors.toList());

        MapLine mapLine = nearestLines.stream()
                .min(Comparator.comparing(line -> ((Double) getDistanceFromLine(point, line).getFirst())))
                .get();

        return getPointInLine(mapLine, getDistanceFromLine(point, mapLine).getSecond());
    }

    private Pair<Double, Double> getDistanceFromLine(Point2D point, MapLine line) {
        double angleToPoint = getAngleTo(line, point);
        double distanceToPoint = point.getDistanceTo(line.getStartPoint());

        double distanceFromLine = abs(sin(angleToPoint) * distanceToPoint);
        double linePosition = cos(angleToPoint) * distanceToPoint;

        return new Pair<>(distanceFromLine, linePosition);
    }

    private double getAngleTo(MapLine mapLine, LivingUnit point) {
        return getAngleTo(mapLine, new Point2D(point.getX(), point.getY()));
    }

    private double getAngleTo(MapLine mapLine, Point2D point) {
        Point2D startPoint = mapLine.getStartPoint();

        double absoluteAngleTo = atan2(point.getY() - startPoint.getY(), point.getX() - startPoint.getX());
        double relativeAngleTo = absoluteAngleTo - mapLine.getAngle();

        while (relativeAngleTo > PI) {
            relativeAngleTo -= 2.0D * PI;
        }

        while (relativeAngleTo < -PI) {
            relativeAngleTo += 2.0D * PI;
        }

        return relativeAngleTo;
    }

    public Point2D getLinePointToBaseEnemy(LaneType laneType) {
        MapLine enemyLine = mapLines.stream()
                .filter(mapLine -> mapLine.getLaneType() == laneType)
                .filter(mapLine -> mapLine.getEndPoint().equals(enemyBasePoint))
                .findFirst().get();

        Boolean isInLine = getLinePositions(wizard, 1).stream()
                .anyMatch(linePosition -> linePosition.getMapLine().equals(enemyLine));

        if (isInLine)
            return enemyLine.getEndPoint();
        else
            return enemyLine.getStartPoint();
    }


}
