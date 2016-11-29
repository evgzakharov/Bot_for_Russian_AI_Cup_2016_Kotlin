import model.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.lang.StrictMath.abs;

public class WayFinder {

    private Wizard wizard;
    private World world;
    private Game game;
    private FindHelper findHelper;

    private double matrixStep;

    private static final double MAX_RANGE = 90;
    private static final double MIN_CLOSEST_RANGE = 5;

    public WayFinder(Wizard wizard, World world, Game game) {
        this.wizard = wizard;
        this.world = world;
        this.game = game;
        this.matrixStep = wizard.getRadius() / 2;
        this.findHelper = new FindHelper(world, game, wizard);
    }

    public List<Point2D> findWay(Point2D point) {
        Matrix matrixStart = new Matrix(new Point2D(wizard.getX(), wizard.getY()), new MatrixPoint(), null);
        matrixStart.setPathCount(0);

        List<LivingUnit> allUnits = findHelper.getAllUnits(true, false, true);

        List<Point2D> findLine = growMatrix(Collections.singletonList(matrixStart), point, allUnits);

        return findLine;
    }

    private List<Point2D> growMatrix(List<Matrix> stepPoints, Point2D findingWayPoint, List<LivingUnit> allUnits) {
        List<Matrix> newStepPoints = new ArrayList<>();

        Matrix lastMatrix = null;
        for (Matrix stepMatrix : stepPoints) {
            for (short diffX = -1; diffX <= 1; diffX++) {
                for (short diffY = -1; diffY <= 1; diffY++) {
                    if (diffX == 0 && diffY == 0) continue;

                    MatrixPoint matrixPoint = new MatrixPoint((short) (diffX + stepMatrix.getMatrixPoint().getDiffX()),
                            (short) (diffY + stepMatrix.getMatrixPoint().getDiffY()));

                    float newPathCount = (float) (stepMatrix.getPathCount() + 1 + abs(diffX * diffY) * 0.5);

                    if (stepMatrix.getMatrixPoints().keySet().contains(matrixPoint)) {
                        Matrix matrixAtPoint = stepMatrix.getMatrixPoints().get(matrixPoint);
                        if (matrixAtPoint.getPathCount() < newPathCount)
                            continue;
                    }

                    double newX = stepMatrix.getPoint().getX() + matrixStep * diffX;
                    double newY = stepMatrix.getPoint().getY() + matrixStep * diffY;
                    Point2D newPoint = new Point2D(newX, newY);

                    if (!checkPointPosition(newPoint, findingWayPoint)) continue;

                    Matrix newMatrix = new Matrix(newPoint, matrixPoint, stepMatrix);
                    newMatrix.setPathCount(newPathCount);

                    if (!freeLocation(newPoint, allUnits)) continue;

                    newStepPoints.add(newMatrix);
                    newMatrix.getMatrixPoints().put(matrixPoint, newMatrix);
                    lastMatrix = newMatrix;

                    if (newPoint.getDistanceTo(findingWayPoint) <= matrixStep)
                        return findLineFromMatrix(newMatrix);
                }
            }
        }

        if (!newStepPoints.isEmpty()) {
            List<Point2D> point2DS = growMatrix(newStepPoints, findingWayPoint, allUnits);
            if (point2DS.isEmpty()) {
                if (stepPoints.size() == 1 && lastMatrix != null && lastMatrix.getMatrixPoints().size() > 0) {
                    Optional<Matrix> nearestMatrix = lastMatrix.getMatrixPoints().values().stream()
                            .min((o1, o2) -> ((Double) o1.getPoint().getDistanceTo(findingWayPoint)).compareTo(o2.getPoint().getDistanceTo(findingWayPoint)));

                    if (nearestMatrix.isPresent())
                        return findLineFromMatrix(nearestMatrix.get());
                }
            }
        }

        return Collections.emptyList();
    }

    private boolean freeLocation(Point2D newPoint, List<LivingUnit> allUnits) {
        return allUnits.stream()
                .filter(unit -> abs(unit.getX() - wizard.getX()) < game.getWizardCastRange())
                .filter(unit -> abs(unit.getX() - wizard.getX()) < game.getWizardCastRange())
                .filter(unit -> isFractionBase(unit) || abs(unit.getX() - newPoint.getX()) < MAX_RANGE)
                .filter(unit -> isFractionBase(unit) || abs(unit.getY() - newPoint.getY()) < MAX_RANGE)
                .noneMatch(unit -> newPoint.getDistanceTo(unit) <= getUnitDistance(unit) + wizard.getRadius() + MIN_CLOSEST_RANGE);
    }

    private double getUnitDistance(LivingUnit unit) {
        if (isFractionBase(unit)) {
            return unit.getRadius() + MIN_CLOSEST_RANGE * 5;
        }

        return unit.getRadius();
    }

    private boolean isFractionBase(LivingUnit unit) {
        if (unit instanceof Building) {
            Building building = (Building) unit;

            if (building.getType() == BuildingType.FACTION_BASE)
                return true;
        }
        return false;
    }

    private List<Point2D> findLineFromMatrix(Matrix stepMatrix) {
        List<Point2D> findPoints = new ArrayList<>();

        Matrix currentMatrix = stepMatrix;
        while (currentMatrix != null) {
            findPoints.add(0, currentMatrix.getPoint());
            currentMatrix = currentMatrix.getPreviousMatrix();

            if (currentMatrix.getPreviousMatrix() == null)
                break;
        }

        return findPoints;
    }

    private boolean checkPointPosition(Point2D newPoint, Point2D wayPoint) {
        return inRange(newPoint.getX(), wayPoint.getX(), wizard.getX(), world.getWidth()) &&
                inRange(newPoint.getY(), wayPoint.getY(), wizard.getY(), world.getHeight());
    }

    private boolean inRange(double newValue, double wayPointValue, double wizardValue, double limit) {
        return abs(wizardValue - newValue) <= MAX_RANGE
                && (newValue - wizard.getRadius() - MIN_CLOSEST_RANGE) >= 0
                && (newValue + wizard.getRadius() + MIN_CLOSEST_RANGE) <= limit;
    }

}
