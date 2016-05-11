package org.dync.floatwindowdemo;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.Environment;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import java.io.IOException;
import java.lang.reflect.Field;

public class FloatWindowSmallView extends LinearLayout implements MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnInfoListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnSeekCompleteListener,
        MediaPlayer.OnVideoSizeChangedListener, SurfaceHolder.Callback,
        View.OnClickListener {

    /**
     * 记录小悬浮窗的宽度
     */
    public static int viewWidth;

    /**
     * 记录小悬浮窗的高度
     */
    public static int viewHeight;

    /**
     * 记录系统状态栏的高度
     */
    private static int statusBarHeight;

    /**
     * 用于更新小悬浮窗的位置
     */
    private WindowManager windowManager;

    /**
     * 小悬浮窗的参数
     */
    private WindowManager.LayoutParams mParams;

    /**
     * 记录当前手指位置在屏幕上的横坐标值
     */
    private float xInScreen;

    /**
     * 记录当前手指位置在屏幕上的纵坐标值
     */
    private float yInScreen;

    /**
     * 记录手指按下时在屏幕上的横坐标的值
     */
    private float xDownInScreen;

    /**
     * 记录手指按下时在屏幕上的纵坐标的值
     */
    private float yDownInScreen;

    /**
     * 记录手指按下时在小悬浮窗的View上的横坐标的值
     */
    private float xInView;

    /**
     * 记录手指按下时在小悬浮窗的View上的纵坐标的值
     */
    private float yInView;
    Context context;
    Display currentDisplay;
    SurfaceView surfaceView;
    SurfaceHolder surfaceHolder;
    Button mButton;
    MediaPlayer mediaPlayer;// 使用的是MediaPlayer来播放视频
    int videoWidth = 0; // 视频的宽度，初始化，后边会对其进行赋值
    int videoHeight = 0; // 同上
    boolean readyToPlayer = false;
    public final static String LOGCAT = "CUSTOM_VIDEO_PLAYER";
    private final ViewGroup view;

    public FloatWindowSmallView(Context context) {
        super(context);
        this.context = context;
        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        LayoutInflater.from(context).inflate(R.layout.float_window_small, this);
        view = (ViewGroup) findViewById(R.id.small_window_layout);
        viewWidth = view.getLayoutParams().width;
        viewHeight = view.getLayoutParams().height;

        initialUI();
//        TextView percentView = (TextView) findViewById(R.id.percent);
//        percentView.setText(MyWindowManager.getUsedPercentValue(context));
    }

    private void initialUI() {
        // setContentView(R.layout.main);
        // 以屏幕左上角为原点，设置x、y初始值
        currentDisplay = windowManager.getDefaultDisplay();

        // 关于SurfaceView和Surfaceolder可以查看文档
        surfaceView = (SurfaceView) view.findViewById(R.id.surfaceView);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnSeekCompleteListener(this);
        mediaPlayer.setOnVideoSizeChangedListener(this);
        String filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/I Believe.mp4";
        // 本地地址和网络地址都可以
        try {
            mediaPlayer.setDataSource(filePath);
        } catch (IllegalArgumentException e) {
            // TODO: handle exception
            Log.v(LOGCAT, e.getMessage());
            onExit();
        } catch (IllegalStateException e) {
            Log.v(LOGCAT, e.getMessage());
            onExit();
        } catch (IOException e) {
            Log.v(LOGCAT, e.getMessage());
            onExit();
        }
    }

    public void onExit() {

        try {
            MyWindowManager.removeSmallWindow(getContext());
            mediaPlayer.pause();
            mediaPlayer.stop();
            mediaPlayer.release();
        } catch (Exception e) {
            // TODO: handle exception
        }
    }

    private long startTime;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // 手指按下时记录必要数据,纵坐标的值都需要减去状态栏高度
                xInView = event.getX();
                yInView = event.getY();
                xDownInScreen = event.getRawX();
                yDownInScreen = event.getRawY() - getStatusBarHeight();
                xInScreen = event.getRawX();
                yInScreen = event.getRawY() - getStatusBarHeight();
                break;
            case MotionEvent.ACTION_MOVE:
                xInScreen = event.getRawX();
                yInScreen = event.getRawY() - getStatusBarHeight();
                // 手指移动的时候更新小悬浮窗的位置
                updateViewPosition();
                break;
            case MotionEvent.ACTION_UP:
                // 如果手指离开屏幕时，xDownInScreen和xInScreen相等，且yDownInScreen和yInScreen相等，则视为触发了单击事件。
                if (Math.abs(xDownInScreen - xInScreen) < 5 && Math.abs(yDownInScreen - yInScreen) < 5) {
                    long end = System.currentTimeMillis() - startTime;
                    // 双击的间隔在 300ms以下
                    if (end < 300) {
                        openBigWindow();
                    }
                    startTime = System.currentTimeMillis();
                }
                break;
            default:
                break;
        }
        return true;
    }

    /**
     * 将小悬浮窗的参数传入，用于更新小悬浮窗的位置。
     *
     * @param params 小悬浮窗的参数
     */
    public void setParams(WindowManager.LayoutParams params) {
        mParams = params;
    }

    /**
     * 更新小悬浮窗在屏幕中的位置。
     */
    private void updateViewPosition() {
        mParams.x = (int) (xInScreen - xInView);
        mParams.y = (int) (yInScreen - yInView);
        windowManager.updateViewLayout(this, mParams);
    }

    /**
     * 打开大悬浮窗，同时关闭小悬浮窗。
     */
    private void openBigWindow() {
        MyWindowManager.createBigWindow(getContext());
        MyWindowManager.removeSmallWindow(getContext());
    }

    /**
     * 用于获取状态栏的高度。
     *
     * @return 返回状态栏高度的像素值。
     */
    private int getStatusBarHeight() {
        if (statusBarHeight == 0) {
            try {
                Class<?> c = Class.forName("com.android.internal.R$dimen");
                Object o = c.newInstance();
                Field field = c.getField("status_bar_height");
                int x = (Integer) field.get(o);
                statusBarHeight = getResources().getDimensionPixelSize(x);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return statusBarHeight;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.e(LOGCAT, "suc calles");
        mediaPlayer.setDisplay(holder);// 若无此句，将只有声音而无图像
        try {
            mediaPlayer.prepare();
        } catch (IllegalStateException e) {
            onExit();
        } catch (IOException e) {
            onExit();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.e(LOGCAT, "surfaceChanged Called");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.e(LOGCAT, "surfaceDestroyed Called");
    }

    @Override
    public void onClick(View v) {

    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.e(LOGCAT, "onCompletion Called");
        onExit();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.v(LOGCAT, "onError Called");
        if (what == MediaPlayer.MEDIA_ERROR_SERVER_DIED) {
            Log.v(LOGCAT, "Media Error, Server Died " + extra);
        } else if (what == MediaPlayer.MEDIA_ERROR_UNKNOWN) {
            Log.v(LOGCAT, "Media Error, Error Unknown " + extra);
        }

        return false;
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        if (what == MediaPlayer.MEDIA_INFO_BAD_INTERLEAVING) {
            Log.v(LOGCAT, "Media Info, Media Info Bad Interleaving " + extra);
        } else if (what == MediaPlayer.MEDIA_INFO_NOT_SEEKABLE) {
            Log.v(LOGCAT, "Media Info, Media Info Not Seekable " + extra);
        } else if (what == MediaPlayer.MEDIA_INFO_UNKNOWN) {
            Log.v(LOGCAT, "Media Info, Media Info Unknown " + extra);
        } else if (what == MediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING) {
            Log.v(LOGCAT, "MediaInfo, Media Info Video Track Lagging " + extra);
        } else if (what == MediaPlayer.MEDIA_INFO_METADATA_UPDATE) {
            Log.v(LOGCAT, "MediaInfo, Media Info Metadata Update " + extra);
        }

        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.v(LOGCAT, "onPrepared Called");
        videoWidth = mp.getVideoWidth();
        videoHeight = mp.getVideoHeight();
        /** 这一步为videod的高宽赋值，将其值控制在可控的范围之内，在VideoView的源码中也有相关的代码，有兴趣可以一看 */
        if (videoWidth > currentDisplay.getWidth()
                || videoHeight > currentDisplay.getHeight()) {
            float heightRatio = (float) videoHeight
                    / (float) currentDisplay.getHeight();
            float widthRatio = (float) videoWidth
                    / (float) currentDisplay.getWidth();
            if (heightRatio > 1 || widthRatio > 1) {
                if (heightRatio > widthRatio) {
                    videoHeight = (int) Math.ceil((float) videoHeight
                            / (float) heightRatio);
                    videoWidth = (int) Math.ceil((float) videoWidth
                            / (float) heightRatio);
                } else {
                    videoHeight = (int) Math.ceil((float) videoHeight
                            / (float) widthRatio);
                    videoWidth = (int) Math.ceil((float) videoWidth
                            / (float) widthRatio);
                }
            }
        }

        videoWidth = 400;
        videoHeight = 300;

        // 设置悬浮窗口长宽数据
        mParams.width = videoWidth;
        mParams.height = videoHeight + 36;

       /* mButton = new Button(context);
        mButton.setText("close");
        FrameLayout.LayoutParams sParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        sParams.setMargins(0, videoHeight, 0, 0);
        mButton.setLayoutParams(sParams);
        mButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                onExit();
            }
        });
        view.addView(mButton);
*/
        surfaceView.setLayoutParams(new FrameLayout.LayoutParams(videoWidth,
                videoHeight));
        mp.start();
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        Log.e(LOGCAT, "onSeekComplete Called");
    }

    @Override
    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
        Log.e(LOGCAT, "onVideoSizeChanged Called");
    }
}
