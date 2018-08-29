package com.dgcheshang.cheji.Activity;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DownloadManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.usb.UsbDevice;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.chainway.facedet.FD_FSDKFace;
import com.chainway.facedet.FaceDetJni;
import com.dgcheshang.cheji.Activity.Lukao.LukaoActivity;
import com.dgcheshang.cheji.Bean.VersionBean;
import com.dgcheshang.cheji.CjApplication;
import com.dgcheshang.cheji.R;
import com.dgcheshang.cheji.Tools.Helper;
import com.dgcheshang.cheji.Tools.LoadingDialogUtils;
import com.dgcheshang.cheji.Tools.Speaking;
import com.dgcheshang.cheji.Tools.Speakout;
import com.dgcheshang.cheji.broadcastReceiver.TrainReceiver;
import com.dgcheshang.cheji.netty.conf.NettyConf;
import com.dgcheshang.cheji.netty.thread.SpeakThread;
import com.dgcheshang.cheji.netty.timer.CacheTimer;
import com.dgcheshang.cheji.netty.timer.DzwlTimerTask;
import com.dgcheshang.cheji.netty.timer.LoginoutTimer;
import com.dgcheshang.cheji.netty.util.InitUtil;
import com.dgcheshang.cheji.netty.util.LocationUtil;
import com.dgcheshang.cheji.netty.util.RlsbUtil;
import com.dgcheshang.cheji.netty.util.ZdUtil;
import com.dgcheshang.cheji.nettygps.conf.Params;
import com.dgcheshang.cheji.nettygps.util.GpsUtil;
import com.dgcheshang.cheji.networkUrl.NetworkUrl;
import com.google.gson.Gson;
import com.rscja.customservices.ICustomServices;
import com.serenegiant.usb.CameraDialog;
import com.serenegiant.usb.DeviceFilter;
import com.serenegiant.usb.IFrameCallback;
import com.serenegiant.usb.Size;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.UVCCamera;
import com.serenegiant.widget.UVCCameraTextureView;
import com.shenyaocn.android.Encoder.CameraRecorder;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.util.EncodingUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 主菜单
 * */
public class LoginActivity extends BaseInitActivity implements View.OnClickListener,CameraDialog.CameraDialogParent {
    public static LoginActivity instance = null;

    Context context = LoginActivity.this;

    private static final String TAG = "LoginActivity";
    public static final int REQUEST_COACH = 1;//跳转教练登录页面
    public static final int REQUEST_STUDENT = 2;//跳转学员登录页面
    public static final int REQUEST_SETTING = 3;//跳转设置ip,端口页面
    Timer qzOutTimer;
    Dialog loading;
    TextView tv_coach_state,tv_student_state;
    private static final int PREVIEW_WIDTH = 320;
    private static final int PREVIEW_HEIGHT = 240;
    private static final int PREVIEW_MODE = 1;

    private static final boolean DEBUG = true;
    private static final boolean USE_SURFACE_ENCODER = false;
    private final Object mSync = new Object();
    String fileurl="/sdcard/APPdown";//下载文件夹路径
    BroadcastReceiver receiver;//下载广播
    NetworkReceiver networkReceiver;//网络监听广播
    View layout_showphoto;
    SoundPool soundPool;
    boolean isFirstStartApp=false;//是否第一次启动app
    TrainReceiver trainReceiver;
    SharedPreferences stusp,coachsp;
    ICustomServices mCustomServices;

    public static boolean hyconstate=false;//华盈连接标志

    private int bufferSize;

    private USBMonitor mUSBMonitor;					// 用于监视USB设备接入
    private UVCCamera mUVCCameraL;					// 表示左边摄像头设备
    private UVCCamera mUVCCameraR;					// 表示右边摄像头设备

    private OutputStream snapshotOutStreamL;		// 用于左边摄像头拍照
    private String snapshotFileNameL;

    private static final float[] BANDWIDTH_FACTORS = { 0.5f, 0.5f };

    private int currentWidth = UVCCamera.DEFAULT_PREVIEW_WIDTH;
    private int currentHeight = UVCCamera.DEFAULT_PREVIEW_HEIGHT;

    private UVCCameraTextureView mUVCCameraViewL;	// 用于左边摄像头预览
    private Surface mLeftPreviewSurface;
    private CameraRecorder mp4RecorderL=new CameraRecorder(1);

    private FaceDetJni FaceDet = new FaceDetJni();
    public String landmarks_path, facenet_path, train_path;
    List<FD_FSDKFace> faceResult = new ArrayList<>();
    //指纹模块初始化
    SharedPreferences zdcs;
    SharedPreferences.Editor zdcs_edit;
    int js = 0;//定时初始化时间
    boolean stopcamera=false;//控制是否停止循环抓拍照片比对
    boolean ismakephoto=false;//是否在拍照
    Handler handler=new  Handler(){
        @Override
        public void handleMessage(Message msg) {
            if(msg.arg1==1) {//注销
                handleCancel(msg);
            }else if(msg.arg1==2){
                if(isCameraL==true||isCameraR==true){
                    //摄像头正常
                    Bundle data = msg.getData();
                    final String scms = data.getString("scms");
                    final String tdh = data.getString("tdh");
                    final String lx = data.getString("lx");
                    final String gnss=data.getString("gnss");
                    //拍照提前发出滴滴声
                    soundPool.play(1,1, 1, 0, 0, 1);
                    //开启摄像头
                    mUSBMonitor.register();
                    stopcamera=false;
                    //判断抓拍模式是哪种
                    if(!lx.equals("5")){
                        //指纹识别 不比对照片
                        final String path = captureSnapshot();
                        postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                ZdUtil.sendZpsc2(scms,tdh,lx,gnss,path);
                                //  关闭摄像头
                                releaseCameraL();
                                mUSBMonitor.unregister();
                            }
                        },3000);

                    }else {
                        //人脸识别
                        ismakephoto=true;
                        //计时timer
                        final Timer jstimer = new Timer();
                        TimerTask task=new TimerTask() {
                            @Override
                            public void run() {
                                js++;
                                Log.e("TAG",js+"");
                                final String path1 = captureSnapshot();
                                Log.e("TAG","拍照1");
                                stopcamera=compare(path1);
                                if(stopcamera==true){
                                    jstimer.cancel();
                                    js=0;
                                    //上传拍照数据
                                    postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            ZdUtil.sendZpsc2(scms,tdh,lx,gnss,path1);
                                            //  关闭摄像头
                                            releaseCameraL();
                                            mUSBMonitor.unregister();
                                            ismakephoto=false;
                                        }
                                    },3000);
                                }
                                if(js==20){
                                    //结束对比，直接抓拍
                                    Log.e("TAG","结束对比，直接抓拍");
                                    js=0;
                                    stopcamera=true;
                                    jstimer.cancel();
                                    //拍照
                                    final String path = captureSnapshot();
                                    postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            ZdUtil.sendZpsc2(scms,tdh,lx,gnss,path);
                                            //  关闭摄像头
                                            releaseCameraL();
                                            mUSBMonitor.unregister();
                                            ismakephoto=false;
                                        }
                                    },3000);
                                }
                            }
                        };
                        jstimer.schedule(task,1000,3000);
                    }
                }else {
                  //摄像头不正常
                    Speaking.in("摄像头异常");
                }

            }else if(msg.arg1==3){

            }else if(msg.arg1==5){
                handleshow();

            }else if(msg.arg1==6){
                Bundle data = msg.getData();
                String url= (String) data.getSerializable("url");
                String version= (String) data.getSerializable("version");
                downFile(url, version);

            }else if(msg.arg1==11){
                //重启位置汇报广播
                Intent intent=new Intent();
                intent.setAction("wzhb");
                sendBroadcast(intent);

            }else if(msg.arg1==12){
                //重启系统或关机
                seReboot();

            }else if(msg.arg1==22){
                //自动登出触发(可能)
                Intent xydcIntent=new Intent();
                xydcIntent.setAction("xydc");
                sendBroadcast(xydcIntent);

            }else if(msg.arg1==13){
                //强制登出与指令拍照
                if(isCameraL==true||isCameraR==true){
                    //摄像头正常
                    Bundle data = msg.getData();
                    final String scms = data.getString("scms");
                    final String tdh = data.getString("tdh");
                    final String lx = data.getString("lx");
                    final String gnss=data.getString("gnss");
                    //拍照提前发出滴滴声
                    soundPool.play(1,1, 1, 0, 0, 1);
                    //开启摄像头
                    mUSBMonitor.register();
                    final String path = captureSnapshot();
                    postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            ZdUtil.sendZpsc2(scms,tdh,lx,gnss,path);
                            //  关闭摄像头
                            releaseCameraL();
                            mUSBMonitor.unregister();
                        }
                    },3000);
                }else {
                    //摄像头异常
                    Speaking.in("摄像头异常");
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        com.rscja.deviceapi.OTG.getInstance().on(); //打开OTG
        //启动语音播放器
        new Thread(new SpeakThread()).start();
        trainReceiver=new TrainReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("wzhb");
        filter.addAction("xydl");
        filter.addAction("xydc");
        filter.addAction("autoLoginout");
        filter.addAction("autoLoginwarn");

        filter.addAction("reboot");
        this.registerReceiver(trainReceiver,filter);

        NettyConf.handlersmap.put("login",handler);

        //强制打开GPS
        try {
            if (!LocationUtil.isOPen()) {
                openGPS(this);
            }
        }catch(Exception e){}

        //初始化
        InitUtil.initSystem();
        loading = LoadingDialogUtils.createLoadingDialog(context, "正在初始化...");
        initView();
        //广播
        registerReceiver();
        instance = this;

        //拍照秒提示嘀嘀声初始化
        soundPool= new SoundPool(10, AudioManager.STREAM_SYSTEM,5);
        soundPool.load(CjApplication.getInstance(), R.raw.didi4,1);

        //清除缓存数据
        CacheTimer cacheTimer=new CacheTimer();
        new Timer().scheduleAtFixedRate(cacheTimer,0,24*60*60*1000);

        //看是否学员登陆成功了学时汇报和拍照是否启动
        if(NettyConf.xystate==1){
            //发送学员登陆广播
            Intent xydlIntent=new Intent();
            xydlIntent.setAction("xydl");
            sendBroadcast(xydlIntent);

            try {
                SimpleDateFormat dff = new SimpleDateFormat("yyMMdd");
                String ee = dff.format(new Date());

                String ee2 = NettyConf.xydltime.substring(0, 6);
                if (!ee.equals(ee2)) {
                    Toast.makeText(context,"培训已跨天,学时无效,请登出后重新登陆！",Toast.LENGTH_SHORT).show();
                }
            }catch(Exception e){}
        }

        //定时验证身份
        //new Timer().scheduleAtFixedRate(new ValidateTimerTask(),NettyConf.cxyzsj*60*1000,NettyConf.cxyzsj*60*1000);

        //启动强制登出定时器
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date=new Date();//取时间
        Calendar calendar = Calendar.getInstance();
        StringBuffer sb=new StringBuffer(sdf.format(date).substring(0,11));
        sb.append("23:55:00");
        try {
            if(NettyConf.debug){
                Log.e("TAG","登出计时器触发时间:"+sb.toString());
            }
            Date d=sdf.parse(sb.toString());
            Intent intent=new Intent();
            intent.setAction("autoLoginout");
            PendingIntent pi=PendingIntent.getBroadcast(this, 0, intent,0);
            //设置一个PendingIntent对象，发送广播
            AlarmManager am=(AlarmManager)getSystemService(ALARM_SERVICE);
            //获取AlarmManager对象
            am.set(AlarmManager.RTC_WAKEUP, d.getTime(), pi);

            //在加一次自动登出
            calendar.setTime(d);
            calendar.add(Calendar.MINUTE, 5);
            Date dd=calendar.getTime();
            new Timer().schedule(new LoginoutTimer(), dd);

            calendar.setTime(d);
            calendar.add(Calendar.MINUTE, -5);

            Date d2=calendar.getTime();
            if(NettyConf.debug){
                Log.e("TAG","登出报警计时器触发时间:"+sdf.format(d2));
            }

            Intent i2=new Intent();
            i2.setAction("autoLoginwarn");
            PendingIntent pi2=PendingIntent.getBroadcast(this, 0, i2,0);
            AlarmManager am3=(AlarmManager)getSystemService(ALARM_SERVICE);
            am3.set(AlarmManager.RTC_WAKEUP, d2.getTime(), pi2);

            calendar.setTime(d);
            calendar.add(Calendar.MINUTE, 5);
            calendar.add(Calendar.SECOND,(int)(Math.random()*(59)));
            Date d3=calendar.getTime();

            Intent i3=new Intent();
            i3.setAction("reboot");
            PendingIntent pi3=PendingIntent.getBroadcast(this, 0, i3,0);
            AlarmManager am4=(AlarmManager)getSystemService(ALARM_SERVICE);
            am4.set(AlarmManager.RTC_WAKEUP, d3.getTime(), pi3);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        /**
         * 启动电子围栏
         */
        if("1".equals(NettyConf.dzwlcl)) {
            NettyConf.dzwlTimer=new Timer();
            NettyConf.dzwlTimer.schedule(new DzwlTimerTask(), 0, NettyConf.dzwllxsj*1000);
        }
//        readSIM();
    }

    /**
     * 初始化布局
     * */
    private void initView() {
        coachsp = getSharedPreferences("coach", Context.MODE_PRIVATE);//教练员保存数据
        stusp = getSharedPreferences("student", Context.MODE_PRIVATE);//学员保存数据
        zdcs = getSharedPreferences("zdcs", Context.MODE_PRIVATE);//获取终端保存参数信息
        zdcs_edit = zdcs.edit();//终端参数编辑器
        View layout_back = findViewById(R.id.layout_back);//返回
        View layout_coach = findViewById(R.id.layout_coach);//教练
        View layout_student = findViewById(R.id.layout_student);//学员
        tv_coach_state = (TextView) findViewById(R.id.tv_coach_state);//教练显示状态
        tv_student_state = (TextView) findViewById(R.id.tv_student_state);//学员显示状态
        View layout_cardetail = findViewById(R.id.layout_cardetail);//车辆信息
        View layout_about = findViewById(R.id.layout_about);//关于我们
        View layout_lukao = findViewById(R.id.layout_lukao);//模拟路考
        View layout_setting = findViewById(R.id.layout_setting);//参数设置
        View layout_basic_set = findViewById(R.id.layout_basic_set);//基本设置
        layout_showphoto = findViewById(R.id.layout_showphoto);//显示拍照框
        layout_showphoto.setVisibility(View.INVISIBLE);
        layout_basic_set.setOnClickListener(this);
        layout_back.setOnClickListener(this);
        layout_coach.setOnClickListener(this);
        layout_student.setOnClickListener(this);
        layout_cardetail.setOnClickListener(this);
        layout_about.setOnClickListener(this);
        layout_setting.setOnClickListener(this);
        layout_lukao.setOnClickListener(this);
        layout_showphoto.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(ZdUtil.ispz==false){
                    //点击其他地方关闭摄像头页面
                    layout_showphoto.setVisibility(View.INVISIBLE);
                    //关闭摄像头
                    if(mUSBMonitor.isRegistered()){
                        //注册了
                        releaseCameraL();
                        mUSBMonitor.unregister();
                    }
                }
                return true;
            }
        });

//        //摄像头
        mUVCCameraViewL = (UVCCameraTextureView)findViewById(R.id.camera_view_L);
        mUVCCameraViewL.setAspectRatio(PREVIEW_WIDTH / (float)PREVIEW_HEIGHT);
        refreshControls();
        mUSBMonitor = new USBMonitor(this, mOnDeviceConnectListener);
        final List<DeviceFilter> filters = DeviceFilter.getDeviceFilters(this, R.xml.device_filter);
        mUSBMonitor.setDeviceFilter(filters);
        mUSBMonitor.register();//start
        refreshControls();
        initrlCamera();
    }

    /**
     * 点击监听
     * */
    @Override
    public void onClick(View view) {
        Intent intent = new Intent();
        switch (view.getId()) {
            case R.id.layout_back://摄像头
                if(ZdUtil.ispz==false){
                    layout_showphoto.setVisibility(View.VISIBLE);
                    if(mUSBMonitor!=null){
                        //注册了
                        releaseCameraL();
                        mUSBMonitor.unregister();
                    }
                    mUSBMonitor.register();
                }else {
                    Toast.makeText(context,"正在拍照，请稍后",Toast.LENGTH_SHORT).show();
                }
                break;

            case R.id.layout_coach://教练员登录
                if(ZdUtil.ispz==false){
                    if(NettyConf.jlstate==0&&ZdUtil.canLogin()) {
                        //未登录
                        logintypeDialog(1);
//                        intent.setClass(context, LoginCoachActivity.class);
//                        startActivityForResult(intent, REQUEST_COACH);
                    }else if(NettyConf.jlstate==1&&ZdUtil.canLogin()){
                        //登录过
                        intent.setClass(context, LoginCoachActivity.class);
                        startActivityForResult(intent, REQUEST_COACH);
                    }
                }else {
                    Toast.makeText(context,"正在拍照，请稍后",Toast.LENGTH_SHORT).show();
                }
                break;

            case R.id.layout_student://学员登录
                if(ZdUtil.ispz==false){
                    if(NettyConf.xystate == 0 && NettyConf.jlstate == 0){
                        Toast.makeText(context, "请先登录教练员", Toast.LENGTH_SHORT).show();
                    }else {
                        if(NettyConf.xystate==0&&ZdUtil.canLogin()) {
                            //未登录
                            logintypeDialog(4);
                        }else if(NettyConf.xystate==1&&ZdUtil.canLogin()){
                            //登录过
                            intent.setClass(context, LoginStudentActivity.class);
                            startActivityForResult(intent, REQUEST_STUDENT);
                        }
                    }
//                    if(NettyConf.xystate==1||ZdUtil.canLogin()) {
//                        if (NettyConf.xystate == 0 && NettyConf.jlstate == 0) {
//                            Toast.makeText(context, "请先登录教练员", Toast.LENGTH_SHORT).show();
//                        } else {
//                            logintypeDialog(4);
////                            intent.setClass(context, LoginStudentActivity.class);
////                            startActivityForResult(intent, REQUEST_STUDENT);
//                        }
//                    }
                }else {
                    Toast.makeText(context,"正在拍照，请稍后",Toast.LENGTH_SHORT).show();
                }
                break;

            case R.id.layout_setting://参数设置
                if(ZdUtil.ispz==false){
                    intent.setClass(context,MainActivity.class);
                    startActivityForResult(intent, REQUEST_SETTING);
                }else {
                    Toast.makeText(context,"正在拍照，请稍后",Toast.LENGTH_SHORT).show();
                }
                break;

            case R.id.layout_basic_set://基本设置
                if(ZdUtil.ispz==false){
                    intent.setClass(context,SystemSetActivity.class);
                    startActivityForResult(intent, REQUEST_SETTING);
                }else {
                    Toast.makeText(context,"正在拍照，请稍后",Toast.LENGTH_SHORT).show();
                }
                break;

            case R.id.layout_cardetail://车辆信息
                if(ZdUtil.ispz==false){
                    intent.setClass(context,CarDetailActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                }else {
                    Toast.makeText(context,"正在拍照，请稍后",Toast.LENGTH_SHORT).show();
                }
                break;

            case R.id.layout_about://关于我们
                if(ZdUtil.ispz==false){
                    intent.setClass(context,AboutActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                }else {
                    Toast.makeText(context,"正在拍照，请稍后",Toast.LENGTH_SHORT).show();
                }
                break;

            case R.id.layout_lukao://模拟路考
                if(ZdUtil.ispz==false){
                    intent.setClass(context, LukaoActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                }else {
                    Toast.makeText(context,"正在拍照，请稍后",Toast.LENGTH_SHORT).show();
                }
                break;

        }
    }

    /**
     * 跳转页面返回回来结果处理
     * */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode){
            case REQUEST_COACH://教练页面返回来
                switch (resultCode){
                    case LoginCoachActivity.LOGIN_COA_SUCCESS://教练返回回来

                        break;
                }
                break;

            case REQUEST_STUDENT://学员页面返回来
                switch (resultCode){
                    case LoginStudentActivity.LOGIN_STU_SUCCESS://学员返回回来

                        break;
                }
                break;

        }
    }


    /**
     * 控制返回键无效
     * */
    public boolean onKeyDown(int keyCode,KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0){
//            //这里重写返回键
//            return true;
        }
        return false;
    }

    /**
     * 注销返回处理
     *
     * @param msg*/
    public void handleCancel(Message msg){

        Bundle data = msg.getData();
        int zxjg = (int) data.get("zxjg");
        if(zxjg==1){//注销成功
            //清除定位计时器
            Object o=NettyConf.timermap.get("wzhb");
            if(o!=null){
                Timer timer= (Timer) o;
                timer.cancel();
            }
            //清除定位服务
            o=NettyConf.servicemap.get("wzhb");
            if(o!=null){
                Intent intent= (Intent) o;
                stopService(intent);
            }
            NettyConf.zcstate=0;//改变注册状态
            NettyConf.jqstate=0;//改变鉴权状态
            //清除保存状态
            SharedPreferences jianquan = getSharedPreferences("jianquan", Context.MODE_PRIVATE);
            Intent intent = new Intent();
            intent.setClass(context,MainActivity.class);
            jianquan.edit().clear();
            startActivity(intent);
            finish();
        }else {
            //注销失败
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        handleshow();
    }

    public void handleshow(){
        if(NettyConf.jlstate==1){
            tv_coach_state.setText("教练员管理(已登录)");
        }else {
            tv_coach_state.setText("教练员管理(未登录)");
        }
        if(NettyConf.xystate==1){
            tv_student_state.setText("学员管理(已登录)");
        }else {
            tv_student_state.setText("学员管理(未登录)");
            //息屏
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }


    /**
     * to access from CameraDialog
     * @return
     */
    @Override
    public USBMonitor getUSBMonitor() {
        return mUSBMonitor;
    }

    @Override
    public void onDialogResult(boolean canceled) {
        if (DEBUG) Log.v(TAG, "onDialogResult:canceled=" + canceled);
        if (canceled) {
        }
    }

    /**
     * app获取版本,是否需要更新
     * */
    public void getVersion( ) {
        StringRequest request = new StringRequest(Request.Method.POST, NetworkUrl.UpdateCodeUrl, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    Gson gson = new Gson();
                    VersionBean versionbean = gson.fromJson(response, VersionBean.class);

                    //管理员卡uid
                    if(!versionbean.getManageruid().equals("")){
                        //保存管理员卡号
                        SharedPreferences uidsp = getSharedPreferences("uid", Context.MODE_PRIVATE);
                        SharedPreferences.Editor edit = uidsp.edit();
                        if(NettyConf.debug){
                            Log.e("TAG",versionbean.getManageruid());
                        }
                        edit.putString("uid",versionbean.getManageruid());
                        edit.commit();
                    }
                    String version = versionbean.getVersion();//版本号
                    String url = versionbean.getUrl();//下载路径
                    //判断是否版本一致
                    if (Double.valueOf(versionbean.getVersion())>Double.valueOf(NettyConf.version)) {
                        //进行版本更新
//                        updateDialog(versionbean.getUrl(), versionbean.getMsg());
                        if(versionbean.getImei().equals("")){
                            //全部更新
                            updateDialog(url,version);
                        }else {
                            //个别更新
                            String imei = versionbean.getImei();
                            String[] split = imei.split(",");
                            for (int i=0; i<split.length;i++){
                                if(split[i].equals(NettyConf.imei)){
                                    updateDialog(url,version);
                                    return;
                                }

                            }
                        }
                    }else {
                        //没有新版本更新，删除创建的文件夹里的文件
                        File file = new File(fileurl);
                        deleteAllFiles(file);

                    }
                }catch (Exception e){

                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Log.e("TGA","volleyError="+volleyError);
            }
        }) {

            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> map = new HashMap<>();
                return map;
            }
        };
        CjApplication.getHttpQueue().add(request);
    }

    /**
     * 删除文件夹底下所有文件
     * */
    private void deleteAllFiles(File root) {
        File files[] = root.listFiles();
        if (files != null)
            for (File f : files) {
                if (f.isDirectory()) { // 判断是否为文件夹
                    deleteAllFiles(f);
                    try {
                        f.delete();
                    } catch (Exception e) {
                    }
                } else {
                    if (f.exists()) { // 判断是否存在
                        deleteAllFiles(f);
                        try {
                            f.delete();
                        } catch (Exception e) {
                        }
                    }
                }
            }
    }

    /**
     * 版本更新提
     *
     * @param url
     * @param version*/
    private void updateDialog(final String url, final String version){
        AlertDialog.Builder builder=new AlertDialog.Builder(this);  //先得到构造器
        builder.setTitle("版本提示"); //设置标题
        builder.setMessage("有新版本"+version+"，是否更新？\n(全程自动安装，请等待系统自动重启安装完成)"); //设置内容
        builder.setPositiveButton("更新",     new DialogInterface.OnClickListener() { //设置确定按钮
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                boolean b = fileIsExists(version);//判断文件夹或者最新apk文件是否存在
                if(b==true){
                    //存在
                    updateApp(version);
                }else {
                    //不存在
                    downFile(url,version);
                }

            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() { //设置取消按钮
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        //参数都设置完成了，创建并显示出来//不可按返回键取消
        builder.setCancelable(false).create().show();
    }

    /**
     * 判断文件夹和最新apk是否存在
     *
     * @param version*/
    public boolean fileIsExists(String version)
    {
            File f=new File(fileurl);
            if(!f.exists()){
               //不存在
                f.mkdirs();
                return false;
            }else {
               //文件夹存在
                File file = new File(fileurl + "/cheji" + version + ".apk");
                //判断最新版本apk是否存在
                if(!file.exists()){
                    return false;
                }else {
                    return true;
                }
            }

    }

    /**
     * 下载文件
     * */
    public void downFile(String url, final String version){
        if(loading!=null){
            loading.cancel();
        }
        loading = LoadingDialogUtils.createLoadingDialog(context, "自动完成更新，无需操作...");
        loading.setCancelable(false);
        //下载文件
        final DownloadManager dManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        Uri uri = Uri.parse(url);
        DownloadManager.Request request = new DownloadManager.Request(uri);
        // 设置下载路径和文件名
        request.setDestinationInExternalPublicDir("APPdown", "cheji"+version+".apk");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
        request.setDescription("培训系统app正在下载");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setMimeType("application/vnd.android.package-archive");
        request.setAllowedOverRoaming(false);
        // 设置为可被媒体扫描器找到
        request.allowScanningByMediaScanner();
        // 设置为可见和可管理
         request.setVisibleInDownloadsUi(true);
        // 获取此次下载的ID
         final long refernece = dManager.enqueue(request);

        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);

        receiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                       long myDwonloadID = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (refernece == myDwonloadID) {
//                                Intent install = new Intent(Intent.ACTION_VIEW);
//                                Uri downloadFileUri = dManager.getUriForDownloadedFile(refernece);
//                                 install.setDataAndType(downloadFileUri, "application/vnd.android.package-archive");
//                                install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                                startActivity(install);

                                updateApp(version);
                               }
                        }
              };
         registerReceiver(receiver, filter);
        }

    /**
     * 自动升级
     *
     * @param version*/
    public void updateApp(String version){
        Boolean blStart = true;    //  安装后是否启动 ,  true，启动； false，不启动
        Intent apk = new Intent("com.wskyo.intent.systemupdate.InstallApk");
        apk.setPackage("com.wskyo.systemupdate");
        apk.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        apk.putExtra("ApkFile", fileurl+"/cheji"+version+".apk");//  apk 目录
        apk.putExtra("ApkStart",blStart);
        startService(apk);

    }

    /**
     * 注册网络监听广播
     * */

    private  void registerReceiver(){
        IntentFilter filter=new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        networkReceiver = new NetworkReceiver();
        this.registerReceiver(networkReceiver, filter);
    }

    /**
     * 网络广播
     * */
    public class NetworkReceiver extends BroadcastReceiver {
        boolean shenji=false;
        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mobNetInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
            NetworkInfo wifiNetInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

            boolean state;

            if (!mobNetInfo.isConnected() && !wifiNetInfo.isConnected()) {
                state=false;
            }else{
                state=true;
                if(shenji==false){
                    getVersion();//获取版本更新
                    shenji=true;
                }

            }

            if (NettyConf.netstate == null) {
                NettyConf.netstate = state;
                if(state){
                    ZdUtil.conServer();

                    GpsUtil.conServer();
                }
            } else if (NettyConf.netstate != state) {
                NettyConf.netstate = state;
                //如果网络变化
                if (state) {
                    ZdUtil.conServer();

                    GpsUtil.conServer();
                } else {
                    NettyConf.constate = 0;
                    NettyConf.jqstate = 0;

                    Params.gpsconstate=0;
                    Params.gpsjqstate=0;
                    Speaking.in("网络已断开");
                }
            }

        }
    }

    /**
     * 释放home键跟菜单键，及下拉通知栏功能
     * */
    boolean b;
    public void seReboot(){
        Intent intentCust = new Intent();
        intentCust.setAction("com.rscja.CustomService");
        intentCust.setPackage("com.rscja.customservices");
        b = bindService(intentCust, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mCustomServices = ICustomServices.Stub.asInterface(iBinder);
            try {
                if(NettyConf.autoroot==2) {
                    mCustomServices.reboot();
                }else if(NettyConf.autoroot==1){
                    mCustomServices.shutdown();
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    /**
     * 强制帮用户打开GPS
     * @param context
     */
    public static final void openGPS(Context context) {
        Intent GPSIntent = new Intent();
        GPSIntent.setClassName("com.android.settings",
                "com.android.settings.widget.SettingsAppWidgetProvider");
        GPSIntent.addCategory("android.intent.category.ALTERNATIVE");
        GPSIntent.setData(Uri.parse("custom:3"));
        try {
            PendingIntent.getBroadcast(context, 0, GPSIntent, 0).send();
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }
    }

    /**
     * 刷新摄像头UI控件状态
     * */
    private void refreshControls() {
        try {
            findViewById(R.id.textViewUVCPromptL).setVisibility(mUVCCameraL != null ? View.GONE : View.VISIBLE);
            invalidateOptionsMenu();
        } catch (Exception e){}
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
                    //Log.d(TAG, "*******releaseCameraL mUVCCameraL=null");
                    mUVCCameraL = null;
                }
            }
            if (mLeftPreviewSurface != null) {
                mLeftPreviewSurface.release();
                mLeftPreviewSurface = null;
            }
//			try {
//
//			if (mLeftPreviewSurface != null) {
//				mLeftPreviewSurface.release();
//				mLeftPreviewSurface = null;
//			}
//				}
//			 catch (final Exception e) {
//					Log.e(TAG, e.getMessage());
//			}finally{
//				mLeftPreviewSurface = null;
//				//Log.d(TAG, "*******releaseCameraL mLeftPreviewSurface=null");
//			}
        }
    }

    // 实现快照抓取
    private synchronized String captureSnapshot() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd.HH.mm.ss.SSSS");
        Date currentTime = new Date();

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

    }


    //判断是哪个摄像头在使用
    Boolean isCameraL =false;
    Boolean isCameraR =false;
    private final USBMonitor.OnDeviceConnectListener mOnDeviceConnectListener = new USBMonitor.OnDeviceConnectListener() {
        @Override
        public void onAttach(final UsbDevice device) {
            if (DEBUG) Log.i(TAG, "onAttach:" + device);
            final List<UsbDevice> list = mUSBMonitor.getDeviceList();
            mUSBMonitor.requestPermission(list.get(0));

            if(list.size() > 1)
                new Handler().postDelayed(new Runnable() {public void run() {mUSBMonitor.requestPermission(list.get(1));}}, 200);
        }

        @Override
        public void onConnect(final UsbDevice device, final USBMonitor.UsbControlBlock ctrlBlock, final boolean createNew) {

            if (DEBUG) Log.i(TAG, "onConnect:"+ctrlBlock.getVenderId());

            if(!NettyConf.camerastate) {
                NettyConf.camerastate = true;
//                Speaking.in("摄像头已开启");
                //如果教练登录时间不是同一天则自动教练跟学员登出
                if(isFirstStartApp==false){
                    //只执行一次
                    compelobligeOut();

                    isFirstStartApp=true;
                }
            }
            if(loading!=null){
                loading.cancel();

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
                    if (DEBUG) Log.i(TAG, "MJPEG Failed");
                    try {
                        camera.setPreviewSize(PREVIEW_WIDTH, PREVIEW_HEIGHT, UVCCamera.DEFAULT_PREVIEW_MODE, 0.5f);
                    } catch (final IllegalArgumentException e2) {
                        try {
                            currentWidth = UVCCamera.DEFAULT_PREVIEW_WIDTH;
                            currentHeight = UVCCamera.DEFAULT_PREVIEW_HEIGHT;
                            camera.setPreviewSize(PREVIEW_WIDTH, PREVIEW_HEIGHT, UVCCamera.DEFAULT_PREVIEW_MODE, 0.5f);
                        } catch (final IllegalArgumentException e3) {
                            camera.destroy();

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    //Toast.makeText(ShowCameraViewActivity.this, "UVC设备错误", Toast.LENGTH_LONG).show();
                                }
                            });

                            return;
                        }
                    }
                }

                // 将摄像头进行分配
                if(ctrlBlock.getVenderId() == 2 ||ctrlBlock.getVenderId() == 3 && mUVCCameraL == null) {
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
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        refreshControls();

                        //      if (mUVCCameraL != null || mUVCCameraR != null)
                        //      startAudio();
                    }
                });
            }
        }

        @Override
        public void onDisconnect(final UsbDevice device, final USBMonitor.UsbControlBlock ctrlBlock) {
            if (DEBUG) Log.i(TAG, "onDisconnect:" + device);
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
            if (DEBUG) Log.i(TAG, "onDettach:" + device);
            if ((mUVCCameraL != null) && mUVCCameraL.getDevice().equals(device)) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        releaseCameraL();
                    }
                });

            }

        }

        @Override
        public void onCancel(final UsbDevice device) {
            if (DEBUG) Log.i(TAG, "onCancel:");
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
                        Helper.fileSavedProcess(LoginActivity.this, snapshotFileNameL);
                    } catch (Exception ex) {
                    } finally {
                        snapshotOutStreamL = null;
                    }
                }
            }
            buffer = null;
        }
    };


    /**
     * 教练登录时间不是同一天强制登出
     * */
    public void compelobligeOut(){
        SharedPreferences coachsp = getSharedPreferences("coach", Context.MODE_PRIVATE);
        String logintime = coachsp.getString("logintime", "");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String format = sdf.format(new Date(System.currentTimeMillis()));
        if(!logintime.equals("")&&!logintime.equals(format)&&NettyConf.jlstate==1){
            //登录时间不是同一天
            final Dialog qzoutDialog = LoadingDialogUtils.createLoadingDialog(context, "正在强制登出，请稍后...");
            qzoutDialog.setCancelable(false);
            qzOutTimer = new Timer();
            qzOutTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    qzoutDialog.cancel();
                }
            },10000);
            new Timer().schedule(new LoginoutTimer(), 100);
            SharedPreferences.Editor edit = coachsp.edit();
            //清除登录时间
            edit.putString("logintime","");
            edit.commit();
        }
    }

    /**
     * 比对照片
     * */
    public boolean compare(String newcameraurl) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (faceResult.size() > 0) {
            faceResult.clear();
        }
        boolean ret = FaceDet.FD_FSDK_FaceDetection(newcameraurl, faceResult);
        Log.e("TAG","获取比对照片轮廓结果=" + ret);
        if(ret==true&&faceResult!=null&&faceResult.size()>0){
            String MatchName = FaceDet.FaceDetectMuti(newcameraurl, NettyConf.thd);
            Log.e("TAG","比对照片名字轮廓结果=" + MatchName);

            if(StringUtils.isNotEmpty(MatchName)){
                return true;
            }else {
                boolean delete = RlsbUtil.delete(newcameraurl);
                //filepath-->图片绝对路径
                getContentResolver().delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, MediaStore.Images.Media.DATA + "=?", new String[]{newcameraurl});
                return false;
            }

//            float score = FaceDet.FaceDetect(newcameraurl);
//            int score1=(int)(score*100);
//            Log.e("TAG","对比结果=" + score1);
//            if(score1>=NettyConf.rlsb_jd){
//                return true;
//            }else {
//                RlsbUtil.delete(newcameraurl);
//                //filepath-->图片绝对路径
//                getContentResolver().delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, MediaStore.Images.Media.DATA + "=?", new String[]{newcameraurl});
//                return false;
//            }
        }else {
            RlsbUtil.delete(newcameraurl);
            //filepath-->图片绝对路径
            getContentResolver().delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, MediaStore.Images.Media.DATA + "=?", new String[]{newcameraurl});
            return false;
        }

    }

    /**
     * 初始化人脸识别摄像头
     * */
    public void initrlCamera(){
        Timer initbdtimer = new Timer();
        TimerTask task=new TimerTask() {
            @Override
            public void run() {
                Log.e("TAG","进行初始化比对");
                landmarks_path =RlsbUtil.getAssetsCacheFile(LoginActivity.this,"face_landmarks_5_cilab.dat");
                facenet_path = RlsbUtil.getAssetsCacheFile(LoginActivity.this,"facenet_cilab.dat");
                train_path =RlsbUtil.getAssetsCacheFile(LoginActivity.this,"complex_training.txt");
                FaceDet.FaceDetInit(landmarks_path, facenet_path, train_path);
                //获取照片特征值文件内容
//                gettxt("complex_training.txt");
            }
        };
        initbdtimer.schedule(task,500);
        RlsbUtil.addtimer(initbdtimer);
    }

    /**
     * 这个是读取data/data/包名/file路径下的文件
     * */
    public void gettxt(String txtname){
        try {
            // 获取文件
            FileInputStream fin = openFileInput(txtname);
            // 获得长度
            int length = fin.available();
            // 创建字节数组
            byte[] buffer = new byte[length];
            // 读取内容
            fin.read(buffer);
            // 获得编码格式
            String type = RlsbUtil.codetype(buffer);
            // 按编码格式获得内容
            String txt = EncodingUtils.getString(buffer, type);
            Log.e("TAG","train_path:"+txt);

        }
        catch(Exception e) {

        }
    }

    /**
     * 登录方式dialog
     * */
    private void logintypeDialog(final int whologin){
        final AlertDialog builder = new AlertDialog.Builder(this,R.style.CustomDialog).create(); // 先得到构造器
        builder.show();
        builder.getWindow().setContentView(R.layout.dialog_login_type);
        builder.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);//解决不能弹出键盘
        LayoutInflater factory = LayoutInflater.from(this);
        View view = factory.inflate(R.layout.dialog_login_type, null);
        builder.getWindow().setContentView(view);
        View login_face = view.findViewById(R.id.login_face_layout);
        View login_saoma = view.findViewById(R.id.login_saoma_layout);
        View login_phone = view.findViewById(R.id.login_phone_layout);
        View login_weixin = view.findViewById(R.id.login_weixin_layout);
        final Intent intent = new Intent();
        //人脸识别登录
        login_face.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               if(whologin==1){
                   //教练员
                   intent.setClass(context, LoginCoachActivity.class);
                   startActivityForResult(intent, REQUEST_COACH);
                   coachsp.edit().putInt("logintype",1).commit();
                   NettyConf.logintype_coach=1;
               }else {
                   //学员
                   intent.setClass(context, LoginStudentActivity.class);
                   startActivityForResult(intent, REQUEST_STUDENT);
                   stusp.edit().putInt("logintype",1).commit();
                   NettyConf.logintype_stu=1;
               }
                builder.cancel();
            }
        });
        //扫码登录
        login_saoma.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(whologin==1){
                    //教练员
                    coachsp.edit().putInt("logintype",2).commit();
                    NettyConf.logintype_coach=2;
                }else {
                    //学员
                    stusp.edit().putInt("logintype",2).commit();
                    NettyConf.logintype_stu=2;
                }
                intent.setClass(LoginActivity.this, LoginSmActivity.class);
                intent.putExtra("whologin",whologin);
                startActivity(intent);
                builder.cancel();
            }
        });

        //手机登录
        login_phone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(whologin==1){
                    //教练员
                    coachsp.edit().putInt("logintype",3).commit();
                    NettyConf.logintype_coach=3;
                }else {
                    //学员
                    stusp.edit().putInt("logintype",3).commit();
                    NettyConf.logintype_stu=3;
                }
                intent.setClass(context,LoginPhoneActivity.class);
                intent.putExtra("whologin",whologin);
                startActivity(intent);
                builder.cancel();
            }
        });

        login_weixin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(whologin==1){
                    //教练员
                    coachsp.edit().putInt("logintype",4).commit();
                    NettyConf.logintype_coach=4;
                }else {
                    //学员
                    stusp.edit().putInt("logintype",4).commit();
                    NettyConf.logintype_stu=4;
                }

                intent.setClass(LoginActivity.this, LoginSmActivity.class);
                intent.putExtra("whologin",whologin);
                startActivity(intent);
                builder.cancel();
            }
        });
    }

    @Override
    protected void onStop() {
        if(NettyConf.debug) {
            Log.e("TAG", "onStop:");
        }
//        mUSBMonitor.unregister();

        //关闭摄像头
        if(mUSBMonitor.isRegistered()){
            //注册了
            releaseCameraL();
            mUSBMonitor.unregister();
        }
        super.onStop();
    }

    @Override
    public void onDestroy() {
        Log.e("TAG", "onDestroy:");
        //stopAudio();

        if (mUSBMonitor != null) {
            mUSBMonitor.destroy();
            mUSBMonitor = null;
        }

        if(qzOutTimer!=null){
            qzOutTimer.cancel();
        }

        //解绑广播
        if(receiver!=null){
            unregisterReceiver(receiver);
        }
        if(networkReceiver!=null){
            unregisterReceiver(networkReceiver);
        }
        if(trainReceiver!=null) {
            this.unregisterReceiver(trainReceiver);
        }
        if(loading!=null){
            loading.cancel();
        }

        if(Speakout.tts!=null){
            Speakout.tts.stop();
            Speakout.tts.shutdown();
            Speakout.tts=null;
        }

        if(b){
            unbindService(mServiceConnection);
        }

        //删除人脸识别初始化
        if(FaceDet!=null){
            FaceDet.FaceDetDeInit();
        }

        super.onDestroy();
    }

    public void readSIM(){
        TelephonyManager telMgr = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);

        Log.e("TAG", "SIM卡的序列号:" + telMgr.getSimSerialNumber());
    }

}
