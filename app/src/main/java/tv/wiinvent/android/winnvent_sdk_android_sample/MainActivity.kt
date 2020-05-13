package tv.wiinvent.android.winnvent_sdk_android_sample

import android.content.ComponentName
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import tv.wiinvent.wiinventsdk.OverlayManager
import tv.wiinvent.wiinventsdk.interfaces.DefaultOverlayEventListener
import tv.wiinvent.wiinventsdk.interfaces.PlayerChangeListener
import tv.wiinvent.wiinventsdk.models.ConfigData
import tv.wiinvent.wiinventsdk.models.OverlayData


class MainActivity : AppCompatActivity() {

    companion object {
        val TAG = MainActivity.javaClass.canonicalName
        val SAMPLE_ACCOUNT_ID = "1"
        val SAMPLE_CHANNEL_ID = "1"
        val SAMPLE_STREAM_ID = "1"
    }

    private var exoplayerView: PlayerView? = null
    private var exoplayer: SimpleExoPlayer? = null
    private var mediaSession: MediaSessionCompat? = null
    private var concatenatingMediaSource: ConcatenatingMediaSource? = null
    private var playbackStateBuilder: PlaybackStateCompat.Builder? = null
    private var overlayManager: OverlayManager? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        setContentView(R.layout.activity_main)

        exoplayerView = findViewById(R.id.simple_exo_player_view)

        init(savedInstanceState)
    }

    private fun init(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            initializePlayer()
            initializeOverlays()
        }
    }

    private fun initializePlayer() {
        concatenatingMediaSource = ConcatenatingMediaSource();
        val componentName = ComponentName(baseContext, "Exo")

        exoplayer = SimpleExoPlayer.Builder(baseContext).build()
        exoplayerView?.player = exoplayer
        exoplayerView?.useController = true

        playbackStateBuilder = PlaybackStateCompat.Builder()
        playbackStateBuilder?.setActions(
            PlaybackStateCompat.ACTION_PLAY or
                    PlaybackStateCompat.ACTION_PAUSE or
                    PlaybackStateCompat.ACTION_FAST_FORWARD)

        mediaSession = MediaSessionCompat(baseContext, "ExoPlayer", componentName, null)
        mediaSession?.setPlaybackState(playbackStateBuilder?.build())
        mediaSession?.isActive = true
    }

    private fun initializeOverlays() {
        val overlayData = OverlayData.Builder()
            .mappingType(OverlayData.MappingType.THIRDPARTY)
            .accountId(SAMPLE_ACCOUNT_ID)
            .channelId(SAMPLE_CHANNEL_ID)
            .streamId(SAMPLE_STREAM_ID)
            .debug(true)
            .env(OverlayData.Environment.DEV)
            .deviceType(OverlayData.DeviceType.PHONE)
            .build()

        overlayManager = OverlayManager(
            this,
            R.id.wisdk_overlay_view,
            overlayData
        )
        overlayManager?.addOverlayListener(object: DefaultOverlayEventListener {
            override fun onConfigReady(config: ConfigData) {


                this@MainActivity.runOnUiThread {
                    for (source in config.getStreamSources()) {
                        Log.d(TAG, "============onConfigReady: " + source?.url ?: "")
                        val mediaSource = buildMediaSource(source?.url ?: "")
                        exoplayer?.playWhenReady = true
                        exoplayer?.prepare(mediaSource)

                    }
                }
            }

            override fun onVoted(userId: String, channelId: String, streamId: String?, entryId: String, numPredictSame: Int) {
                TODO("Not yet implemented")
            }

            override fun onUserPurchase(userId: String, productId: Int) {
                TODO("Not yet implemented")
            }
        })

        // Set the player position for VOD playback.
        overlayManager?.addPlayerListener(object: PlayerChangeListener {
            override val currentPosition: Long?
                get() = exoplayer?.currentPosition
        })

        // Add player event listeners to determine overlay visibility.
        exoplayer?.addListener(object : Player.EventListener{
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {

                Log.d(TAG, "====onPlayerStateChanged playWhenReady: $playWhenReady - $playbackState")

                overlayManager?.setVisible(playWhenReady && playbackState == Player.STATE_READY)
            }

        })

    }

    private fun buildMediaSource(url: String) : MediaSource {
        val userAgent = Util.getUserAgent(baseContext, "Exo")
        val dataSourceFactory = DefaultDataSourceFactory(baseContext, userAgent)
        val uri = Uri.parse(url)

        return when (val type = Util.inferContentType(uri)) {
            C.TYPE_DASH -> DashMediaSource
                .Factory(dataSourceFactory)
                .createMediaSource(uri)
            C.TYPE_HLS -> HlsMediaSource
                .Factory(dataSourceFactory)
                .setAllowChunklessPreparation(true)
                .createMediaSource(uri)
            C.TYPE_SS -> SsMediaSource
                .Factory(dataSourceFactory)
                .createMediaSource(uri)
            C.TYPE_OTHER -> ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(uri)
            else -> throw IllegalStateException("Unsupported type :: $type")
        }

    }

    private fun playNextMediaSource() {
        // Play next media source by removing current from collection.
        exoplayer?.currentWindowIndex?.let {
            concatenatingMediaSource?.removeMediaSource(it)
        }
        concatenatingMediaSource?.let {
            exoplayer?.playWhenReady = true
          //  exoplayer?.prepare(concatenatingMediaSource, true, true)
        }
    }

    private fun releasePlayer() {
        if (exoplayer != null) {
            exoplayer?.stop()
            exoplayer?.release()
            exoplayer = null
        }
    }

    private fun releaseOverlays() {
        if (overlayManager != null) {
            overlayManager?.release()
            overlayManager = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
        releaseOverlays()
    }

    override fun onPause() {
        super.onPause()
        exoplayer?.playWhenReady = false
    }

}
