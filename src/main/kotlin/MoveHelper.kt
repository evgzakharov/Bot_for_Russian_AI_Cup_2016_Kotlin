import model.*

import java.lang.StrictMath.*

class MoveHelper(private val self: Wizard, private val world: World, private val game: Game, private val move: Move) {

    private var lastFindingPoint: Point2D? = null
    private var lastFoundPoint: Point2D? = null

    fun goTo(unit: LivingUnit) {
        goTo(unit.toPoint())
    }

    fun goTo(point: Point2D) {
        val correctedPoint = correctPoint(point)

        val angle = self.getAngleTo(correctedPoint.x, correctedPoint.y)
        move.turn = angle

        goWithoutTurn(point)
    }

    fun goWithoutTurn(point: Point2D) {
        val correctedPoint = correctPoint(point)

        val diffAngle = self.getAngleTo(correctedPoint.x, correctedPoint.y)

        val backCoef = cos(diffAngle)
        val strickCoef = sin(diffAngle)

        if (abs(diffAngle) > PI / 2) {
            move.speed = game.wizardBackwardSpeed * backCoef
        } else {
            move.speed = game.wizardForwardSpeed * backCoef
        }
        move.strafeSpeed = game.wizardStrafeSpeed * strickCoef
    }

    fun correctPoint(point2D: Point2D): Point2D {
        if (point2D == lastFindingPoint) {
            if (lastFoundPoint != null)
                return lastFoundPoint!!
            else
                return point2D
        }

        lastFindingPoint = point2D

        val wayFinder = WayFinder(self, world, game)
        val way = wayFinder.findWay(point2D)

        if (way != null && way.size > 0) {
            val findPoint = way[0]
            lastFoundPoint = findPoint

            return findPoint
        }

        return point2D
    }


}
