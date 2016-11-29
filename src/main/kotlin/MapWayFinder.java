import model.Game;
import model.LaneType;
import model.Wizard;
import model.World;

import java.util.*;
import java.util.stream.Collectors;

public class MapWayFinder {

    private World world;
    private Game game;
    private Wizard wizard;
    private MapHelper mapHelper;

    public static final double NEXT_LINE_DISTANCE = 300D;
    public static final double WAY_FINDER_DISTANCE = 250D;

    public static final double NEXT_LINE_DISTANCE_MULTIPLIER = 1.1;

    public MapWayFinder(World world, Game game, Wizard wizard) {
        this.wizard = wizard;
        this.mapHelper = new MapHelper(world, game, wizard);
    }

    public Point2D getNextWaypoint(LaneType laneType) {
        List<LinePosition> wizardOnLine = mapHelper.getLinePositions(wizard, 1).stream()
                .filter(linePosition -> linePosition.getMapLine().getLaneType() == laneType)
                .collect(Collectors.toList());

        Point2D linePointToBaseEnemy = mapHelper.getLinePointToBaseEnemy(laneType);
        if (wizardOnLine.size() > 0) {
            return getPointTo(linePointToBaseEnemy, false);
        } else
            return getPointTo(linePointToBaseEnemy, true);

    }

    public Point2D getPreviousWaypoint(LaneType laneType) {
        return getPointTo(mapHelper.friendBasePoint, true);
    }

    public Point2D getPointTo(Point2D point, boolean safeWay) {
        List<LinePosition> wizardPositions = mapHelper.getLinePositions(wizard, 1);

        double pointDistance = point.getDistanceTo(wizard);

        if (pointDistance < WAY_FINDER_DISTANCE)
            return point;

        if (wizardPositions == null || wizardPositions.size() == 0) {
            return mapHelper.getNearestPointInLine(wizard);
        } else {
            List<LinePosition> linePositions = mapHelper.getLinePositions(point.getX(), point.getY(), 1);

            for (LinePosition wizardPosition : wizardPositions) {
                for (LinePosition linePosition : linePositions) {
                    if (linePositions.isEmpty())
                        throw new RuntimeException("asfd");

                    MapLine wizardLine = wizardPosition.getMapLine();
                    if (wizardLine.equals(linePosition.getMapLine())) {
                        if (safeWay) {
                            if (wizardLine.getEnemyWizardPositions().isEmpty())
                                return mapHelper.getPointInLine(linePosition);

                            boolean isSafeWay = wizardLine.getEnemyWizardPositions().values().stream()
                                    .noneMatch(value -> isInRange(value, wizardPosition.getPosition(), linePosition.getPosition()));

                            if (isSafeWay)
                                return mapHelper.getPointInLine(linePosition);
                        } else
                            return mapHelper.getPointInLine(linePosition);
                    }
                }
            }

            Optional<Pair<Double, Point2D>> findWayPoint = wizardPositions.stream()
                    .map(wizardPosition -> findMapWayPoint(wizardPosition, linePositions, safeWay))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .min(Comparator.comparing(Pair::getFirst));

            if (!findWayPoint.isPresent() && safeWay)
                return wizardPositions.stream()
                        .map(wizardPosition -> findMapWayPoint(wizardPosition, linePositions, false))
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .min(Comparator.comparing(Pair::getFirst)).get().getSecond();

            return findWayPoint.get().getSecond();
        }
    }

    private boolean isInRange(double checkedValue, double start, double end) {
        double minValue = StrictMath.min(start, end);
        double maxValue = StrictMath.max(start, end);

        if (checkedValue < minValue || checkedValue > maxValue) return false;
        return true;
    }

    private Optional<Pair<Double, Point2D>> findMapWayPoint(LinePosition wizardLinePosition, List<LinePosition> pointLinePositions, boolean safeWay) {
        MapLine wizardMapLine = wizardLinePosition.getMapLine();

        final List<MapLine> startLines = wizardMapLine.getStartLines();
        final List<MapLine> endLines = wizardMapLine.getEndLines();

        if (safeWay) {
            Map<Point2D, Double> enemyWizardPositions = wizardMapLine.getEnemyWizardPositions();

            if (enemyWizardPositions.size() > 0) {
                boolean isStartSafe = enemyWizardPositions.values().stream()
                        .allMatch(enemyWizardsLocation -> enemyWizardsLocation >= wizardLinePosition.getPosition());

                if (!isStartSafe)
                    startLines.clear();

                boolean isEndSafe = enemyWizardPositions.values().stream()
                        .allMatch(enemyWizardsLocation -> enemyWizardsLocation <= wizardLinePosition.getPosition());

                if (!isEndSafe)
                    endLines.clear();
            }
        }

        List<MapLine> allLines = new ArrayList<>();
        allLines.addAll(startLines);
        allLines.addAll(endLines);

        List<MapLine> searchingLines = pointLinePositions.stream().map(LinePosition::getMapLine)
                .collect(Collectors.toList());


        List<Pair<Double, MapLine>> findWays = allLines.stream()
                .filter(searchingLines::contains)
                .map(mapLine -> {
                    LinePosition pointPosition = pointLinePositions.stream()
                            .filter(linePosition -> linePosition.getMapLine().equals(mapLine))
                            .findFirst().get();

                    return new Pair<>(mapLine, pointPosition);
                })
                .filter(mapLine -> {
                    if (!safeWay) return true;

                    if (mapLine.getFirst().getMapLineStatus() == MapLineStatus.GREEN) return true;

                    if (mapLine.getFirst().getStartPoint().equals(wizardMapLine.getStartPoint())
                            || mapLine.getFirst().getStartPoint().equals(wizardMapLine.getEndPoint()))
                        return mapLine.getFirst().getEnemyWizardPositions().values().stream()
                                .anyMatch(position -> mapLine.getSecond().getPosition() < position);
                    else
                        return mapLine.getFirst().getEnemyWizardPositions().values().stream()
                                .anyMatch(position -> mapLine.getSecond().getPosition() > position);
                })
                .map(mapLine -> {
                    boolean ifStartLine = startLines.contains(mapLine.getFirst());
                    double startDistance = wizardLinePosition.getPosition();
                    if (!ifStartLine)
                        startDistance = wizardMapLine.getLineLength() - wizardLinePosition.getPosition();

                    if (mapLine.getFirst().getStartPoint().equals(wizardMapLine.getStartPoint())
                            || mapLine.getFirst().getStartPoint().equals(wizardMapLine.getEndPoint()))
                        return new Pair<>(startDistance + mapLine.getSecond().getPosition(), mapLine.getFirst());
                    else
                        return new Pair<>(startDistance + (mapLine.getFirst().getLineLength() - mapLine.getSecond().getPosition()),
                                mapLine.getFirst());
                })
                .collect(Collectors.toList());

        if (findWays.isEmpty()) {
            if (startLines != null) {
                Point2D startPoint = wizardMapLine.getStartPoint();
                double startPosition = wizardLinePosition.getPosition();

                List<Pair<Double, MapLine>> startWays = getWays(
                        pointLinePositions, safeWay, startLines, startPoint, startPosition, wizardMapLine);

                findWays.addAll(startWays);
            }

            if (endLines != null) {
                List<Pair<Double, MapLine>> stopWays = getWays(
                        pointLinePositions, safeWay, endLines, wizardMapLine.getEndPoint(),
                        wizardMapLine.getLineLength() - wizardLinePosition.getPosition(), wizardMapLine);

                findWays.addAll(stopWays);
            }
        }


        Optional<Pair<Double, Point2D>> minValue = findWays.stream()
                .min(Comparator.comparing(Pair::getFirst))
                .map(pair -> new Pair<>(pair.getFirst(), getPointFromMapLine(wizardLinePosition, pair.getSecond())));

        return minValue;
    }

    private Point2D getPointFromMapLine(LinePosition wizardLinePosition, MapLine line) {
        Double positionToLinePoint = wizardLinePosition.getPosition();
        Point2D wayPoint = wizardLinePosition.getMapLine().getStartPoint();
        if (!wizardLinePosition.getMapLine().getStartLines().contains(line)) {
            positionToLinePoint = wizardLinePosition.getMapLine().getLineLength() - positionToLinePoint;
            wayPoint = wizardLinePosition.getMapLine().getEndPoint();
        }

        if (positionToLinePoint >= NEXT_LINE_DISTANCE) {
            return wayPoint;
        } else {
            double linePosition = NEXT_LINE_DISTANCE * NEXT_LINE_DISTANCE_MULTIPLIER;
            if (line.getEndPoint().equals(wayPoint))
                linePosition = line.getLineLength() - linePosition;

            return mapHelper.getPointInLine(new LinePosition(line, linePosition));
        }
    }


    private List<Pair<Double, MapLine>> getWays(List<LinePosition> pointLinePositions, boolean safeWay, List<MapLine> startLines, Point2D startPoint, double startPosition, MapLine wizardLine) {
        return startLines.stream()
                .map(mapLine -> {
                    List<MapLine> checkedLines = new ArrayList<>();
                    checkedLines.add(wizardLine);

                    WayParams wayParams = new WayParams(
                            mapLine,
                            mapLine,
                            startPoint,
                            startPosition,
                            pointLinePositions,
                            safeWay,
                            checkedLines
                    );

                    return findMapWayPoint(wayParams);
                })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    private Optional<Pair<Double, MapLine>> findMapWayPoint(WayParams wayParams) {
        if (wayParams.getCheckedLines().size() > 3) return Optional.empty();

        MapLine mapLine = wayParams.getMapLine();

        List<MapLine> furtherLines = mapLine.getStartLines();
        if (wayParams.getStartPoint().equals(mapLine.getEndPoint()))
            furtherLines = mapLine.getEndLines();

        List<MapLine> filteredFurtherLines = furtherLines.stream()
                .filter(furtherLine -> !wayParams.getCheckedLines().contains(furtherLine))
                .collect(Collectors.toList());

        if (filteredFurtherLines.isEmpty()) return Optional.empty();

        Optional<LinePosition> findWay = wayParams.getPointLinePositions().stream()
                .filter(linePosition -> {
                    MapLine checkedMapLine = linePosition.getMapLine();
                    if (filteredFurtherLines.contains(checkedMapLine)) {
                        if (!wayParams.isSafeWay()) return true;

                        if (checkedMapLine.getMapLineStatus() == MapLineStatus.GREEN) return true;

                        if (checkedMapLine.getStartPoint().equals(wayParams.getStartPoint()))
                            return checkedMapLine.getEnemyWizardPositions().values().stream()
                                    .anyMatch(position -> linePosition.getPosition() < position);
                        else
                            return checkedMapLine.getEnemyWizardPositions().values().stream()
                                    .anyMatch(position -> linePosition.getPosition() > position);

                    } else
                        return false;
                })
                .findFirst();

        if (findWay.isPresent()) {
            double newLength = wayParams.getStartDestination();
            if (wayParams.getStartPoint().equals(findWay.get().getMapLine().getStartPoint()))
                newLength += findWay.get().getPosition();
            else
                newLength += findWay.get().getMapLine().getLineLength() - findWay.get().getPosition();

            return Optional.of(new Pair<>(newLength, wayParams.getStartMapLine()));
        } else {
            return filteredFurtherLines.stream()
                    .map(furtherLine -> {
                        List<MapLine> newWayLines = new ArrayList<>();
                        newWayLines.addAll(wayParams.getCheckedLines());
                        newWayLines.add(furtherLine);

                        Point2D nextPoint = furtherLine.getStartPoint();
                        if (!furtherLine.getStartPoint().equals(wayParams.getMapLine().getEndPoint()))
                            nextPoint = furtherLine.getEndPoint();

                        WayParams newWayParams = new WayParams(
                                wayParams.getStartMapLine(),
                                furtherLine,
                                nextPoint,
                                wayParams.getStartDestination() + wayParams.getMapLine().getLineLength(),
                                wayParams.getPointLinePositions(),
                                wayParams.isSafeWay(),
                                newWayLines
                        );

                        return findMapWayPoint(newWayParams);
                    })
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .min(Comparator.comparing(Pair::getFirst));
        }

    }


}
