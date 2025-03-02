import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jakewharton.mosaic.LocalTerminal
import com.jakewharton.mosaic.layout.background
import com.jakewharton.mosaic.layout.fillMaxWidth
import com.jakewharton.mosaic.layout.height
import com.jakewharton.mosaic.layout.onKeyEvent
import com.jakewharton.mosaic.layout.width
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.ui.Box
import com.jakewharton.mosaic.ui.Color
import com.jakewharton.mosaic.ui.Column
import com.jakewharton.mosaic.ui.Row
import com.jakewharton.mosaic.ui.Text
import com.jakewharton.mosaic.ui.TextStyle

@Composable
fun App(viewModel: ViewModel) {
	val terminal = LocalTerminal.current
	val selectedLine by viewModel.selectedLine.collectAsState()
	val content by viewModel.content.collectAsState()
	// subtraction of one is necessary, because there is a line with a cursor at the bottom, which moves up all the content
	val height = terminal.size.height - 1
	var offset by remember { mutableIntStateOf(0) }
	LaunchedEffect(selectedLine.first) {
		if (selectedLine.first < offset) {
			offset -= 1
		}
	}
	LaunchedEffect(selectedLine.last) {
		if (selectedLine.last > offset + height - 1) {
			offset += 1
		}
	}

	Box(
		Modifier
			.width(terminal.size.width)
			.height(height)
			.onKeyEvent { event -> viewModel.onKeyEvent(event) }
	) {
		Column {
			content.forEachIndexed { i, line ->
				if (i < offset) return@forEachIndexed
				if (i > offset + height - 1) return@forEachIndexed
				val selected = i in selectedLine
				Row(
					modifier = Modifier
						.fillMaxWidth()
						.background(if (selected) Color.Black else Color.Unspecified)
				) {
					Text(if (selected) "‣ " else "  ")
					if (line.getOrNull(0) == '#') {
						Text(line, color = Gray)
					} else {
						val match = LineRegexp.matchEntire(line)
						if (match == null) {
							Text(line)
						} else {
							val (word, hash, rest) = match.destructured
							Text(word, color = Color.Yellow, textStyle = TextStyle.Bold)
							Text(" ")
							Text(hash, color = Blue)
							Text(" ")
							Text(rest, color = LightGray)
						}
					}
				}
			}
		}
	}
}

private val LineRegexp = """^(pick|reword|edit|squash|fixup|exec|drop)\s+([a-z0-9]{7,40})\s+(.*)$""".toRegex()

private val Blue = Color(59, 120, 255)
private val Gray = Color(128, 128, 128)
private val LightGray = Color(192, 192, 192)
