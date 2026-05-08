package eu.mcomputing.mobv.diplomovapraca.utils

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.EditText
import eu.mcomputing.mobv.diplomovapraca.data.model.KeystrokeEvent
import java.time.Instant
import java.io.File
import java.lang.System

class EditTextLogger(
    private val context: Context,
    private val userId: String,
    private var roundId: Int,
    private val logFileName: String
) : TextWatcher {

    private val TAG = "KeystrokeLogger"

    private var timestampBeforeNs: Long = 0L
    private var contentLengthBefore: Int = 0

    private var charChange: Char? = null
    private var isDeleting: Boolean = false

    private val keystrokesFile: File by lazy {
        FileUtils.getLogFile(context, logFileName).apply {
            if (this.length() == 0L) {
                this.appendText("UserId,RoundId,TimestampAfterNs,TimestampBeforeNs,ContentLengthBefore,ActionType,KeyChar,CursorPosition,InputContent\n")
            }
        }
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        timestampBeforeNs = getNanosecondsTimestamp()
        contentLengthBefore = s?.length ?: 0

        if (count > 0 && after == 0) {
            isDeleting = true
            charChange = s?.get(start)
        } else {
            isDeleting = false
            charChange = null
        }
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        if (count > 0 && before == 0) {
            charChange = s?.get(start)
        }
    }

    override fun afterTextChanged(s: Editable?) {
        val timestampAfterNs: Long = getNanosecondsTimestamp()

        val finalContent = s.toString()
        val finalCursorPosition = finalContent.length
        val currentRoundId = this.roundId

        val event = when {
            charChange != null && (isDeleting || !isDeleting) -> {
                KeystrokeEvent(
                    userId = userId,
                    roundId = currentRoundId,
                    timestampUs = timestampAfterNs,
                    actionType = if (isDeleting) "Delete" else "Insert",
                    keyChar = charChange!!,
                    cursorPosition = finalCursorPosition,
                    inputContent = finalContent
                )
            }
            else -> return
        }

        val csvLine = "${event.userId}," +
                "${event.roundId}," +
                "${event.timestampUs}," +
                "$timestampBeforeNs," +
                "$contentLengthBefore," +
                "${event.actionType}," +
                "${event.keyChar}," +
                "${event.cursorPosition}," +
                "\"${event.inputContent.replace("\"", "\"\"")}\""

        try {
            keystrokesFile.appendText("$csvLine\n")
        } catch (e: Exception) {
            Log.e(TAG, "Error writing keystroke event to file $logFileName", e)
        }
    }

    private fun getNanosecondsTimestamp(): Long {
        val instant = Instant.now()
        val seconds = instant.epochSecond
        val nanosOfSecond = instant.nano
        return seconds * 1_000_000_000L + nanosOfSecond
    }

    fun setRoundId(newRoundId: Int) {
        this.roundId = newRoundId
    }

    fun attachTo(editText: EditText) {
        editText.addTextChangedListener(this)
    }

    fun detachFrom(editText: EditText) {
        editText.removeTextChangedListener(this)
    }
}