package com.leo.livestreamingdemo.activity;

import android.hardware.Camera;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import com.leo.livestreamingdemo.R;
import com.qiniu.android.dns.DnsManager;
import com.qiniu.android.dns.IResolver;
import com.qiniu.android.dns.NetworkInfo;
import com.qiniu.android.dns.http.DnspodFree;
import com.qiniu.android.dns.local.AndroidDnsServer;
import com.qiniu.android.dns.local.Resolver;
import com.qiniu.pili.droid.streaming.AVCodecType;
import com.qiniu.pili.droid.streaming.CameraStreamingSetting;
import com.qiniu.pili.droid.streaming.MediaStreamingManager;
import com.qiniu.pili.droid.streaming.MicrophoneStreamingSetting;
import com.qiniu.pili.droid.streaming.StreamingProfile;
import com.qiniu.pili.droid.streaming.StreamingState;
import com.qiniu.pili.droid.streaming.StreamingStateChangedListener;
import com.qiniu.pili.droid.streaming.widget.AspectFrameLayout;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URISyntaxException;

import butterknife.BindView;
import butterknife.ButterKnife;


public class MyStreamActivity extends AppCompatActivity implements
        StreamingStateChangedListener {


    private static DnsManager dnsManager;
    @BindView(R.id.cameraPreview_afl)
    AspectFrameLayout mCameraPreviewAfl;

    @BindView(R.id.content)
    RelativeLayout mContent;

    @BindView(R.id.glSurfaceView)
    GLSurfaceView mGlSurfaceView;


    private StreamingProfile streamingProfile;
    private MediaStreamingManager streamingManager;
    private MicrophoneStreamingSetting mMicrophoneStreamingSetting;
    private static final String TAG = "leo";

    String publishurl =
            "你的推流地址";

    private CameraStreamingSetting mCameraStreamingSetting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_my_stream);
        ButterKnife.bind(this);
        initStream();


    }


    private void initStream() {
        mCameraPreviewAfl.setShowMode(AspectFrameLayout.SHOW_MODE.FULL);
//        mCameraPreviewSurfaceView.setListener(this);
        streamingProfile = new StreamingProfile();


        try {
            streamingProfile
                    /*-----------------通过setVideoQuality设置推流视频部分质量参数------------

                    Level   Fps Video Bitrate(Kbps)
                    VIDEO_QUALITY_LOW1 12 150
                    VIDEO_QUALITY_LOW2  15  264
                    VIDEO_QUALITY_LOW3  15  350
                    VIDEO_QUALITY_MEDIUM1 30 512
                    VIDEO_QUALITY_MEDIUM2   30  800
                    VIDEO_QUALITY_MEDIUM3   30  1000
                    VIDEO_QUALITY_HIGH1 30  1200
                    VIDEO_QUALITY_HIGH2 30  1500
                    VIDEO_QUALITY_HIGH3 30  2000
                    */
                    .setVideoQuality(StreamingProfile.VIDEO_QUALITY_MEDIUM2)
                    /*-----------------通过setAudioQuality设置推流音频部分质量参数------------

                    Level   Audio Bitrate(Kbps) Audio Sample Rate(Hz)
                    AUDIO_QUALITY_LOW1  18  44100
                    AUDIO_QUALITY_LOW2  24  44100
                    AUDIO_QUALITY_MEDIUM1   32  44100
                    AUDIO_QUALITY_MEDIUM2   48  44100
                    AUDIO_QUALITY_HIGH1 96  44100
                    AUDIO_QUALITY_HIGH2 128 44100
                    */
                    .setAudioQuality(StreamingProfile.AUDIO_QUALITY_MEDIUM2)
                    //自定义设置推流分辨率，此优先级高于setEncodingSizeLevel
//                  .setPreferredVideoEncodingSize(960, 544)
                    /*---------------设置推流的分辨率-------------------------------------------

                            Level              Resolution(16:9) Resolution(4:3)
                    VIDEO_ENCODING_HEIGHT_240   424 x 240   320 x 240
                    VIDEO_ENCODING_HEIGHT_480   848 x 480   640 x 480
                    VIDEO_ENCODING_HEIGHT_544   960 x 544   720 x 544
                    VIDEO_ENCODING_HEIGHT_720   1280 x 720  960 x 720
                    VIDEO_ENCODING_HEIGHT_1088  1920 x 1088 1440 x 1088
                    */
                    .setEncodingSizeLevel(StreamingProfile.VIDEO_ENCODING_HEIGHT_480)
                    /*--------------通过setEncoderRCMode设置质量优先还是码率优先----------------
                        EncoderRCModes.QUALITY_PRIORITY: 质量优先，实际的码率可能高于设置的码率
                        EncoderRCModes.BITRATE_PRIORITY: 码率优先，更精确地码率控制
                        默认值为 EncoderRCModes.QUALITY_PRIORITY*/
                    .setEncoderRCMode(StreamingProfile.EncoderRCModes.BITRATE_PRIORITY)
                    /*--------------通过AVProfile参数自定义推流音视频码率帧率--------------------
                       自定义设置音频采样率为44100Hz, 码率为96 * 1024 bps
                       自定义设置视频帧率为30, 码率为1000 * 1024 bps,最大帧率为48
                        setAVProfile 的优先级高于 Quality，也就是说，当同时调用了 Quality 和 AVProfile 的设置，AVProfile 会覆盖 Quality 的设置值
                    .setAVProfile(avProfile)*/
                    .setDnsManager(getMyDnsManager())
                    .setAdaptiveBitrateEnable(true)
                    .setFpsControllerEnable(true)
                    .setStreamStatusConfig(new StreamingProfile.StreamStatusConfig(3))

                    .setPublishUrl(publishurl)
                    /*-------------------设置推流播放端方向-------------------------------
                    设置ENCODING_ORIENTATION.LAND推流播放端会横屏播放
                    设置ENCODING_ORIENTATION.PORT推流播放端会竖屏播放
                    .setEncodingOrientation(StreamingProfile.ENCODING_ORIENTATION.PORT)*/
                    .setSendingBufferProfile(new StreamingProfile.SendingBufferProfile(0.2f, 0.8f, 3.0f, 20 * 1000));


            mCameraStreamingSetting = new CameraStreamingSetting();
            mCameraStreamingSetting
                    .setCameraId(Camera.CameraInfo.CAMERA_FACING_FRONT)
                    .setContinuousFocusModeEnabled(true)
                    .setCameraPrvSizeLevel(CameraStreamingSetting.PREVIEW_SIZE_LEVEL.LARGE)
                    .setCameraPrvSizeRatio(CameraStreamingSetting.PREVIEW_SIZE_RATIO.RATIO_16_9);

            /*       setting.setFaceBeautySetting(new CameraStreamingSetting.FaceBeautySetting(1.0f, 1.0f, 0.8f))
                            .setVideoFilter(CameraStreamingSetting.VIDEO_FILTER_TYPE.VIDEO_FILTER_BEAUTY);
                    setting.setCameraSourceImproved(true);*/


            streamingManager = new MediaStreamingManager(this, mCameraPreviewAfl, mGlSurfaceView,
                    /* hw codec ----- soft codec */
                    AVCodecType.HW_VIDEO_SURFACE_AS_INPUT_WITH_HW_AUDIO_CODEC);
/*            streamingManager.setVideoFilterType(CameraStreamingSetting.VIDEO_FILTER_TYPE.VIDEO_FILTER_BEAUTY);*/

            /*------------------------麦克风参数配置-----------------------------*/
            mMicrophoneStreamingSetting = new MicrophoneStreamingSetting();
            /*希望增加蓝牙麦克风的支持,可以设置:*/
            mMicrophoneStreamingSetting.setBluetoothSCOEnabled(false);

            streamingManager.prepare(mCameraStreamingSetting, mMicrophoneStreamingSetting, streamingProfile);

            streamingManager.setStreamingStateListener(this);


        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        streamingManager.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // You must invoke pause here.
        streamingManager.pause();
    }


    private static DnsManager getMyDnsManager() {
        IResolver r0 = new DnspodFree();
        IResolver r1 = AndroidDnsServer.defaultResolver();
        IResolver r2 = null;
        try {
            r2 = new Resolver(InetAddress.getByName("119.29.29.29"));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        dnsManager = new DnsManager(NetworkInfo.normal, new IResolver[]{r0, r1, r2});
        return dnsManager;
    }


    @Override
    public void onStateChanged(StreamingState streamingState, Object o) {
        Log.e(TAG, "StreamingState streamingState:" + streamingState + ",extra:" + o);

        switch (streamingState) {
            case PREPARING:
                Log.e(TAG, "onStateChanged: "  + "准备" );
                break;
            case READY:

                // start streaming when READY
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (streamingManager != null) {
                            boolean b = streamingManager.startStreaming();
                            Log.e(TAG, "run: " + "推流" + b);
                        }
                    }
                }).start();
                break;
            case CONNECTING:
                Log.e(TAG, "onStateChanged: " + "已连接");
                break;
            case STREAMING:

                Log.e(TAG, "onStateChanged: " + "已发送");
                // The av packet had been sent.
                break;
            case SHUTDOWN:
                Log.e(TAG, "onStateChanged: " + "推流完成");
                // The streaming had been finished.
                break;
            case IOERROR:
                Log.e(TAG, "onStateChanged: " + "IO错误");
                // Network connect error.
                break;
            case SENDING_BUFFER_EMPTY:
                Log.e(TAG, "onStateChanged: " + "缓冲区数据为空");
                break;
            case SENDING_BUFFER_FULL:
                Log.e(TAG, "onStateChanged: " + "缓冲区数据存满");
                break;
            case AUDIO_RECORDING_FAIL:
                Log.e(TAG, "onStateChanged: " + "录音失败");
                // Failed to record audio.
                break;
            case OPEN_CAMERA_FAIL:
                Log.e(TAG, "onStateChanged: " + "打开相机失败");
                // Failed to open camera.
                break;
            case DISCONNECTED:
                Log.e(TAG, "onStateChanged: " + "断开连接");
                // The socket is broken while streaming
                break;
        }
    }


}
