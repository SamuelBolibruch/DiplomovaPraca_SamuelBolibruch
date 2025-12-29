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
    private val logFileName: String // NOVÝ PARAMETER PRE DYNAMICKÝ NÁZOV SÚBORU
) : TextWatcher {

    private val TAG = "KeystrokeLogger"

    // NOVÉ SUROVÉ PREMENNÉ: Ukladajú sa dáta pred zmenou (beforeTextChanged)
    private var timestampBeforeNs: Long = 0L
    private var contentLengthBefore: Int = 0

    private var charChange: Char? = null
    private var isDeleting: Boolean = false

    // Uložíme referenciu na súbor, s opravenou hlavičkou CSV
    private val keystrokesFile: File by lazy {
        // ZMENA: Použitie logFileName a volanie FileUtils.getLogFile
        FileUtils.getLogFile(context, logFileName).apply {
            if (this.length() == 0L) {
                // OPRAVENÁ HLAVIČKA CSV - pridanie TimestampBeforeNs a ContentLengthBefore
                this.appendText("UserId,RoundId,TimestampAfterNs,TimestampBeforeNs,ContentLengthBefore,ActionType,KeyChar,CursorPosition,InputContent\n")
            }
        }
    }


    /**
     * Zaznamená počiatočný stav PRED zmenou textu (TimestampBeforeNs a ContentLengthBefore).
     */
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        // --- ZAZNAMENANIE SUROVÝCH DÁT PRED ZMENOU ---
        timestampBeforeNs = getNanosecondsTimestamp() // Čas začiatku spracovania
        contentLengthBefore = s?.length ?: 0          // Dĺžka obsahu PRED zmenou
        // ----------------------------------------------

        if (count > 0 && after == 0) { // Akcia: Delete/Backspace
            isDeleting = true
            charChange = s?.get(start)
        } else {
            isDeleting = false
            charChange = null
        }
    }

    /**
     * Sleduje zmenu textu.
     */
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        if (count > 0 && before == 0) { // Akcia: Insert
            charChange = s?.get(start)
        }
    }

    /**
     * Finalizuje záznam PO vykonaní zmeny textu a zapisuje ho do CSV súboru.
     */
    override fun afterTextChanged(s: Editable?) {
        // --- ZAZNAMENANIE SUROVÉHO ČASU PO ZMENE ---
        val timestampAfterNs: Long = getNanosecondsTimestamp()
        // ------------------------------------------

        // Úprava: Odstránime .replace(",", "[COMMA]"), namiesto toho použijeme CSV escape
        val finalContent = s.toString()
        val finalCursorPosition = finalContent.length
        val currentRoundId = this.roundId

        val event = when {
            charChange != null && (isDeleting || !isDeleting) -> { // Ošetrenie DELETE a INSERT
                KeystrokeEvent(
                    userId = userId,
                    roundId = currentRoundId,
                    timestampUs = timestampAfterNs, // Toto je teraz TimestampAfterNs
                    actionType = if (isDeleting) "Delete" else "Insert",
                    keyChar = charChange!!,
                    cursorPosition = finalCursorPosition,
                    inputContent = finalContent
                )
            }
            else -> return // Ignorovať nečisté zmeny
        }

        // --- ZÁPIS DO CSV SÚBORU ---

        // Vytvorenie CSV riadku s NOVÝMI stĺpcami
        val csvLine = "${event.userId}," +
                "${event.roundId}," +
                "${event.timestampUs}," +            // TimestampAfterNs
                "$timestampBeforeNs," +              // NOVÝ: TimestampBeforeNs
                "$contentLengthBefore," +            // NOVÝ: ContentLengthBefore
                "${event.actionType}," +
                "${event.keyChar}," +
                "${event.cursorPosition}," +
                "\"${event.inputContent.replace("\"", "\"\"")}\"" // Ošetrenie úvodzoviek a ich escape

        try {
            keystrokesFile.appendText("$csvLine\n")

            // 🔥 NOVÝ RIADOK PRE LOG.D (upravený o názov súboru)
            val pdNs = timestampAfterNs - timestampBeforeNs
            Log.d(TAG,
                "CSV Logged [${logFileName}]: ${event.actionType} | PD: $pdNs ns | After: ${event.timestampUs}"
            )

        } catch (e: Exception) {
            Log.e(TAG, "CHYBA PRI ZÁPISE KEWSTROKE EVENTU do súboru $logFileName!", e)
        }
    }

    /**
     * Samostatná funkcia na generovanie 19-miestneho timestampu (nanosekúnd od epochy).
     */
    private fun getNanosecondsTimestamp(): Long {
        val instant = Instant.now()
        val seconds = instant.epochSecond
        val nanosOfSecond = instant.nano
        return seconds * 1_000_000_000L + nanosOfSecond
    }

    // ... (Zvyšné metódy zostávajú nezmenené) ...

    /**
     * Umožňuje nastaviť roundId dynamicky.
     */
    fun setRoundId(newRoundId: Int) {
        this.roundId = newRoundId
    }

    /**
     * Pripojí logger k cieľovému EditText.
     */
    fun attachTo(editText: EditText) {
        editText.addTextChangedListener(this)
    }

    /**
     * Odpojí logger (dôležité pre správu pamäte).
     */
    fun detachFrom(editText: EditText) {
        editText.removeTextChangedListener(this)
    }
}