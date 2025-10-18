package com.maciej.barshow

import android.content.Context
import android.media.SoundPool
import java.util.concurrent.atomic.AtomicBoolean

class SoundPlayer(context: Context) {

    private val soundPool: SoundPool = SoundPool.Builder().setMaxStreams(2).build()
    private val soundsLoaded = AtomicBoolean(false)
    private var addPointSoundId: Int = 0
    private var removePointSoundId: Int = 0

    init {
        soundPool.setOnLoadCompleteListener { _, _, status ->
            if (status == 0) {
                soundsLoaded.set(true)
            }
        }
        addPointSoundId = soundPool.load(context, R.raw.add_point, 1)
        removePointSoundId = soundPool.load(context, R.raw.remove_point, 1)
    }

    fun playAddPointSound() {
        if (soundsLoaded.get()) {
            soundPool.play(addPointSoundId, 1f, 1f, 1, 0, 1f)
        }
    }

    fun playRemovePointSound() {
        if (soundsLoaded.get()) {
            soundPool.play(removePointSoundId, 1f, 1f, 1, 0, 1f)
        }
    }

    fun release() {
        soundPool.release()
    }
}
