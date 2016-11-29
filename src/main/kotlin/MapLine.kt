import com.sun.istack.internal.Nullable
import model.LaneType

import java.util.ArrayList
import java.util.HashMap

class MapLine @JvmOverloads constructor(val startPoint: Point2D, val endPoint: Point2D, @Nullable val laneType: LaneType? = null, isEnemy: Boolean? = null) {
    var enemy: Boolean? = false

    val lineLength: Double
    val angle: Double

    var friendPosition = -1.0
    var enemyPosition = -1.0

    val friendWizardPositions: MutableMap<Point2D, Double> = HashMap()
    val enemyWizardPositions: MutableMap<Point2D, Double> = HashMap()

    var deadFriendTowerCount: Int = 0
    var deadEnemyTowerCount: Int = 0

    var mapLineStatus: MapLineStatus? = null

    var startLines: MutableList<MapLine> = ArrayList()
    var endLines: MutableList<MapLine> = ArrayList()

    init {

        this.lineLength = startPoint.getDistanceTo(endPoint)
        this.angle = StrictMath.asin((endPoint.y - startPoint.y) / lineLength)

        if (isEnemy != null) {
            this.enemy = isEnemy
            if (isEnemy) {
                deadEnemyTowerCount = 2
                mapLineStatus = MapLineStatus.RED
            } else {
                deadFriendTowerCount = 0
                mapLineStatus = MapLineStatus.GREEN
            }
        }

    }
}
