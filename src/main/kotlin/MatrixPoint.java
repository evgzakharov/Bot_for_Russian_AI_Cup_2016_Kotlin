public class MatrixPoint {
    private final short diffX;
    private final short diffY;

    public MatrixPoint() {
        this.diffX = 0;
        this.diffY = 0;
    }

    public MatrixPoint(short diffX, short diffY) {
        this.diffX = diffX;
        this.diffY = diffY;
    }

    public short getDiffX() {
        return diffX;
    }

    public short getDiffY() {
        return diffY;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MatrixPoint that = (MatrixPoint) o;

        if (diffX != that.diffX) return false;
        return diffY == that.diffY;
    }

    @Override
    public int hashCode() {
        int result = (int) diffX;
        result = 31 * result + (int) diffY;
        return result;
    }
}
