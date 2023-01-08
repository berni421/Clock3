package com.elbourn.android.clock3

import android.util.Log
import android.view.SurfaceHolder
import androidx.wear.watchface.CanvasType
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.WatchFace
import androidx.wear.watchface.WatchFaceService
import androidx.wear.watchface.WatchFaceType
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.style.CurrentUserStyleRepository

class TheWatchFaceService : WatchFaceService() {

    val TAG: String = javaClass.simpleName

    override suspend fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository
    ): WatchFace {
        Log.i(TAG, "started createWatchFace")

        // Creates class that renders the watch face.
        val renderer = TheWatchCanvasRenderer(
            context = applicationContext,
            surfaceHolder = surfaceHolder,
            watchState = watchState,
            complicationSlotsManagerTODO = complicationSlotsManager,
            currentUserStyleRepositoryTODO = currentUserStyleRepository,
            canvasType = CanvasType.HARDWARE
        )

        // Creates the watch face.
        return WatchFace(
            watchFaceType = WatchFaceType.ANALOG,
            renderer = renderer
        ).setTapListener(renderer)
    }
}
