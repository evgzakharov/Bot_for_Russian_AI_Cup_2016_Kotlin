import model.Bonus
import model.LivingUnit
import model.Unit

class Point2D(val x: Double, val y: Double)

fun Point2D.getDistanceTo(x: Double, y: Double): Double {
    return StrictMath.hypot(this.x - x, this.y - y)
}

fun Point2D.getDistanceTo(point: Point2D): Double {
    return getDistanceTo(point.x, point.y)
}

fun Point2D.getDistanceTo(unit: Unit): Double {
    return getDistanceTo(unit.x, unit.y)
}

fun LivingUnit.getDistanceTo(point: Point2D): Double {
    return point.getDistanceTo(this)
}

fun LivingUnit.toPoint(): Point2D = Point2D(this.x, this.y)

fun Bonus.toPoint(): Point2D = Point2D(this.x, this.y)
