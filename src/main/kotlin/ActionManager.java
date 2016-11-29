import model.*;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.StrictMath.PI;
import static java.lang.StrictMath.abs;

abstract class ActionManager {

    protected Wizard self;
    protected World world;
    protected Game game;
    protected Move move;
    protected FindHelper findHelper;
    protected ShootHelper shootHelder;
    protected MoveHelper moveHelper;
    protected MapWayFinder mapWayFinder;
    protected StrategyManager strategyManager;

    protected static final double LOW_HP_FACTOR = 0.25D;
    protected static final double LOW_BUIDING_FACTOR = 0.1D;
    protected static final double LOW_MINION_FACTOR = 0.35D;

    protected static final double MIN_CLOSEST_DISTANCE = 20D;

    public void init(Wizard self, World world, Game game, Move move, StrategyManager strategyManager) {
        this.self = self;
        this.world = world;
        this.game = game;
        this.move = move;
        this.findHelper = new FindHelper(world, game, self);
        this.shootHelder = new ShootHelper(self, game, move);
        this.moveHelper = new MoveHelper(self, world, game, move);
        this.mapWayFinder = new MapWayFinder(world, game, self);
        this.strategyManager = strategyManager;
    }

    public ActionMode move() {
        Optional<Tree> nearestTree = findHelper.getAllTrees().stream()
                .filter(tree -> abs(self.getAngleTo(tree)) < PI / 2)
                .filter(tree -> self.getDistanceTo(tree) < self.getRadius() + tree.getRadius() + MIN_CLOSEST_DISTANCE)
                .findAny();

        nearestTree.ifPresent(tree -> {
            shootHelder.shootToTarget(tree);
        });

        return ActionMode.ATTACK;
    }

    abstract public ActionMode getMode();

    protected boolean buldingCondition() {
        Optional<Building> nearestBuilding = findHelper.getAllBuldings(true).stream()
                .min(Comparator.comparingDouble(self::getDistanceTo));

        boolean buldingCondition = false;
        if (nearestBuilding.isPresent()) {
            double demageRadius = 0;
            if (nearestBuilding.get().getType() == BuildingType.FACTION_BASE)
                demageRadius = game.getFactionBaseAttackRange();
            if (nearestBuilding.get().getType() == BuildingType.GUARDIAN_TOWER)
                demageRadius = game.getGuardianTowerAttackRange() + MIN_CLOSEST_DISTANCE;

            double distanceToBuilding = self.getDistanceTo(nearestBuilding.get());
            if (distanceToBuilding < demageRadius) {

                Optional<LivingUnit> nearestFriendToBuilding = findHelper.getAllMovingUnits(true, true).stream()
                        .filter(unit -> unit.getLife() / unit.getMaxLife() < self.getLife() / self.getMaxLife())
                        .min(Comparator.comparingDouble(nearestBuilding.get()::getDistanceTo));

                boolean noFriends = nearestFriendToBuilding
                        .map(livingUnit -> distanceToBuilding < livingUnit.getDistanceTo(nearestBuilding.get()))
                        .orElse(true);

                boolean buldingIsToClose = (demageRadius - distanceToBuilding) >= game.getWizardRadius() * 4;

                boolean hgIsLow = self.getLife() < (1 - LOW_HP_FACTOR) * self.getMaxLife();

                boolean buldingWillShoot = nearestBuilding.get().getRemainingActionCooldownTicks() < 100;

                if ((noFriends && hgIsLow && buldingWillShoot) || buldingIsToClose)
                    buldingCondition = true;
            }
        }
        return buldingCondition;
    }

    protected boolean singleEnemyCondition(List<Wizard> enemyWizards) {
        Optional<Wizard> enemyWithSmallestHP = enemyWizards.stream()
                .filter(unit -> self.getDistanceTo(unit) < game.getWizardCastRange())
                .min(Comparator.comparingInt(Wizard::getLife));

        boolean singleEnemyCondition = false;
        if (enemyWithSmallestHP.isPresent()) {
            boolean enemyIsToClose = enemyWithSmallestHP.get().getDistanceTo(self) <= game.getWizardCastRange() * 0.8;

            boolean hpIsToLow = self.getLife() < (LOW_HP_FACTOR * 2) * self.getMaxLife()
                    && self.getLife() * (1 - LOW_HP_FACTOR / 2) < enemyWithSmallestHP.get().getLife()
                    && enemyWithSmallestHP.get().getAngleTo(self) <= game.getStaffSector() * 2;

            if (enemyIsToClose || hpIsToLow)
                singleEnemyCondition = true;
        }
        return singleEnemyCondition;
    }

    protected boolean multiEnemiesCondition(List<Wizard> enemyWizards) {
        List<Wizard> enemiesLookingToMe = enemyWizards.stream()
                .filter(unit -> {
                    double distanceTo = self.getDistanceTo(unit);
                    return (distanceTo < game.getWizardCastRange() * 1.1 && abs(unit.getAngleTo(self)) <= game.getStaffSector() * 1.2);
                })
                .collect(Collectors.toList());

        boolean multiEnemiesCondition = false;
        if (enemiesLookingToMe.size() > 1) {
            Wizard enemyWithBiggestHP = enemiesLookingToMe.stream()
                    .max(Comparator.comparingInt(Wizard::getLife)).get();

            boolean hpIsLow = self.getLife() < self.getMaxLife() * (LOW_HP_FACTOR * 3)
                    && self.getLife() * (1 - LOW_HP_FACTOR / 2) < enemyWithBiggestHP.getLife();

            if (hpIsLow)
                multiEnemiesCondition = true;
        }
        return multiEnemiesCondition;
    }

    protected boolean minionConditions() {
        long toCloseMinions = findHelper.getAllMinions(true, true).stream()
                .filter(minion -> {
                    if (minion.getType() == MinionType.FETISH_BLOWDART)
                        return self.getDistanceTo(minion) <= game.getFetishBlowdartAttackRange() * 1.1;
                    else if (minion.getType() == MinionType.ORC_WOODCUTTER)
                        return self.getDistanceTo(minion) <= game.getOrcWoodcutterAttackRange() * 3;

                    return false;
                }).count();

        return toCloseMinions > 0;
    }
}
