import model.Game
import model.Move
import model.Wizard
import model.World

//v15
class MyStrategy : Strategy {

    private var strategyManager: StrategyManager? = null

    override fun move(self: Wizard, world: World, game: Game, move: Move) {
        initialize()
        strategyManager!!.nextTick(self, world, game, move)
    }

    private fun initialize() {
        FindHelper.clearCache()

        if (strategyManager == null) {
            strategyManager = StrategyManager()
        }
    }

}