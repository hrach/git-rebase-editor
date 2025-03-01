import com.jakewharton.mosaic.layout.KeyEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlinx.io.writeString

class ViewModel(
	private val path: Path,
) {
	sealed interface Event {
		data class Exit(val code: Int = 0) : Event
	}

	val events = MutableSharedFlow<Event>(extraBufferCapacity = 1)
	val content: MutableStateFlow<List<String>> = MutableStateFlow(emptyList())
	val selectedLine: MutableStateFlow<IntRange> = MutableStateFlow(-1..-1)

	private var dirUp: Boolean = true
	private val validLineRegexp = """^(pick|reword|edit|sqaush|fixup|drop).*""".toRegex()

	init {
		this.content.value = SystemFileSystem.source(path).buffered().use {
			it.readString().lines()
		}
		this.selectedLine.value = 0..0
	}

	fun onKeyEvent(event: KeyEvent): Boolean {
		when (event) {
			KeyEvent("ArrowUp") -> moveSelectionUp()
			KeyEvent("ArrowDown") -> moveSelectionDown()
			KeyEvent("ArrowUp", shift = true) -> extendSelectionUp()
			KeyEvent("ArrowDown", shift = true) -> extendSelectionDown()
			KeyEvent("ArrowLeft") -> moveUp()
			KeyEvent("ArrowRight") -> moveDown()
			KeyEvent("p") -> changeMode("pick")
			KeyEvent("r") -> changeMode("reword")
			KeyEvent("e") -> changeMode("edit")
			KeyEvent("s") -> changeMode("squash")
			KeyEvent("f") -> changeMode("fixup")
			KeyEvent("d") -> changeMode("drop")
			KeyEvent("Enter") -> saveAndExit()
			KeyEvent("Escape"), KeyEvent("q") -> events.tryEmit(Event.Exit(1))
			else -> return false
		}
		return true
	}

	private fun moveSelectionUp() {
		selectedLine.update {
			val newLine = (it.first - 1).coerceAtLeast(0)
			newLine..newLine
		}
	}

	private fun moveSelectionDown() {
		selectedLine.update {
			val newLine = (it.last + 1).coerceAtMost(content.value.size - 1)
			newLine..newLine
		}
	}

	private fun extendSelectionUp() {
		val selected = selectedLine.value
		if (selected.first == selected.last) dirUp = true
		selectedLine.update {
			val first = if (dirUp) (selected.first - 1).coerceAtLeast(0) else selected.first
			val last = if (dirUp) selected.last else (selected.last - 1).coerceAtLeast(selected.first)
			first..last
		}
	}

	private fun extendSelectionDown() {
		val selected = selectedLine.value
		if (selected.first == selected.last) dirUp = false
		selectedLine.update {
			val first = if (dirUp) (selected.first + 1).coerceAtMost(selected.last) else selected.first
			val last = if (dirUp) selected.last else (selected.last + 1).coerceAtMost(content.value.size - 1)
			first..last
		}
	}

	@Suppress("ReplaceRangeToWithRangeUntil")
	private fun moveUp() {
		val selected = selectedLine.value
		if (selected.first - 1 < 0) return
		content.update {
			val lines = it.toMutableList()
			val moving = lines.removeAt(selected.first - 1)
			lines.add(selected.last, moving)
			lines
		}
		selectedLine.update {
			it.first - 1..it.last - 1
		}
	}

	private fun moveDown() {
		val selected = selectedLine.value
		if (content.value.size < selected.last + 1) return
		content.update {
			val lines = it.toMutableList()
			val moving = lines.removeAt(selected.last + 1)
			lines.add(selected.first, moving)
			lines
		}
		selectedLine.update {
			it.first + 1..it.last + 1
		}
	}

	private fun changeMode(newMode: String) {
		content.update {
			val lines = it.toMutableList()
			selectedLine.value.forEach { i ->
				if (lines[i].matches(validLineRegexp)) {
					lines[i] = newMode + " " + lines[i].substringAfter(" ")
				}
			}
			lines
		}
	}

	private fun saveAndExit() {
		val toSave = content.value.joinToString("\n")
		SystemFileSystem.sink(path).buffered().use {
			it.writeString(toSave)
		}
		events.tryEmit(Event.Exit(0))
	}
}
