import androidx.compose.runtime.LaunchedEffect
import com.jakewharton.mosaic.runMosaicBlocking
import kotlinx.coroutines.awaitCancellation
import kotlinx.io.files.Path
import kotlin.system.exitProcess

fun main(args: Array<String>) {
	val file = args.firstOrNull() ?: run {
		println("Missing an input file")
		exitProcess(1)
	}

	val viewModel = ViewModel(Path(file))

	runMosaicBlocking {
		LaunchedEffect(viewModel) {
			viewModel.events.collect { event ->
				when (event) {
					is ViewModel.Event.Exit -> exitProcess(event.code)
				}
			}
		}

		App(viewModel)

		LaunchedEffect(Unit) {
			awaitCancellation()
		}
	}
}
