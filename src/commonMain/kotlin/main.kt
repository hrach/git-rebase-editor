import androidx.compose.runtime.LaunchedEffect
import com.jakewharton.mosaic.runMosaicBlocking
import com.jakewharton.mosaic.ui.Column
import com.jakewharton.mosaic.ui.Text
import kotlinx.coroutines.awaitCancellation

fun main() = runMosaicBlocking {
	Column {
		Text("Git Rebase Editor")
	}
	LaunchedEffect(Unit) {
		awaitCancellation()
	}
}
