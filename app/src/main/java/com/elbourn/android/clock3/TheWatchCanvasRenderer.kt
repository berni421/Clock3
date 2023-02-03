package com.elbourn.android.clock3


import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.text.TextPaint
import android.util.Log
import android.view.SurfaceHolder
import androidx.core.content.res.ResourcesCompat
import androidx.wear.remote.interactions.RemoteActivityHelper
import androidx.wear.watchface.*
import androidx.wear.watchface.style.CurrentUserStyleRepository
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors


// Default for how long each frame is displayed at expected frame rate.
private const val FRAME_PERIOD_MS_DEFAULT: Long = 1000L // a second is fine

/**
 * Renders watch face via data in Room database. Also, updates watch face state based on setting
 * changes by user via [userStyleRepository.addUserStyleListener()].
 */
class TheWatchCanvasRenderer(
    private val context: Context,
    surfaceHolder: SurfaceHolder,
    watchState: WatchState,
    private val complicationSlotsManagerTODO: ComplicationSlotsManager,
    currentUserStyleRepositoryTODO: CurrentUserStyleRepository,
    canvasType: Int
) : Renderer.CanvasRenderer2<TheWatchCanvasRenderer.TheSharedAssets>(
    surfaceHolder,
    currentUserStyleRepositoryTODO,
    watchState,
    canvasType,
    FRAME_PERIOD_MS_DEFAULT,
    clearWithBackgroundTintBeforeRenderingHighlightLayer = false
),
    WatchFace.TapListener {

    val TAG: String = javaClass.simpleName
    val APP = BuildConfig.APPLICATION_ID

    override fun onTapEvent(tapType: Int, tapEvent: TapEvent, complicationSlot: ComplicationSlot?) {
        /*
        * The watch face receives three different types of touch events:
        * - [TapType.DOWN] when the user puts the finger down on the touchscreen
        * - [TapType.UP] when the user lifts the finger from the touchscreen
        * - [TapType.CANCEL] when the system detects that the user is performing a gesture other
        *   than a tap
        */

        when (tapType) {
            TapType.UP -> processTAP()
            TapType.CANCEL -> {}
            TapType.DOWN -> {}
        }
        invalidate()
    }

    var touchCount = 0
    var touchTimeElapsed = 0L

    fun processTAP() {
        Log.i(TAG, "start processTAP")
        if (!disclaimerOK) {
            messageHandler.removeMessage()
            context.getSharedPreferences(APP, MODE_PRIVATE)
                .edit()
                .putBoolean("disclaimerCheckBox", true)
                .apply()
            handleTerms()
            return
        }
        val now = System.currentTimeMillis()
        touchCount++
        if (touchCount == 1) {
            showBatteryLevel()
            touchTimeElapsed = now + 1000L
            return
        }
        if (touchCount == 2 && touchTimeElapsed < now) {
            showBatteryLevel()
            touchTimeElapsed = now + 1000L
            touchCount = 1
            return
        }
        touchTimeElapsed = 0
        touchCount = 0
        // Feed the cat
        val intent = Intent(Intent.ACTION_VIEW)
            .addCategory(Intent.CATEGORY_BROWSABLE)
            .setData(Uri.parse("https://www.elbourn.com/feed-the-cat/"))
        RemoteActivityHelper(context, Executors.newSingleThreadExecutor())
            .startRemoteActivity(intent, null)
        messageHandler.setupNewMessage(messageHandler.messageDisplayTime)
        messageHandler.addMessage("Check your phone for\nthe 'Feed the cat' website")
        messageHandler.addFontSize(messageHandler.messageTextSize)
    }

    fun showBatteryLevel() {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            context.registerReceiver(null, ifilter)
        }
        val batteryPct: Float? = batteryStatus?.let { intent ->
            val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            level * 100 / scale.toFloat()
        }
        messageHandler.setupNewMessage(messageHandler.messageDisplayTime)
        messageHandler.addMessage("Battery Level\n")
        messageHandler.addMessage("$batteryPct%")
        messageHandler.addMessage("\nTAP again to 'feed the cat'")
        messageHandler.addFontSize(messageHandler.messageTextSize)
        messageHandler.addFontSize(messageHandler.bigMessageTextSize)
        messageHandler.addFontSize(messageHandler.messageTextSize)
    }

    var disclaimerOK = false
    var sharedPreferencesTesting = false
    fun handleTerms() {
        if (sharedPreferencesTesting) {
            Log.i(TAG, "SharedPreference testing")
            context.getSharedPreferences(APP, MODE_PRIVATE)
                .edit()
                .putBoolean("disclaimerCheckBox", false)
                .apply()
            sharedPreferencesTesting = false
        }
        disclaimerOK = context.getSharedPreferences(APP, MODE_PRIVATE)
            .getBoolean("disclaimerCheckBox", false)
        Log.i(TAG, "disclaimerOK: " + disclaimerOK)
        if (disclaimerOK) {
            messageHandler.setupNewMessage(messageHandler.messageDisplayTime)
            messageHandler.addMessage("Checking status...")
            messageHandler.addFontSize(messageHandler.messageTextSize)
            Handler(Looper.getMainLooper()).postDelayed({
                messageHandler.setupNewMessage(messageHandler.messageDisplayTime * 2)
                messageHandler.addMessage("Terms accepted-thank you!\n")
                messageHandler.addMessage(messageHandler.tapScreenText)
                messageHandler.addFontSize(messageHandler.messageTextSize)
                messageHandler.addFontSize(messageHandler.messageTextSize)
            }, messageHandler.messageDisplayTime)
        } else {
            messageHandler.setupNewMessage(messageHandler.messageDisplayForever)
            messageHandler.addMessage(context.getString(R.string.disclaimer_text))
            messageHandler.addFontSize(messageHandler.smallMessageTextSize)
        }
    }

    class TheSharedAssets : SharedAssets {
        override fun onDestroy() {
//            TODO
        }
    }

    override suspend fun createSharedAssets(): TheSharedAssets {
        return TheSharedAssets()
    }

    override fun renderHighlightLayer(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
        sharedAssets: TheSharedAssets
    ) {
//        TODO
    }

    fun doWatchPreview(canvas: Canvas) {
        val d: Drawable? = ResourcesCompat.getDrawable(
            context.resources,
            R.drawable.watch_preview,
            null
        )
        d!!.setBounds(0, 0, canvas.width, canvas.height)
        d.draw(canvas)
    }

    val ambientModeTesting = false
    val ambientModeTime = 60
    val ambientModeLag = 56
    var ambientModeTimer = 0
    lateinit var messageHandler: Messages
    var watchPreviewNeeded = 5 * (1000 / FRAME_PERIOD_MS_DEFAULT)
    override fun render(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
        sharedAssets: TheSharedAssets
    ) {
        // Current Time
        val currentTime = zonedDateTime

        // Handle ambient mode
//        Log.i(TAG, "ambientModeTimer: $ambientModeTimer" )
        val ambientMode = renderParameters.drawMode == DrawMode.AMBIENT || ambientModeTesting
        if (ambientMode && ambientModeTimer > 0) {
            ambientModeTimer--
            if (ambientModeTimer < ambientModeLag) {
//                Log.i(TAG, "ambient mode")
                return
            }
        }

        // Blank the canvas
        canvas.drawColor(Color.BLACK)

        // Show preview for few seconds
        if (watchPreviewNeeded > 0) {
            watchPreviewNeeded--
            doWatchPreview(canvas)
            return
        }

        // Messages
        if (!this::messageHandler.isInitialized) {
            messageHandler = Messages(canvas)
        }
        if (messageHandler.isMessage()) {
            messageHandler.display()
            return
        }

        // Terms
        if (!disclaimerOK) {
            handleTerms()
            return
        }

        // Normal text
        val text = TextPaint(Paint())
        text.textSize = 96f
        text.color = Color.WHITE

        // Clock face main time indicator
        lateinit var formatter: DateTimeFormatter
        if (ambientMode) {
            formatter = DateTimeFormatter.ofPattern("HH:mm")
            text.color = Color.GRAY
            if (ambientModeTimer <= 0) {
                ambientModeTimer = ambientModeTime
            }
        } else {
            formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
        }
        val time = currentTime.format(formatter)
        val textBounds = Rect()
        text.getTextBounds(time, 0, time.length, textBounds)
        canvas.drawText(
            time,
            canvas.width / 2 - textBounds.width() / 2f,
            canvas.height / 2f + textBounds.height() / 2f,
            text
        )

        if (ambientMode) {
//            Log.i(TAG, "Ambient mode update complete")
            return
        }

        // Other time indicators
        val infoLines = 10
        text.textSize = 48f
        formatter = DateTimeFormatter.ofPattern("EEEE")
        val dayOfWeek = currentTime.format(formatter)
        text.getTextBounds(dayOfWeek, 0, dayOfWeek.length, textBounds)
        canvas.drawText(
            dayOfWeek,
            canvas.width / 2 - textBounds.width() / 2f,
            canvas.height * 1 / infoLines + textBounds.height() / 2f,
            text
        )

        formatter = DateTimeFormatter.ofPattern("k")
        val timeOfDay = textTimeName(currentTime.format(formatter).toInt())
        text.getTextBounds(timeOfDay, 0, timeOfDay.length, textBounds)
        canvas.drawText(
            timeOfDay,
            canvas.width / 2 - textBounds.width() / 2f,
            canvas.height * 2 / infoLines + textBounds.height() / 2f,
            text
        )

        formatter = DateTimeFormatter.ofPattern("d LLLL")
        val month = currentTime.format(formatter)
        text.getTextBounds(month, 0, month.length, textBounds)
        canvas.drawText(
            month,
            canvas.width / 2 - textBounds.width() / 2f,
            canvas.height * (infoLines - 2) / infoLines + textBounds.height() / 2f,
            text
        )

        formatter = DateTimeFormatter.ofPattern("yyyy")
        val year = currentTime.format(formatter)
        text.getTextBounds(year, 0, year.length, textBounds)
        canvas.drawText(
            year,
            canvas.width / 2 - textBounds.width() / 2f,
            canvas.height * (infoLines - 1) / infoLines + textBounds.height() / 2f,
            text
        )

    }

    fun textTimeName(hour: Int): String {
        var t = "error"
        if (hour <= 4) t = "early"
        if (hour in (5..11))  t = "morning"
        if (hour in (12..18)) t = "afternoon"
        if (hour in (19..21)) t = "evening"
        if (hour > 21) t = "late"
        return t
    }
}
