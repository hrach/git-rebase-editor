import androidx.compose.runtime.LaunchedEffect
import com.jakewharton.mosaic.runMosaicBlocking
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.io.files.Path
import kotlin.system.exitProcess

fun main(args: Array<String>) {
	val file = args.firstOrNull() ?: run {
		println("Missing an input file")
		exitProcess(1)
	}

	val viewModel = ViewModel(Path(file))

	try {
		runMosaicBlocking {
			App(viewModel)

			LaunchedEffect(viewModel) {
				viewModel.events.filterIsInstance<ViewModel.Event.Exit>().first()
				throw ExitException()
			}
		}
	} catch (_: ExitException) {
		// no-op
	}
}

class ExitException : RuntimeException()
