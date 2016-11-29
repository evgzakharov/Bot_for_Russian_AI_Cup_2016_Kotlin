import java.util.HashMap

data class Matrix(val point: Point2D, val matrixPoint: MatrixPoint, val previousMatrix: Matrix?) {

    var pathCount = 0.0
    var matrixPoints: MutableMap<MatrixPoint, Matrix>? = null
        private set

    init {

        if (previousMatrix != null)
            this.matrixPoints = previousMatrix.matrixPoints
        else
            this.matrixPoints = HashMap<MatrixPoint, Matrix>()
    }
}
