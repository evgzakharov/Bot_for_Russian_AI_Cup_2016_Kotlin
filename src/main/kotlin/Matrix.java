import java.util.HashMap;
import java.util.Map;

public class Matrix {

    private double pathCount = 0;

    private Point2D point;
    private MatrixPoint matrixPoint;
    private Matrix previousMatrix;
    private Map<MatrixPoint, Matrix> matrixPoints;

    public Matrix(Point2D point, MatrixPoint matrixPoint, Matrix previousMatrix) {
        this.point = point;
        this.matrixPoint = matrixPoint;
        this.previousMatrix = previousMatrix;

        if (previousMatrix != null)
            this.matrixPoints = previousMatrix.getMatrixPoints();
        else
            this.matrixPoints = new HashMap<>();
    }

    public MatrixPoint getMatrixPoint() {
        return matrixPoint;
    }

    public double getPathCount() {
        return pathCount;
    }

    public void setPathCount(double pathCount) {
        this.pathCount = pathCount;
    }

    public Point2D getPoint() {
        return point;
    }

    public Matrix getPreviousMatrix() {
        return previousMatrix;
    }

    public Map<MatrixPoint, Matrix> getMatrixPoints() {
        return matrixPoints;
    }
}
