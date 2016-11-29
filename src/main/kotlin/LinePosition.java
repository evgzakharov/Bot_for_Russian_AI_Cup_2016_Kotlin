public class LinePosition {
    private MapLine mapLine;
    private double position;

    public LinePosition(MapLine mapLine, double position) {
        this.mapLine = mapLine;
        this.position = position;
    }

    public MapLine getMapLine() {
        return mapLine;
    }

    public double getPosition() {
        return position;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LinePosition that = (LinePosition) o;

        if (Double.compare(that.position, position) != 0) return false;
        return mapLine != null ? mapLine.equals(that.mapLine) : that.mapLine == null;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = mapLine != null ? mapLine.hashCode() : 0;
        temp = Double.doubleToLongBits(position);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }
}
