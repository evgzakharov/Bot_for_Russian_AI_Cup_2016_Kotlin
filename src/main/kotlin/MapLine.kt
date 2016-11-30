import com.sun.istack.internal.Nullable
import model.LaneType

import java.util.ArrayList
import java.util.HashMap

data class MapLine @JvmOverloads constructor(val startPoint: Point2D, val endPoint: Point2D, @Nullable val laneType: LaneType? = null, val isEnemy: Boolean? = null) {
    var enemy: Boolean = false

    val lineLength: Double
    val angle: Double

    var friendPosition: Double? = null
    var enemyPosition: Double? = null

    val friendWizardPositions: MutableMap<Point2D, Double> = HashMap()
    val enemyWizardPositions: MutableMap<Point2D, Double> = HashMap()

    var deadFriendTowerCount: Int = 0
    var deadEnemyTowerCount: Int = 0


    var startLines: MutableList<MapLine> = ArrayList()
    var endLines: MutableList<MapLine> = ArrayList()

    init {

        this.lineLength = startPoint.getDistanceTo(endPoint)
        this.angle = StrictMath.asin((endPoint.y - startPoint.y) / lineLength)

        if (isEnemy != null) {
            this.enemy = isEnemy
            if (isEnemy) {
                deadEnemyTowerCount = 2
            } else {
                deadFriendTowerCount = 0
            }
        }

    }
}
