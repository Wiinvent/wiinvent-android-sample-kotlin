package tv.wiinvent.android.winnvent_sdk_android_sample

import android.content.ComponentName
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
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
import tv.wiinvent.wiinventsdk.interfaces.UserActionListener
import tv.wiinvent.wiinventsdk.models.ConfigData
import tv.wiinvent.wiinventsdk.models.OverlayData
import tv.wiinvent.wiinventsdk.ui.OverlayView

class MainActivity : AppCompatActivity() {

    companion object {
        val TAG = MainActivity.javaClass.canonicalName
        val SAMPLE_ACCOUNT_ID = "87"
        val SAMPLE_CHANNEL_ID = "100"
        val SAMPLE_STREAM_ID = "2"
    }

    private var exoplayerView: PlayerView? = null
    private var exoplayer: SimpleExoPlayer? = null
    private var mediaSession: MediaSessionCompat? = null
    private var concatenatingMediaSource: ConcatenatingMediaSource? = null
    private var playbackStateBuilder: PlaybackStateCompat.Builder? = null
    private var overlayManager: OverlayManager? = null
    private var overlayView: OverlayView? = null

    var fullscreen = false
    var fullscreenButton: ImageView? = null;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        exoplayerView = findViewById(R.id.simple_exo_player_view)
        fullscreenButton = findViewById(R.id.exo_fullscreen_icon)
        overlayView = findViewById(R.id.wisdk_overlay_view)

        init(savedInstanceState)
    }

    private fun init(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            initializePlayer()
            initializeOverlays()
        }
    }

    private fun initializePlayer() {
        val CONTENT_URL = "https://wiinvent.tv/videos/donationdemo.mp4"

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

        val mediaSource = buildMediaSource(CONTENT_URL)
        exoplayer?.playWhenReady = true
        exoplayer?.prepare(mediaSource)

        fullscreenButton?.setOnClickListener(object: View.OnClickListener{
            override fun onClick(v: View?) {
                if(fullscreen) {
                    fullscreenButton!!.setImageDrawable(
                        ContextCompat.getDrawable(
                            this@MainActivity,
                            R.drawable.ic_fullscreen_open
                        )
                    )
                    window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                    if (supportActionBar != null) {
                        supportActionBar!!.show()
                    }
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

                    //player
                    val params =
                        exoplayerView?.getLayoutParams() as ConstraintLayout.LayoutParams
                    params.width = ViewGroup.LayoutParams.MATCH_PARENT
                    params.height = (250 * applicationContext.resources
                        .displayMetrics.density).toInt()
                    exoplayerView?.setLayoutParams(params)

                    //overlays

                    overlayView?.setLayoutParams(params)


                    fullscreen = false
                } else {
                    fullscreenButton?.setImageDrawable(
                        ContextCompat.getDrawable(
                            this@MainActivity,
                            R.drawable.ic_fullscreen_close
                        )
                    )

                    window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
                    if (supportActionBar != null) {
                        supportActionBar!!.hide()
                    }
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    val params =
                        exoplayerView?.getLayoutParams() as ConstraintLayout.LayoutParams
                    params.width = ViewGroup.LayoutParams.MATCH_PARENT
                    params.height = ViewGroup.LayoutParams.MATCH_PARENT
                    exoplayerView?.setLayoutParams(params)

                    overlayView?.setLayoutParams(params)
                    fullscreen = true
                }

            }
        })
    }

    private fun initializeOverlays() {
        val overlayData = OverlayData.Builder()
            .mappingType(OverlayData.MappingType.THIRDPARTY)
            .accountId(SAMPLE_ACCOUNT_ID)
            .channelId(SAMPLE_CHANNEL_ID)
            .streamId(SAMPLE_STREAM_ID)
            .thirdPartyToken("fa9o7tvuNUUyu8piClEA0+kmAuZg4SFeEsMKMfSeCfhN7eaInptFERC6YJxdepILNJs914fZ1MIMiwDCsUQrSw%3D%3D")
            .debug(true)
            .env(OverlayData.Environment.PRODUCTION)
            .deviceType(OverlayData.DeviceType.PHONE)
                .contentType(OverlayData.ContentType.LIVESTREAM)
            .build()

        overlayManager = OverlayManager(
            this,
            R.id.wisdk_overlay_view,
            overlayData
        )

        overlayManager?.addUserPlayerListener(object: UserActionListener{
            override fun onTokenExpire() {

                Log.d(TAG, "--------onTokenExpire----- ")
            }

            override fun onUserPurchase(userId: String, productId: String) {
                Log.d(TAG, "--------onUserPurchase: " + productId)

                overlayManager?.onUserPurchaseSuccess("41121610f8f6103481f37920bc4cb1f04cd63389", "VIETTELTV_PES")
            }

            override fun onVoted(
                userId: String,
                channelId: String,
                streamId: String?,
                entryId: String,
                entryName: String,
                eventName: String,
                packageName: String,
                numPredictSame: Int
            ) {

                Log.d(TAG, "--------onVoted")
            }

        })

        overlayManager?.addOverlayListener(object: DefaultOverlayEventListener {
            override fun onConfigReady(config: ConfigData) {


                this@MainActivity.runOnUiThread {
//                    for (source in config.getStreamSources()) {
//                        Log.d(TAG, "============onConfigReady: " + source?.url ?: "")
//                        val mediaSource = buildMediaSource(source?.url ?: "")
//                        exoplayer?.playWhenReady = true
//                        exoplayer?.prepare(mediaSource)
//
//                    }
                }
            }

            override fun onTimeout() {
                Log.d(TAG, "-------------------onTimeout")
            }

            override fun onLoadError() {
                Log.d(TAG, "-------------------onLoadError")
            }

            override fun onWebViewBrowserClose() {
                Log.d(TAG, "-------------------onWebViewBrowserClose")
            }

            override fun onWebViewBrowserContentVisible(isVisible: Boolean) {

            }

            override fun onWebViewBrowserOpen() {
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
