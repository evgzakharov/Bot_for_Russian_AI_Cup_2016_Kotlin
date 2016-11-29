import com.sun.istack.internal.Nullable;
import model.LaneType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapLine {
    private Point2D startPoint;
    private Point2D endPoint;
    private LaneType laneType;
    private Boolean isEnemy = false;

    private double lineLength;
    private double angle;

    private double friendPosition = -1;
    private double enemyPosition = -1;

    private Map<Point2D, Double> friendWizardPositions = new HashMap<>();
    private Map<Point2D, Double> enemyWizardPositions = new HashMap<>();

    private int deadFriendTowerCount;
    private int deadEnemyTowerCount;

    private MapLineStatus mapLineStatus;

    private List<MapLine> startLines = new ArrayList<>();
    private List<MapLine> endLines = new ArrayList<>();

    public MapLine(Point2D startPoint, Point2D endPoint, @Nullable LaneType laneType) {
        this(startPoint, endPoint, laneType, null);
    }

    public MapLine(Point2D startPoint, Point2D endPoint, @Nullable LaneType laneType, Boolean isEnemy) {
        this.startPoint = startPoint;
        this.endPoint = endPoint;
        this.laneType = laneType;

        this.lineLength = startPoint.getDistanceTo(endPoint);
        this.angle = StrictMath.asin((endPoint.getY() - startPoint.getY()) / lineLength);

        if (isEnemy == null) return;

        this.isEnemy = isEnemy;
        if (isEnemy) {
            deadEnemyTowerCount = 2;
            mapLineStatus = MapLineStatus.RED;
        } else {
            deadFriendTowerCount = 0;
            mapLineStatus = MapLineStatus.GREEN;
        }
    }

    public Point2D getStartPoint() {
        return startPoint;
    }

    public Point2D getEndPoint() {
        return endPoint;
    }

    public double getAngle() {
        return angle;
    }

    public Boolean getEnemy() {
        return isEnemy;
    }

    public double getLineLength() {
        return lineLength;
    }

    public LaneType getLaneType() {
        return laneType;
    }

    public double getFriendPosition() {
        return friendPosition;
    }

    public void setFriendPosition(double friendPosition) {
        this.friendPosition = friendPosition;
    }

    public double getEnemyPosition() {
        return enemyPosition;
    }

    public void setEnemyPosition(double enemyPosition) {
        this.enemyPosition = enemyPosition;
    }

    public int getDeadFriendTowerCount() {
        return deadFriendTowerCount;
    }

    public void setDeadFriendTowerCount(int deadFriendTowerCount) {
        this.deadFriendTowerCount = deadFriendTowerCount;
    }

    public int getDeadEnemyTowerCount() {
        return deadEnemyTowerCount;
    }

    public void setDeadEnemyTowerCount(int deadEnemyTowerCount) {
        this.deadEnemyTowerCount = deadEnemyTowerCount;
    }

    public MapLineStatus getMapLineStatus() {
        return mapLineStatus;
    }

    public void setMapLineStatus(MapLineStatus mapLineStatus) {
        this.mapLineStatus = mapLineStatus;
    }

    public Map<Point2D, Double> getFriendWizardPositions() {
        return friendWizardPositions;
    }

    public Map<Point2D, Double> getEnemyWizardPositions() {
        return enemyWizardPositions;
    }

    public List<MapLine> getStartLines() {
        return startLines;
    }

    public void setStartLines(List<MapLine> startLines) {
        this.startLines = startLines;
    }

    public List<MapLine> getEndLines() {
        return endLines;
    }

    public void setEndLines(List<MapLine> endLines) {
        this.endLines = endLines;
    }
}
