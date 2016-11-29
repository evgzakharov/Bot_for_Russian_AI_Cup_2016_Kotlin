import model.Game;
import model.Move;
import model.Wizard;
import model.World;

public final class MyStrategy implements Strategy {

    private StrategyManager strategyManager;

    @Override
    public void move(Wizard self, World world, Game game, Move move) {
        initialize();
        strategyManager.nextTick(self, world, game, move);
    }

    private void initialize() {
        FindHelper.clearCache();

        if (strategyManager == null) {
            strategyManager = new StrategyManager();
        }
    }

}