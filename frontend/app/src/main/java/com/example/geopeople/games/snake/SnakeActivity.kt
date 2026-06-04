package tp4.uge.snake

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels

class SnakeActivity : ComponentActivity() {

    private val viewModel: SnakeViewModel by viewModels()
    private lateinit var sensorHandler: SensorHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sensorHandler = SensorHandler(this, onDirection = viewModel::pushDirection)
        setContent { SnakeGame(viewModel) }
    }

    override fun onResume()  { super.onResume();  sensorHandler.register()   }
    override fun onPause()   { super.onPause();   sensorHandler.unregister() }
}