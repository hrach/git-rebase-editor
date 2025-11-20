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
		data object Exit : Event
	}

	val events = MutableSharedFlow<Event>(extraBufferCapacity = 1)
	val content: MutableStateFlow<List<String>> = MutableStateFlow(emptyList())
	val selectedLine: MutableStateFlow<IntRange> = MutableStateFlow(-1..-1)

	private var dirUp: Boolean = true
	private val commitLineRegexp = """^(pick|reword|edit|squash|fixup|drop).*""".toRegex()
	private val updateRefLineRegexp = """^(#\s+)?update-ref.*""".toRegex()

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
			KeyEvent("p") -> changeMode("pick", "update-ref")
			KeyEvent("r") -> changeMode("reword")
			KeyEvent("e") -> changeMode("edit")
			KeyEvent("s") -> changeMode("squash")
			KeyEvent("f") -> changeMode("fixup")
			KeyEvent("d") -> changeMode("drop", "# update-ref")
			KeyEvent("Enter") -> saveAndExit()
			KeyEvent("Escape"), KeyEvent("q") -> events.tryEmit(Event.Exit)
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

	private fun changeMode(newMode: String, updateRefNewMode: String? = null) {
		content.update {
			val lines = it.toMutableList()
			selectedLine.value.forEach { i ->
				if (lines[i].matches(commitLineRegexp)) {
					lines[i] = newMode + " " + lines[i].substringAfter(" ")
				} else if (updateRefNewMode != null && lines[i].matches(updateRefLineRegexp)) {
					lines[i] = updateRefNewMode + " " + lines[i].removePrefix("# ").substringAfter(" ")
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
		events.tryEmit(Event.Exit)
	}
}
