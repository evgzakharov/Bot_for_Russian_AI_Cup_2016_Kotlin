import model.ActionType;
import model.LivingUnit;
import model.Tree;
import model.Wizard;

import java.util.List;
import java.util.Optional;


public class AttackActionManager extends ActionManager {
    @Override
    public ActionMode move() {
        if (self.getLife() < self.getMaxLife() * LOW_HP_FACTOR) {
            moveHelper.goTo(mapWayFinder.getPreviousWaypoint(strategyManager.getLaneType()));
            return ActionMode.ATTACK;
        }

        Optional<LivingUnit> nearestTarget = findHelper.getNearestEnemy();

        Point2D nextWaypoint = null;
        if (isNeedToMoveBack()) {
            moveHelper.goWithoutTurn(mapWayFinder.getPreviousWaypoint(strategyManager.getLaneType()));

            nearestTarget.ifPresent(livingUnit -> shootHelder.shootToTarget(livingUnit));

            return ActionMode.ATTACK;
        } else {
            nextWaypoint = mapWayFinder.getNextWaypoint(strategyManager.getLaneType());
            moveHelper.goWithoutTurn(nextWaypoint);

            if (nearestTarget.isPresent()) {
                shootHelder.shootToTarget(nearestTarget.get());
                return ActionMode.ATTACK;
            }
        }

        moveHelper.goTo(nextWaypoint);

        Optional<Tree> nearestTree = findHelper.getAllTrees().stream()
                .filter(tree -> self.getAngleTo(tree) < game.getStaffSector())
                .filter(tree -> self.getDistanceTo(tree) < self.getRadius() + tree.getRadius() + MIN_CLOSEST_DISTANCE)
                .findAny();

        nearestTree.ifPresent(tree -> move.setAction(ActionType.STAFF));

        super.move();

        return ActionMode.ATTACK;
    }

    private boolean isNeedToMoveBack() {
        boolean minionsCondition = minionConditions();
        if (minionsCondition) return true;

        List<Wizard> enemyWizards = findHelper.getAllWizards(true, true);

        boolean multiEnemiesCondition = multiEnemiesCondition(enemyWizards);
        if (multiEnemiesCondition) return true;

        boolean singleEnemyCondition = singleEnemyCondition(enemyWizards);
        if (singleEnemyCondition) return true;

        boolean buldingCondition = buldingCondition();
        if (buldingCondition) return true;

        return false;
    }

    @Override
    public ActionMode getMode() {
        return ActionMode.ATTACK;
    }
}
