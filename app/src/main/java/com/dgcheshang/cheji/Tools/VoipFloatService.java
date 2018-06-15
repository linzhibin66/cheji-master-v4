package com.dgcheshang.cheji.Tools;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.usb.UsbDevice;
import android.opengl.GLSurfaceView;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;

import com.chainway.facedet.FD_FSDKFace;
import com.chainway.facedet.FaceDetJni;
import com.dgcheshang.cheji.Activity.LoginCoachActivity;
import com.dgcheshang.cheji.R;
import com.dgcheshang.cheji.netty.conf.NettyConf;
import com.dgcheshang.cheji.netty.util.RlsbUtil;
import com.serenegiant.usb.DeviceFilter;
import com.serenegiant.usb.IFrameCallback;
import com.serenegiant.usb.Size;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.UVCCamera;
import com.serenegiant.widget.UVCCameraTextureView;
import com.shenyaocn.android.Encoder.CameraRecorder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Administrator on 2018/6/4 0004.
 */

public class VoipFloatService extends Service {
    private static final String TAG="TAG";

    private WindowManager mWindowManager;

    private WindowManager.LayoutParams mLayoutParams;

    private USBMonitor mUSBMonitor;					// 用于监视USB设备接入
    private UVCCamera mUVCCameraL;					// 表示左边摄像头设备
    private UVCCamera mUVCCameraR;					// 表示右边摄像头设备

    private OutputStream snapshotOutStreamL;		// 用于左边摄像头拍照
    private String snapshotFileNameL;

    private OutputStream snapshotOutStreamR;		// 用于右边摄像头拍照
    private String snapshotFileNameR;
    private UVCCameraTextureView mUVCCameraViewR;	// 用于右边摄像头预览
    private Surface mRightPreviewSurface;

    private UVCCameraTextureView mUVCCameraViewL;	// 用于左边摄像头预览
    private Surface mLeftPreviewSurface;
    private static final int PREVIEW_WIDTH = 320;
    private static final int PREVIEW_HEIGHT = 240;
    private CameraRecorder mp4RecorderL=new CameraRecorder(1);
    private CameraRecorder mp4RecorderR=new CameraRecorder(2);
    private int currentWidth = UVCCamera.DEFAULT_PREVIEW_WIDTH;
    private int currentHeight = UVCCamera.DEFAULT_PREVIEW_HEIGHT;
    public String landmarks_path, facenet_path, train_path;
    Button bt_open,bt_close;
    private ImageButton mCaptureButton;//拍照按钮

    View layout_showphoto;
/**

 * float的布局view

 */

    private View mFloatView;

    private GLSurfaceView glSurfaceView;

    private int mFloatWinWidth,mFloatWinHeight;//悬浮窗的宽高

    private int mFloatWinMarginTop,mFloatWinMarginRight;

    private int mLastX=0,mLastY=0;

    private int mStartX=0,mStartY=0;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String data = intent.getStringExtra("type");
        Log.e("TAG","service="+data);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override

    public void onCreate() {

        super.onCreate();

        Log.e(TAG,"onCreate: ");

        createWindowManager();

        createFloatView();

    }

    @Override

    public void onDestroy() {

        super.onDestroy();

        Log.e(TAG,"onDestroy: ");
//关闭USB摄像头
        if (mUSBMonitor != null) {
            mUSBMonitor.unregister();
            mUSBMonitor.destroy();
            mUSBMonitor = null;
        }

        //删除人脸识别初始化
        if(FaceDet!=null){
            FaceDet.FaceDetDeInit();
        }
        removeFloatView();

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createWindowManager() {

        Log.e(TAG,"createWindowManager: ");

// 取得系统窗体

        mWindowManager= (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);

//计算得出悬浮窗口的宽高

        DisplayMetrics metric =new DisplayMetrics();

        mWindowManager.getDefaultDisplay().getMetrics(metric);

        int screenWidth = metric.widthPixels;
        int heightPixels = metric.heightPixels;

        mFloatWinWidth = (int) (screenWidth );

        mFloatWinHeight=heightPixels;

//        mFloatWinMarginTop= (int)this.getResources().getDimension(R.dimen.rkcloud_av_floatwin_margintop);
//
//        mFloatWinMarginRight= (int)this.getResources().getDimension(R.dimen.rkcloud_av_floatwin_marginright);
//        mFloatWinWidth = 1;
//
//        mFloatWinHeight=1;
//
//        mFloatWinMarginTop=10;
//        mFloatWinMarginRight=10;

// 窗体的布局样式

// 获取LayoutParams对象

        mLayoutParams=new WindowManager.LayoutParams();

// 确定爱悬浮窗类型，表示在所有应用程序之上，但在状态栏之下

//TODO? 在android2.3以上可以使用TYPE_TOAST规避权限问题

        mLayoutParams.type= WindowManager.LayoutParams.TYPE_TOAST;//TYPE_PHONE

        mLayoutParams.format= PixelFormat.RGBA_8888;

        mLayoutParams.flags= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE| WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;

// 悬浮窗的对齐方式

        mLayoutParams.gravity= Gravity.RIGHT| Gravity.TOP;

// 悬浮窗的位置

        mLayoutParams.x=mFloatWinMarginRight;

        mLayoutParams.y=mFloatWinMarginTop;

        mLayoutParams.width=mFloatWinWidth;

        mLayoutParams.height=mFloatWinHeight;

    }

    /**

     * 创建悬浮窗

     */

    private void createFloatView() {

        Log.e(TAG,"createFloatView: ");

        LayoutInflater inflater = LayoutInflater.from(VoipFloatService.this);

        mFloatView= inflater.inflate(R.layout.voipfloat_layout, null);
        layout_showphoto = mFloatView.findViewById(R.id.layout_showphoto);
        mUVCCameraViewL = (UVCCameraTextureView)mFloatView.findViewById(R.id.camera_view_L);
        mUVCCameraViewL.setAspectRatio(PREVIEW_WIDTH / (float)PREVIEW_HEIGHT);
        mUVCCameraViewR = (UVCCameraTextureView)mFloatView.findViewById(R.id.camera_view_R);
        mUVCCameraViewR.setAspectRatio(PREVIEW_WIDTH / (float)PREVIEW_HEIGHT);
        mCaptureButton = (ImageButton)mFloatView.findViewById(R.id.capture_button);
        bt_open = (Button)mFloatView. findViewById(R.id.bt_open);
        bt_close = (Button)mFloatView. findViewById(R.id.bt_close);
        mCaptureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                captureSnapshot();
            }
        });
        bt_open.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mUSBMonitor.register();
            }
        });
        bt_close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                releaseCameraL();
                releaseCameraR();
                mUSBMonitor.unregister();
            }
        });
//        layout_showphoto.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                //点击其他地方关闭摄像头页面
//                layout_showphoto.setVisibility(View.GONE);
//                //关闭摄像头
//                if(mUSBMonitor.isRegistered()){
//                    //注册了
//                    releaseCameraR();
//                    releaseCameraL();
//                    mUSBMonitor.unregister();
//                }
//                return true;
//            }
//        });
        layout_showphoto.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mFloatView.setVisibility(View.GONE);
                //关闭摄像头
                if(mUSBMonitor.isRegistered()){
                    //注册了
                    releaseCameraR();
                    releaseCameraL();
                    mUSBMonitor.unregister();
                }
                return true;
            }
        });
        refreshControls(mFloatView);
        mUSBMonitor = new USBMonitor(this, mOnDeviceConnectListener);
        final List<DeviceFilter> filters = DeviceFilter.getDeviceFilters(this, R.xml.device_filter);
        mUSBMonitor.setDeviceFilter(filters);
        mUSBMonitor.register();
        refreshControls(mFloatView);
        initrlCamera();
        mWindowManager.addView(mFloatView,mLayoutParams);

    }


    private void removeFloatView() {

        Log.e(TAG,"removeFloatView: ");

        if(mFloatView!=null&&mWindowManager!=null) {

            mWindowManager.removeView(mFloatView);

        }

    }

    // 实现快照抓取
    private synchronized String captureSnapshot() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd.HH.mm.ss.SSSS");
        Date currentTime = new Date();
        if(isCameraL==true){
            snapshotFileNameL = Environment.getExternalStorageDirectory().getAbsolutePath() + "/chejiCamera";
            File path = new File(snapshotFileNameL);
            if (!path.exists())
                path.mkdirs();
            snapshotFileNameL += "/IPC_";
            snapshotFileNameL += format.format(currentTime);
            snapshotFileNameL += ".L.jpg";
            File recordFile = new File(snapshotFileNameL);	// 左边摄像头快照的文件名
            if(recordFile.exists()) {
                recordFile.delete();
            }
            try {
                boolean newFile = recordFile.createNewFile();
                snapshotOutStreamL = new FileOutputStream(recordFile);

            } catch (Exception e){
                Log.e("TAG",e.getMessage());
            }
            return snapshotFileNameL;
        }else {
            snapshotFileNameR = Environment.getExternalStorageDirectory().getAbsolutePath() + "/chejiCamera";
            File path1 = new File(snapshotFileNameR);
            if (!path1.exists())
                path1.mkdirs();
            snapshotFileNameR += "/IPC_";
            snapshotFileNameR += format.format(currentTime);
            snapshotFileNameR += ".R.jpg";
            File recordFile = new File(snapshotFileNameR);		// 右边摄像头快照的文件名
            if(recordFile.exists()) {
                recordFile.delete();
            }
            try {
                recordFile.createNewFile();
                snapshotOutStreamR = new FileOutputStream(recordFile);
            } catch (Exception e){}
            return snapshotFileNameR;
        }

    }


    private synchronized void releaseCameraL() {
        synchronized (this) {

            if (mUVCCameraL != null) {
                try {
                    mUVCCameraL.setStatusCallback(null);
                    mUVCCameraL.setButtonCallback(null);
                    mUVCCameraL.close();
                    mUVCCameraL.destroy();
                } catch (final Exception e) {
                    Log.e(TAG, e.getMessage());
                } finally {
                    mUVCCameraL = null;
                }
            }
            if (mLeftPreviewSurface != null) {
                mLeftPreviewSurface.release();
                mLeftPreviewSurface = null;
            }

        }
    }

    private synchronized void releaseCameraR() {
        synchronized (this) {

            if (mUVCCameraR != null) {
                try {
                    mUVCCameraR.setStatusCallback(null);
                    mUVCCameraR.setButtonCallback(null);
                    mUVCCameraR.close();
                    mUVCCameraR.destroy();
                } catch (final Exception e) {
                    Log.e(TAG, e.getMessage());
                } finally {
                    mUVCCameraR = null;
                }
            }

            if (mRightPreviewSurface != null) {
                mRightPreviewSurface.release();
                mRightPreviewSurface = null;
            }

        }
    }

    /**
     * 初始化人脸识别摄像头
     * */
    FaceDetJni FaceDet;
    List<FD_FSDKFace> faceResult;
    public void initrlCamera(){
        faceResult = new ArrayList<>();
        FaceDet = new FaceDetJni();

//        Speaking.in("正在人脸识别，请对准摄像头");

        Timer initbdtimer = new Timer();
        TimerTask task=new TimerTask() {
            @Override
            public void run() {
                Log.e("TAG","进行初始化比对");

                landmarks_path = RlsbUtil.getAssetsCacheFile(getApplicationContext(),"face_landmarks_5_cilab.dat");
                facenet_path = RlsbUtil.getAssetsCacheFile(getApplicationContext(),"facenet_cilab.dat");
                train_path =RlsbUtil.getAssetsCacheFile(getApplicationContext(),"complex_training.txt");
                FaceDet.FaceDetInit(landmarks_path, facenet_path, train_path);
                if(mUSBMonitor.isRegistered()){
                    //注册了
                    releaseCameraR();
                    releaseCameraL();
                    mUSBMonitor.unregister();
                }
                mUSBMonitor.register();
            }
        };
        initbdtimer.schedule(task,2000);
        RlsbUtil.addtimer(initbdtimer);

    }

    //判断是哪个摄像头在使用
    Boolean isCameraL =false;
    Boolean isCameraR =false;
    private final USBMonitor.OnDeviceConnectListener mOnDeviceConnectListener = new USBMonitor.OnDeviceConnectListener() {
        @Override
        public void onAttach(final UsbDevice device) {
            Log.i(TAG, "onAttach:" + device);
            final List<UsbDevice> list = mUSBMonitor.getDeviceList();
            mUSBMonitor.requestPermission(list.get(0));

            if(list.size() > 1)
                new Handler().postDelayed(new Runnable() {public void run() {mUSBMonitor.requestPermission(list.get(1));}}, 200);
        }

        @Override
        public void onConnect(final UsbDevice device, final USBMonitor.UsbControlBlock ctrlBlock, final boolean createNew) {

             Log.i(TAG, "onConnect:"+ctrlBlock.getVenderId());

            if(!NettyConf.camerastate) {
                NettyConf.camerastate = true;
//                Speaking.in("摄像头已开启");
            }

            synchronized (this) {
                if (mUVCCameraL != null && mUVCCameraR != null) { // 如果左右摄像头都打开了就不能再接入设备了
                    return;
                }
                if (ctrlBlock.getVenderId() == 2){

                    if (mUVCCameraL != null && mUVCCameraL.getDevice().equals(device)){
                        return;
                    }
                } else if (ctrlBlock.getVenderId() == 3) {

                    if ((mUVCCameraR != null && mUVCCameraR.getDevice().equals(device))) {
                        return;
                    }
                }else {
                    return;
                }
                final UVCCamera camera = new UVCCamera();
                final int open_camera_nums = (mUVCCameraL != null ? 1 : 0) + (mUVCCameraR != null ? 1 : 0);
                camera.open(ctrlBlock);

                try {
                    camera.setPreviewSize(PREVIEW_WIDTH, PREVIEW_HEIGHT, UVCCamera.FRAME_FORMAT_MJPEG, 0.5f); // 0.5f是一个重要参数，表示带宽可以平均分配给两个摄像头，如果是一个摄像头则是1.0f，可以参考驱动实现
                } catch (final IllegalArgumentException e1) {
                    Log.i(TAG, "MJPEG Failed");
                    try {
                        camera.setPreviewSize(PREVIEW_WIDTH, PREVIEW_HEIGHT, UVCCamera.DEFAULT_PREVIEW_MODE, 0.5f);
                    } catch (final IllegalArgumentException e2) {
                        try {
                            currentWidth = UVCCamera.DEFAULT_PREVIEW_WIDTH;
                            currentHeight = UVCCamera.DEFAULT_PREVIEW_HEIGHT;
                            camera.setPreviewSize(PREVIEW_WIDTH, PREVIEW_HEIGHT, UVCCamera.DEFAULT_PREVIEW_MODE, 0.5f);
                        } catch (final IllegalArgumentException e3) {
                            camera.destroy();

                            return;
                        }
                    }
                }

                // 将摄像头进行分配
                if(ctrlBlock.getVenderId() == 2 && mUVCCameraL == null) {
                    isCameraL=true;
                    mUVCCameraL = camera;
                    try {
                        if (mLeftPreviewSurface != null) {
                            mLeftPreviewSurface.release();
                            mLeftPreviewSurface = null;
                        }

                        final SurfaceTexture st = mUVCCameraViewL.getSurfaceTexture();
                        if (st != null)
                            mLeftPreviewSurface = new Surface(st);
                        mUVCCameraL.setPreviewDisplay(mLeftPreviewSurface);

                        mUVCCameraL.setFrameCallback(mUVCFrameCallbackL, UVCCamera.PIXEL_FORMAT_YUV420SP);
                        mUVCCameraL.startPreview();
                    } catch (final Exception e) {
                        Log.e(TAG, e.getMessage());
                    }
                } else if(ctrlBlock.getVenderId() == 3 && mUVCCameraR == null) {
                    //判断右边摄像头可用
                    isCameraL=false;
                    isCameraR=true;
                    mUVCCameraR = camera;
                    if (mRightPreviewSurface != null) {
                        mRightPreviewSurface.release();
                        mRightPreviewSurface = null;
                    }

                    final SurfaceTexture st = mUVCCameraViewR.getSurfaceTexture();
                    if (st != null)
                        mRightPreviewSurface = new Surface(st);
                    mUVCCameraR.setPreviewDisplay(mRightPreviewSurface);

                    mUVCCameraR.setFrameCallback(mUVCFrameCallbackR, UVCCamera.PIXEL_FORMAT_YUV420SP);
                    mUVCCameraR.startPreview();
                }

//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        refreshControls(mFloatView);
//
//                        //      if (mUVCCameraL != null || mUVCCameraR != null)
//                        //      startAudio();
//                    }
//                });
//                new Runnable() {
//                    @Override
//                    public void run() {
//                        refreshControls(mFloatView);
//                    }
//                };
                refreshControls(mFloatView);

            }
        }

        @Override
        public void onDisconnect(final UsbDevice device, final USBMonitor.UsbControlBlock ctrlBlock) {
            Log.i(TAG, "onDisconnect:" + device);
//            if(NettyConf.camerastate) {
//                NettyConf.camerastate = false;
//                Speaking.in("摄像头已断开");
//            }
		/*	runOnUiThread(new Runnable() {
				@Override
				public void run() {
					//refreshControls();
					if (mUVCCameraL == null && mUVCCameraR == null)
						stopAudio();
				}
			});*/

        }

        @Override
        public void onDettach(final UsbDevice device) {
            Log.i(TAG, "onDettach:" + device);
            if ((mUVCCameraL != null) && mUVCCameraL.getDevice().equals(device)) {
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        releaseCameraL();
//                    }
//                });
//                new Runnable() {
//                    @Override
//                    public void run() {
//                        releaseCameraL();
//                    }
//                };
                releaseCameraL();


            } else if ((mUVCCameraR != null) && mUVCCameraR.getDevice().equals(device)) {
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        releaseCameraR();
//                    }
//                });
//                new Runnable() {
//                    @Override
//                    public void run() {
//                        releaseCameraR();
//                    }
//                };
                releaseCameraR();

            }

        }

        @Override
        public void onCancel(final UsbDevice device) {
            Log.i(TAG, "onCancel:");

        }
    };


    // 左边摄像头的NV21视频帧回调
    private final IFrameCallback mUVCFrameCallbackL = new IFrameCallback() {
        @Override
        public void onFrame(final ByteBuffer frame) {

            if(mUVCCameraL == null)
                return;

            final Size size = mUVCCameraL.getPreviewSize();
            byte[] buffer = null;

            int FrameSize = frame.remaining();
            if (buffer == null) {
                buffer = new byte[FrameSize];
                frame.get(buffer);
            }
            if (mp4RecorderL.isVideoRecord()) { // 将视频帧发送到编码器
                mp4RecorderL.feedData(buffer);
            }

            if(snapshotOutStreamL != null) { // 将视频帧压缩成jpeg图片，实现快照捕获
                if (!(FrameSize < size.width * size.height * 3 / 2) && (buffer != null)) {
                    try {
                        new YuvImage(buffer, ImageFormat.NV21, size.width, size.height, null).compressToJpeg(new Rect(0, 0, size.width, size.height), 60, snapshotOutStreamL);
                        snapshotOutStreamL.flush();
                        snapshotOutStreamL.close();
                        Helper.fileSavedProcess(getApplicationContext(), snapshotFileNameL);
                    } catch (Exception ex) {
                    } finally {
                        snapshotOutStreamL = null;
                    }
                }
            }
            buffer = null;
        }
    };

    // 参考上面的注释
    private final IFrameCallback mUVCFrameCallbackR = new IFrameCallback() {
        @Override
        public void onFrame(final ByteBuffer frame) {

            if(mUVCCameraR == null)
                return;

            final Size size = mUVCCameraR.getPreviewSize();
            byte[] buffer = null;

            int FrameSize = frame.remaining();
            if (buffer == null) {
                buffer = new byte[FrameSize];
                frame.get(buffer);
            }

            if (mp4RecorderR.isVideoRecord()) {
                mp4RecorderR.feedData(buffer);
            }

            if(snapshotOutStreamR != null) {
                if (!(FrameSize < size.width * size.height * 3 / 2) && (buffer != null)) {
                    try {
                        new YuvImage(buffer, ImageFormat.NV21, size.width, size.height, null).compressToJpeg(new Rect(0, 0, size.width, size.height), 60, snapshotOutStreamR);
                        snapshotOutStreamR.flush();
                        snapshotOutStreamR.close();
                        Helper.fileSavedProcess(getApplicationContext(), snapshotFileNameR);
                    } catch (Exception ex) {
                    } finally {
                        snapshotOutStreamR = null;
                    }
                }
            }
            buffer = null;
        }
    };
    // 刷新UI控件状态
    private void refreshControls(View mFloatView) {
        try {
            boolean enabled = (mUVCCameraL != null || mUVCCameraR != null);
            mFloatView.findViewById(R.id.capture_button).setEnabled(enabled);
            mFloatView.findViewById(R.id.textViewUVCPromptL).setVisibility(mUVCCameraL != null ? View.GONE : View.VISIBLE);
            mFloatView.findViewById(R.id.textViewUVCPromptR).setVisibility(mUVCCameraR != null ? View.GONE : View.VISIBLE);

        } catch (Exception e){}
    }

}
