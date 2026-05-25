package tp4.uge.snake

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class SensorHandler(
    context: Context,
    private val onDirection: (Direction) -> Unit,
    private val tiltThreshold: Float = 3f
) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer =
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    fun register() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun unregister() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val dir = when {
            y < -tiltThreshold && Math.abs(y) > Math.abs(x) -> Direction.UP
            y >  tiltThreshold && Math.abs(y) > Math.abs(x) -> Direction.DOWN
            x >  tiltThreshold && x > Math.abs(y)           -> Direction.LEFT
            x < -tiltThreshold && Math.abs(x) > Math.abs(y) -> Direction.RIGHT
            else -> return
        }
        onDirection(dir)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}