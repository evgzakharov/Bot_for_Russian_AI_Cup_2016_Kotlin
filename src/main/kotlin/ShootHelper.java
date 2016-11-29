import model.*;

import static java.lang.StrictMath.abs;

public class ShootHelper {

    private Wizard self;
    private Game game;
    private Move move;

    public ShootHelper(Wizard self, Game game, Move move) {
        this.self = self;
        this.game = game;
        this.move = move;
    }

    public void shootToTarget(LivingUnit nearestTarget) {
        double distance = self.getDistanceTo(nearestTarget);

        double angle = self.getAngleTo(nearestTarget);

        move.setTurn(angle);

        if (distance > self.getCastRange()) return;

        if (abs(angle) < game.getStaffSector() / 2.0D) {
            move.setAction(ActionType.MAGIC_MISSILE);
            move.setCastAngle(angle);
            move.setMinCastDistance(distance - nearestTarget.getRadius() + game.getMagicMissileRadius());
        }
    }

}
