import java.util.List;

public class WayParams {

    private MapLine mapLine;
    private MapLine startMapLine;
    private Point2D startPoint;
    private Double startDestination;
    private List<LinePosition> pointLinePositions;
    private boolean safeWay;

    private List<MapLine> checkedLines;

    public WayParams(MapLine startMapLine, MapLine mapLine, Point2D startPoint, Double startDestination, List<LinePosition> pointLinePositions, boolean safeWay, List<MapLine> checkLines) {
        this.startMapLine = startMapLine;
        this.mapLine = mapLine;
        this.startPoint = startPoint;
        this.startDestination = startDestination;
        this.pointLinePositions = pointLinePositions;
        this.safeWay = safeWay;
        this.checkedLines = checkLines;
    }

    public MapLine getStartMapLine() {
        return startMapLine;
    }

    public MapLine getMapLine() {
        return mapLine;
    }

    public Point2D getStartPoint() {
        return startPoint;
    }

    public Double getStartDestination() {
        return startDestination;
    }

    public List<LinePosition> getPointLinePositions() {
        return pointLinePositions;
    }

    public boolean isSafeWay() {
        return safeWay;
    }

    public List<MapLine> getCheckedLines() {
        return checkedLines;
    }
}
