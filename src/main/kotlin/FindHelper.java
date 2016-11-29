import model.*;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.lang.StrictMath.abs;

public class FindHelper {

    private Wizard wizard;
    private World world;
    private Game game;

    private static Map<List<Boolean>, List<LivingUnit>> allUnitsCache = new HashMap<>();
    private static Map<List<Boolean>, List<LivingUnit>> allMovingUnitsCache = new HashMap<>();
    private static Map<List<Boolean>, List<Wizard>> allWizardsCache = new HashMap<>();
    private static Map<Boolean, List<Building>> allBuldings = new HashMap<>();
    private static Map<List<Boolean>, List<Minion>> allMinions = new HashMap<>();
    private static List<Tree> allTrees = null;

    public static void clearCache() {
        allUnitsCache = new HashMap<>();
        allMovingUnitsCache = new HashMap<>();
        allWizardsCache = new HashMap<>();
        allBuldings = new HashMap<>();
        allMinions = new HashMap<>();
        allTrees = null;
    }

    public FindHelper(World world, Game game, Wizard wizard) {
        this.world = world;
        this.game = game;
        this.wizard = wizard;
    }

    public List<LivingUnit> getAllUnits(boolean withTrees, boolean onlyEnemy, boolean onlyNearest) {
        List<Boolean> cacheKey = Arrays.asList(withTrees, onlyEnemy, onlyNearest);
        if (allUnitsCache.get(cacheKey) != null) return allUnitsCache.get(cacheKey);

        List<LivingUnit> units = new ArrayList<>();

        units.addAll(getAllWizards(onlyEnemy, onlyNearest));
        units.addAll(getAllBuldings(onlyEnemy));
        units.addAll(getAllMinions(onlyEnemy, onlyNearest));

        if (withTrees)
            units.addAll(Arrays.asList(world.getTrees()));

        allUnitsCache.put(cacheKey, units);

        return units;
    }

    public List<LivingUnit> getAllMovingUnits(boolean onlyEnemy, boolean onlyNearest) {
        List<Boolean> cacheKey = Arrays.asList(onlyEnemy, onlyNearest);
        if (allMovingUnitsCache.get(cacheKey) != null) return allMovingUnitsCache.get(cacheKey);

        List<LivingUnit> units = new ArrayList<>();

        units.addAll(getAllWizards(onlyEnemy, onlyNearest));
        units.addAll(getAllMinions(onlyEnemy, onlyNearest));

        allMovingUnitsCache.put(cacheKey, units);

        return units;
    }

    public List<Wizard> getAllWizards(boolean onlyEnemy, boolean onlyNearest) {
        List<Boolean> cacheKey = Arrays.asList(onlyEnemy, onlyNearest);

        if (allWizardsCache.get(cacheKey) != null) return allWizardsCache.get(cacheKey);

        List<Wizard> newUnins = Arrays.stream(world.getWizards())
                .filter(wizard -> !wizard.isMe())
                .filter(filterLivingUnits(onlyEnemy, onlyNearest))
                .collect(Collectors.toList());

        allWizardsCache.put(cacheKey, newUnins);

        return newUnins;
    }

    private Predicate<LivingUnit> filterLivingUnits(boolean onlyEnemy, boolean onlyNearest) {
        return unit -> (!onlyEnemy || isEnemy(wizard.getFaction(), unit))
                && (!onlyNearest || abs(unit.getX() - wizard.getX()) < game.getWizardCastRange() * 3)
                && (!onlyNearest || abs(unit.getY() - wizard.getY()) < game.getWizardCastRange() * 3);

    }

    public List<Building> getAllBuldings(boolean onlyEnemy) {
        Boolean cacheKey = onlyEnemy;

        if (allBuldings.get(cacheKey) != null) return allBuldings.get(cacheKey);

        List<Building> newUnits = Arrays.stream(world.getBuildings())
                .filter(filterLivingUnits(onlyEnemy, false))
                .collect(Collectors.toList());

        allBuldings.put(cacheKey, newUnits);

        return newUnits;
    }

    public List<Minion> getAllMinions(boolean onlyEnemy, boolean onlyNearest) {
        List<Boolean> cacheKey = Arrays.asList(onlyEnemy, onlyNearest);

        if (allMinions.get(cacheKey) != null) return allMinions.get(cacheKey);

        List<Minion> newUnits = Arrays.stream(world.getMinions())
                .filter(filterLivingUnits(onlyEnemy, onlyNearest))
                .collect(Collectors.toList());

        allMinions.put(cacheKey, newUnits);

        return newUnits;
    }

    public List<Tree> getAllTrees() {
        if (allTrees != null) return allTrees;

        List<Tree> newUnits = Arrays.stream(world.getTrees())
                .filter(filterLivingUnits(false, true))
                .collect(Collectors.toList());

        allTrees = newUnits;

        return allTrees;
    }

    public boolean isEnemy(Faction self, LivingUnit unit) {
        return self != unit.getFaction() && unit.getFaction() != Faction.NEUTRAL;
    }

    public Optional<LivingUnit> getNearestEnemy() {
        Optional<LivingUnit> nearestWizard = getNearestTarget(world.getWizards());

        if (nearestWizard.isPresent()) return nearestWizard;

        Optional<LivingUnit> nearestBuilding = getNearestTarget(world.getBuildings());

        if (nearestBuilding.isPresent()) return nearestBuilding;

        else return getNearestTarget(world.getMinions());
    }

    public Optional<LivingUnit> getNearestTarget(LivingUnit[] targets) {
        List<LivingUnit> nearestTargets = new ArrayList<>();

        for (LivingUnit target : targets) {
            if (!isEnemy(wizard.getFaction(), target)) {
                continue;
            }

            if (abs(wizard.getX() - target.getX()) > game.getWizardCastRange() * 2) continue;
            if (abs(wizard.getY() - target.getY()) > game.getWizardCastRange() * 2) continue;

            double distance = wizard.getDistanceTo(target);

            if (distance < wizard.getCastRange()) {
                nearestTargets.add(target);
            }
        }

        return nearestTargets
                .stream()
                .min(Comparator.comparingInt(LivingUnit::getLife));
    }
}
