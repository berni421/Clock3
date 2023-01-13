package com.elbourn.android.clock3

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.Layout
import android.text.StaticLayout

import android.text.TextPaint
import android.util.Log


class Messages (val canvas: Canvas){
    val TAG:String = javaClass.simpleName
    var messageActiveUntil: Long = 0
    val messageDisplayTime: Long = 2000
    lateinit var message: ArrayList<String>
    lateinit var messageFontSize: ArrayList<Float>
    val messageTextSize = 24f
    val smallMessageTextSize = 14f
    val bigMessageTextSize = 96f
    lateinit var messagePaint: Paint
    val tapScreenText = """
        TAP screen:

        once - battery level
        twice - 'feed the cat'
        """.trimIndent()
    val messageDisplayForever = -1L
    var messageActiveForever = false

    init {
        setupNewMessage(-99L)
    }

    fun setupNewMessage(delay:Long) {
        message = ArrayList()
        messageFontSize = ArrayList()
        messagePaint = Paint()
        messagePaint.color = Color.LTGRAY
        if (delay == messageDisplayForever) {
            messageActiveForever = true
        } else {
            messageActiveForever = false
            messageActiveUntil = System.currentTimeMillis() + delay
        }
    }

    fun isMessage():Boolean {
        val now = System.currentTimeMillis()
        return now < messageActiveUntil || messageActiveForever
    }

    fun addMessage(m:String) {
        message.add(m)
    }

    fun addFontSize(fs:Float) {
        messageFontSize.add(fs)
    }

    fun display() {
        canvas.save()
        var totalY = 0f
        val messageLayout: ArrayList<Layout> = ArrayList()
        for (i in message.indices) {
            val m = message[i]
            if (messageFontSize.size - 1 < i) {
                Log.e(TAG, "messageFontSize too small")
                break
            }
            val mfs = messageFontSize[i]
//            Log.i(TAG, "mfs:$mfs")
            val textPaint = TextPaint(messagePaint)
            textPaint.textSize = mfs
            val indents = intArrayOf(30)
            // Construct text layout
            val builder = StaticLayout.Builder.obtain(
                m,
                0,
                m.length,
                textPaint,
                canvas.width
            )
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setLineSpacing(1f, 1f)
                .setIncludePad(false)
                .setIndents(indents, indents)
            val layout = builder.build()
            totalY += layout.height.toFloat()
            messageLayout.add(layout)
//            Log.i(TAG, "TotalY: $totalY")
        }
        canvas.translate(0f, canvas.height / 2f - totalY / 2f)
        for (i in 0 until messageLayout.size) {
            val ml: Layout = messageLayout[i]
            ml.draw(canvas)
            canvas.translate(0f, ml.height.toFloat())
        }
        canvas.restore()
//        Log.i(TAG, "end displayText")
    }

    fun removeMessage() {
        setupNewMessage(0)
    }
}
