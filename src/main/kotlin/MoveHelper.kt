import model.*

import java.lang.StrictMath.*

class MoveHelper(private val self: Wizard, private val world: World, private val game: Game, private val move: Move) {

    fun goTo(unit: LivingUnit, increaseDistanceToMinion: Boolean = false) {
        goTo(unit.toPoint(), increaseDistanceToMinion)
    }

    fun goTo(point: Point2D, increaseDistanceToMinion: Boolean = false) {
        val correctedPoint = correctPoint(point, increaseDistanceToMinion)

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

    fun correctPoint(point2D: Point2D, increaseDistanceToMinion: Boolean = false): Point2D {
        val wayFinder = WayFinder(self, world, game)
        val way = wayFinder.findWay(point2D, increaseDistanceToMinion)

        if (way != null) {
            val findPoint = way

            return findPoint
        }

        return point2D
    }


}
