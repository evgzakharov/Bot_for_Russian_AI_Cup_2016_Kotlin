class WayParams(
        val startMapLine: MapLine,
        val mapLine: MapLine,
        val startPoint: Point2D,
        val startDestination: Double?,
        val pointLinePositions: List<LinePosition>,
        val isSafeWay: Boolean,
        val checkedLines: List<MapLine>)
