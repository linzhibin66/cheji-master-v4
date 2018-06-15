package com.dgcheshang.cheji.Activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.usb.UsbDevice;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.chainway.facedet.FD_FSDKFace;
import com.chainway.facedet.FaceDetJni;
import com.dgcheshang.cheji.Database.DbConstants;
import com.dgcheshang.cheji.Database.DbHandle;
import com.dgcheshang.cheji.Database.MyDatabase;
import com.dgcheshang.cheji.R;
import com.dgcheshang.cheji.Tools.Helper;
import com.dgcheshang.cheji.Tools.LoadingDialogUtils;
import com.dgcheshang.cheji.Tools.Speaking;
import com.dgcheshang.cheji.netty.conf.NettyConf;
import com.dgcheshang.cheji.netty.po.Jlydl;
import com.dgcheshang.cheji.netty.po.Tdata;
import com.dgcheshang.cheji.netty.serverreply.JlydcR;
import com.dgcheshang.cheji.netty.serverreply.JlydlR;
import com.dgcheshang.cheji.netty.serverreply.SfrzR;
import com.dgcheshang.cheji.netty.timer.CardTimer;
import com.dgcheshang.cheji.netty.timer.FrinterTimer;
import com.dgcheshang.cheji.netty.timer.LoadingTimer;
import com.dgcheshang.cheji.netty.tools.fingerprint.BaseInitTask;
import com.dgcheshang.cheji.netty.util.ByteUtil;
import com.dgcheshang.cheji.netty.util.ForwardUtil;
import com.dgcheshang.cheji.netty.util.MsgUtilClient;
import com.dgcheshang.cheji.netty.util.RlsbUtil;
import com.dgcheshang.cheji.netty.util.ZdUtil;
import com.rscja.deviceapi.Fingerprint;
import com.rscja.deviceapi.Fingerprint.BufferEnum;
import com.rscja.deviceapi.RFIDWithISO14443A;
import com.rscja.deviceapi.exception.ConfigurationException;
import com.serenegiant.usb.CameraDialog;
import com.serenegiant.usb.DeviceFilter;
import com.serenegiant.usb.IFrameCallback;
import com.serenegiant.usb.Size;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.UVCCamera;
import com.serenegiant.widget.GreenRect;
import com.serenegiant.widget.UVCCameraTextureView;
import com.shenyaocn.android.Encoder.CameraRecorder;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import sun.misc.BASE64Encoder;

/**
 * 教练员登录
 * */
public class LoginCoachActivity extends BaseInitActivity implements View.OnClickListener,CameraDialog.CameraDialogParent {
    public String TAG="LoginCoachActivity";
    Context context=LoginCoachActivity.this;
    public static final int LOGIN_COA_SUCCESS = 0;

    public Fingerprint mFingerprint;
    private BaseInitTask mBaseInitTask;

    ImageView image_shuaka,image_zhiwen,image_shenfen;
    TextView tv_shenfen,tv_jlbh,tv_chexin,tv_coach_name;
    ImageView image_coach;
    View layout_shenfen;
    TextView tv_zhiwen;
    private TextView tv_title;
    RFIDWithISO14443A mRFID;
    SharedPreferences coachsp;
    SharedPreferences.Editor editor;
    Dialog loading;
    Dialog outloalDialog;
    LoadingTimer loadingTimer;
    Timer timer;
    SfrzR jlxx;
    String yzmm;
    private final Object mSync = new Object();
    View layout_showphoto;//拍照框
    String fileurl="/sdcard/jlypic/";//下载教练员图片文件夹路径
    BroadcastReceiver receiver;//下载广播

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
    private static final boolean DEBUG = true;
    private CameraRecorder mp4RecorderL=new CameraRecorder(1);
    private CameraRecorder mp4RecorderR=new CameraRecorder(2);
    private int currentWidth = UVCCamera.DEFAULT_PREVIEW_WIDTH;
    private int currentHeight = UVCCamera.DEFAULT_PREVIEW_HEIGHT;
    public String landmarks_path, facenet_path, train_path;
    GreenRect leftRect;
    ArrayList<Rect> rectArrayList = null;
    boolean isback=true;//是否可按返回键
//    List<FD_FSDKFace> faceResult = new ArrayList<>();
    Handler handler=new  Handler(){
        @Override
        public void handleMessage(Message msg) {
            if(msg.arg1==1){//教练登录
                image_shenfen.setBackgroundResource(R.mipmap.login_ok_y);
                handleIn(msg);
            }else if(msg.arg1==2){//教练员登出
                Bundle data = msg.getData();
                JlydcR jldcr = (JlydcR) data.getSerializable("jlydcr");//教练登录成功后返回来的数据
                handleOut(jldcr);
            }else if(msg.arg1==5){//获取教练信息
                Bundle data = msg.getData();
                jlxx = (SfrzR) data.getSerializable("jlxx");
//                jlxx.setXx("http://192.168.0.111:8092/lin.jpg");
                getJlxx(jlxx);
            }else if(msg.arg1==6){//uid
                String jluid = msg.getData().getString("jluid");
                image_shuaka.setBackgroundResource(R.mipmap.login_rid_jlcard_y);

                String sql="select * from tsfrz where uuid=? and lx=?";
                //判断是否使用指纹识别还是人脸识别,1指纹识别 4人脸识别
                String[] params={jluid,"1"+NettyConf.sbtype};
                ArrayList<SfrzR> list= DbHandle.queryTsfrz(sql,params);
                if(list.size()==0){
                    if(ZdUtil.pdNetwork()&&NettyConf.constate==1) {
                        ZdUtil.sendSfrz(jluid,NettyConf.sbtype, "1");
                    }else {
                        Speaking.in("请连接服务器");
                    }
                }else{
                    jlxx=list.get(0);
                    getJlxx(jlxx);
                }
            }else if(msg.arg1==7){
                //指纹验证成功返回登录
                image_zhiwen.setBackgroundResource(R.mipmap.login_fingerprint_y);
                loading = LoadingDialogUtils.createLoadingDialog(context, "正在登录...");
                coachLogin();
            }else if(msg.arg1==8){
                coachOut2();
            }else if(msg.arg1==9){
                //指纹匹配成功登出
                coachOut1();
            }else if (msg.arg1==10){
                //强制登出返回回来处理
                int yzjg = msg.getData().getInt("yzjg");
                if(yzjg==0){
                    editor.putString("yzmm",yzmm);//保存验证密码
                    editor.commit();
                    if(!ZdUtil.ispz){
                        if (NettyConf.xystate==1){
                            Toast.makeText(context,"请先登出学员！",Toast.LENGTH_SHORT).show();
                        }else {
                            qzCoachOut();
                        }
                    }else {
                        Toast.makeText(context,",正在拍照请稍后操作",Toast.LENGTH_SHORT).show();
                    }
                }else {
                    loading.cancel();
                    Speaking.in("密码验证失败");
                }
            }else if(msg.arg1==15){
                //比对成功后关闭摄像头预览框
                layout_showphoto.setVisibility(View.INVISIBLE);
                image_zhiwen.setBackgroundResource(R.mipmap.login_face_y);//显示人脸识别成功
                loading = LoadingDialogUtils.createLoadingDialog(context,"处理中...");

            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_coach);
        NettyConf.handlersmap.put("logincoach",handler);
        initView();

        if(NettyConf.fringerTimer!=null){
            NettyConf.fringerTimer.cancel();
            NettyConf.fringerTimer=null;
        }

        if(NettyConf.cardtimer!=null){
            NettyConf.cardtimer.cancel();
            NettyConf.cardtimer=null;
        }

        CardTimer.isstop = false;
        FrinterTimer.ispp=false;

        if(NettyConf.jlstate!=1) {
            try {
                mRFID = RFIDWithISO14443A.getInstance();
            } catch (Exception e) {
            }
        }
    }

    /**
     *
     * 初始化布局
     * */
    private void initView() {
        //保存教练信息
        coachsp = getSharedPreferences("coach", Context.MODE_PRIVATE); //私有数据
        editor = coachsp.edit();//获取编辑器
        View layout_back = findViewById(R.id.layout_back);//返回
        tv_title = (TextView) findViewById(R.id.tv_title);//标题
        //登录页面布局
        View layout_coachin = findViewById(R.id.layout_coachin);//登录布局
        layout_shenfen = findViewById(R.id.layout_shenfen);//身份信息布局
        layout_shenfen.setVisibility(View.INVISIBLE);
        image_shuaka = (ImageView) findViewById(R.id.image_shuaka);//刷卡图片
        image_zhiwen = (ImageView) findViewById(R.id.image_zhiwen);//指纹图片
        tv_zhiwen = (TextView) findViewById(R.id.tv_fingerprint);//指纹识别
        image_shenfen = (ImageView) findViewById(R.id.image_shenfen);//身份验证图片
        tv_shenfen = (TextView) findViewById(R.id.tv_shenfen);//身份证号
        tv_jlbh = (TextView) findViewById(R.id.tv_jlbh);//教练编号
        tv_coach_name = (TextView) findViewById(R.id.tv_coach_name);//教练姓名
        tv_chexin = (TextView) findViewById(R.id.tv_chexin);//车型

        //登出页面布局
        View layout_coachout = findViewById(R.id.layout_coachout);//登出布局
        Button bt_coachout = (Button) findViewById(R.id.bt_coachout);//登出按钮
        TextView tv_coachcode = (TextView) findViewById(R.id.tv_coachcode);//教练编号
        TextView tv_coachzj = (TextView) findViewById(R.id.tv_coachzj);//证件号码
        TextView tv_cartype = (TextView) findViewById(R.id.tv_cartype);//车牌类型
        TextView tv_coachname = (TextView) findViewById(R.id.tv_coachname);//教练姓名
        image_coach = (ImageView) findViewById(R.id.image_coach);//教练证件照
        View layout_qzout = findViewById(R.id.layout_qzout);//强制登出
        //判断是否教练登录过
        if(NettyConf.jlstate==1){//登录过
            layout_coachin.setVisibility(View.GONE);
            layout_coachout.setVisibility(View.VISIBLE);
            layout_qzout.setVisibility(View.VISIBLE);
            tv_title.setText("教练员管理");
            showCoachPhoto();
            tv_coachcode.setText(NettyConf.jbh);
            tv_cartype.setText(NettyConf.cx);
            tv_coachzj.setText(NettyConf.jzjhm);
            String jlxm = coachsp.getString("jlxm", "");
            tv_coachname.setText(jlxm);

        }else {//没登录
            layout_coachin.setVisibility(View.VISIBLE);
            layout_coachout.setVisibility(View.GONE);
            layout_qzout.setVisibility(View.GONE);
        }

        if(NettyConf.sbtype.equals("4")){
            //人脸模块
            image_zhiwen.setBackgroundResource(R.mipmap.login_face_n);//指纹识别改成显示人脸识别图像
            tv_zhiwen.setText("人脸识别");
        }
        layout_showphoto = findViewById(R.id.layout_showphoto);//显示拍照框
        layout_showphoto.setVisibility(View.INVISIBLE);
        //摄像头
        mUVCCameraViewL = (UVCCameraTextureView)findViewById(R.id.camera_view_L);
        mUVCCameraViewL.setAspectRatio(PREVIEW_WIDTH / (float)PREVIEW_HEIGHT);
        refreshControls();

        rectArrayList = new ArrayList<>();
        layout_back.setOnClickListener(this);
        bt_coachout.setOnClickListener(this);
        layout_qzout.setOnClickListener(this);
    }

    /**
     * 点击监听
     * */
    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.layout_back://返回
                finish();
                break;

            case R.id.bt_coachout://登出
                if(!ZdUtil.ispz){
                    if (NettyConf.xystate==1){
                        Toast.makeText(context,"请先登出学员！",Toast.LENGTH_SHORT).show();
                    }else {
                        if(NettyConf.sbtype.equals("1")){
                            //指纹识别
                            loading = LoadingDialogUtils.createLoadingDialog(context, "教练登出中(请验证指纹)...");
                        }
                        coachOut();
                    }
                }else {
                    Toast.makeText(context,",正在拍照请稍后操作",Toast.LENGTH_SHORT).show();
                }
                break;

            case R.id.layout_qzout://强制退出
                showliuyanDialog();
                break;
        }
    }



    /**
     * 教练登录
     * */
    private void coachLogin() {
        try {
            if (ZdUtil.pdGps()) {
                String gnss = ZdUtil.getGnss();
                Jlydl jlydl = new Jlydl();
                jlydl.setJlybh(NettyConf.jbh);//教练员编号
                jlydl.setJlyzjhm(NettyConf.jzjhm);
                jlydl.setZjcx(NettyConf.cx);//车型
                jlydl.setGnss(gnss);
                byte[] b3 = jlydl.getJlydlbytes();
                byte[] b2 = MsgUtilClient.getMsgExtend(b3, "0101", "13", "2");
                List<Tdata> list = MsgUtilClient.generateMsg(b2, "0900", NettyConf.mobile, "1");

                if(ZdUtil.pdNetwork()&&NettyConf.constate==1&&NettyConf.jqstate==1) {
                    if(NettyConf.debug){
                        Log.e("TAG"+TAG,"发送教练数据");
                    }
                    ForwardUtil.sendData(list, 0,1);
                }else{
                    if(NettyConf.debug){
                        Log.e("TAG"+TAG,"缓存教练数据");
                    }
                    DbHandle.insertTdatas(list,1);

                    Message msg=new Message();
                    JlydlR jr=new JlydlR();
                    jr.setJg(1);
                    Bundle bundle = new Bundle();
                    bundle.putSerializable("jldlr",jr);
                    msg.setData(bundle);
                    handleIn(msg);
                }
            } else {
                Toast.makeText(context, "gps数据获取失败", Toast.LENGTH_SHORT).show();
            }
        }catch(Exception e){
            Toast.makeText(context,"教练员登陆数据异常",Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 教练登出
     * */
    private void coachOut() {
        String sql="select * from tsfrz where tybh=? and lx=?";
        String[] params={NettyConf.jbh,"1"+NettyConf.sbtype};
        ArrayList<SfrzR> list= DbHandle.queryTsfrz(sql,params);
        if(list.size()==0){
            coachOut1();
        }else{
            SfrzR jlxx=list.get(0);
            getJlyxxOut(jlxx);
        }
        //coachOut1();
    }

    /**
     * 教练登出
     * */
    private void coachOut1() {
        //loading = LoadingDialogUtils.createLoadingDialog(context, "正在登出...");
        loadingTimer = new LoadingTimer(loading);
        timer = new Timer();
        timer.schedule(loadingTimer, NettyConf.controltime);
        if(NettyConf.sbtype.equals("1")){
            //指纹识别
            ZdUtil.coachOut1();
        }else {
            //人脸识别
            try {
                if (ZdUtil.pdGps()) {
                    ZdUtil.sendZpsc2("129", "0", "21",ZdUtil.getGnss4(),bdpic);
                } else {
                    Speaking.in("定位数据获取失败");
                }
            }catch(Exception e){
                Speaking.in("教练员登出数据异常");
            }
        }

    }

    /**
     * 教练强制登出通道
     * */
    private void qzCoachOut() {
        //loading = LoadingDialogUtils.createLoadingDialog(context, "正在登出...");
        loadingTimer = new LoadingTimer(loading);
        timer = new Timer();
        timer.schedule(loadingTimer, NettyConf.controltime);
            ZdUtil.qzCoachOut();
    }


    private void coachOut2() {
        try {
            if (ZdUtil.pdGps()) {
                List<Tdata> list=ZdUtil.coachOut2();

                if(ZdUtil.pdNetwork()&&NettyConf.constate==1&&NettyConf.jqstate==1){
                    ForwardUtil.sendData(list, 1,7);
                }else{
                    DbHandle.insertTdatas(list,7);
                    JlydcR jr=new JlydcR();
                    jr.setJg(1);
                    handleOut(jr);
                }
            } else {
                Toast.makeText(context,"gps数据获取失败",Toast.LENGTH_SHORT).show();
            }
        }catch(Exception e){
            Toast.makeText(context,"教练员登出数据异常",Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 登录处理
     * */
    public synchronized void handleIn(Message msg){
        if(loadingTimer!=null) {
            loadingTimer.cancel();
        }
        if(timer!=null) {
            timer.cancel();
        }
        if(NettyConf.jlstate!=1) {
            Bundle data = msg.getData();
            JlydlR jldlr = (JlydlR) data.getSerializable("jldlr");//教练登录成功后返回来的数据

            if (jldlr.getJg() == 1) {//教练登录成功

                //存入缓存
                String temp=(jlxx.getLx()+"").substring(0,1)+NettyConf.sbtype;
                jlxx.setLx(Byte.valueOf(temp));
                DbHandle.insertTsfrz(jlxx);
                editor.putString("jlbh", NettyConf.jbh);//教练编号
                editor.putInt("jlstate", 1);
                editor.putString("cx", NettyConf.cx);//教练车型
                editor.putString("jzjhm", NettyConf.jzjhm);//证件号码
                editor.putString("jlxm", jlxx.getXm());//教练姓名
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");//保存年月日
                editor.putString("logintime",sdf.format(new Date(System.currentTimeMillis())));
                editor.commit();//提交修改
                NettyConf.jlstate = 1;
                //上传拍照数据
                if(NettyConf.sbtype.equals("1")){
                    //指纹识别进入loginactivity页面拍照
                    ZdUtil.sendZpsc("129", "0", "20");
                }else {
                    //人脸识别上传对比成功的照片
                    ZdUtil.sendZpsc2("129","0","20",ZdUtil.getGnss4(),bdpic);
                }
                Intent intent = new Intent();
                setResult(LOGIN_COA_SUCCESS, intent);

                LoadingDialogUtils.closeDialog(loading);
                Speaking.in("教练登陆成功");
            } else {
                //删除缓存
                String[] params = {jlxx.getUuid(), String.valueOf(jlxx.getLx())};
                DbHandle.deleteData(DbConstants.T_SFRZ, "uuid=? and lx=?", params);
                //取消模态
                LoadingDialogUtils.closeDialog(loading);
                //报读原因
                if(StringUtils.isNotEmpty(jldlr.getFjxx())) {
                    Speaking.in(jldlr.getFjxx());
                }
            }
            finish();
        }
    }

    /**
     * 登出处理
     * */
    public synchronized void handleOut(JlydcR jldcr){
        if(loadingTimer!=null){
            loadingTimer.cancel();
        }
        if(timer!=null){
            timer.cancel();
        }

        if(NettyConf.jlstate==1) {
            if (jldcr.getJg() == 1) {
                NettyConf.jlstate = 0;
                editor.putInt("jlstate", 0);
                editor.commit();

                Intent intent = new Intent();
                setResult(LOGIN_COA_SUCCESS, intent);
                editor.putString("logintime","");//清除登录时间
                LoadingDialogUtils.closeDialog(loading);
                Speaking.in("教练员登出成功");
                finish();
            } else {
                Speaking.in("教练员登出失败");
            }
        }
    }

    /**
     * 读卡成功后获取教练信息
     * */
    public void getJlxx(final SfrzR jlxx){
        String xx = jlxx.getXx();//教练指纹
        NettyConf.cx = jlxx.getCx();//车型
        NettyConf.jbh = jlxx.getTybh();//统一编号
        NettyConf.jzjhm = jlxx.getSfzh();//身份证号
        tv_shenfen.setText(jlxx.getSfzh());
        tv_chexin.setText(jlxx.getCx());
        tv_jlbh.setText(jlxx.getTybh());
        tv_coach_name.setText(jlxx.getXm());
        //获取信息成功后显示身份信息
        layout_shenfen.setVisibility(View.VISIBLE);
        if(StringUtils.isNotEmpty(xx)) {
            mRFID.free();
            if(NettyConf.cardtimer!=null){
                NettyConf.cardtimer.cancel();
                NettyConf.cardtimer=null;
            }

            //判断选择指纹识别还是人脸识别
            if(NettyConf.sbtype.equals("1")){
                //指纹识别
                commonCoach(xx,"jlcard");
            }else {
                //人脸识别
                if(ZdUtil.ispz==false){
                    commonCoach2(jlxx,"login");
                }else {
                    Toast.makeText(context,"正在拍照，请稍后...",Toast.LENGTH_SHORT).show();
                }

            }

        }else{
            //验证成功返回登录
            image_zhiwen.setBackgroundResource(R.mipmap.login_fingerprint_y);
            loading = LoadingDialogUtils.createLoadingDialog(context, "正在登录...");
            coachLogin();
        }
    }

    /**
     * 教练登出
     */
    public void getJlyxxOut(final SfrzR jlxx){
        String xx = jlxx.getXx();//教练指纹
        if(StringUtils.isNotEmpty(xx)){
            if(NettyConf.sbtype.equals("1")){
                //指纹识别
                commonCoach(xx,"jlcardout");
            }else {
                //人脸识别
                if(ZdUtil.ispz==false){
                    commonCoach2(jlxx,"out");
                }else {
                    Toast.makeText(context,"正在拍照，请稍后...",Toast.LENGTH_SHORT).show();
                }

            }

        }else{
            qzCoachOut();
        }
    }

    /**
     * 指纹识别通道
     * */
    public void commonCoach(String xx,String type){
        try {
            mFingerprint = Fingerprint.getInstance();
            initFingerprint(-1);

            Thread.sleep(1000);
        } catch (Exception e) {
            e.printStackTrace();
        }
            //有指纹模块
            mFingerprint.setReg(5, 1);
            mFingerprint.empty();

            String[] ss=new String[2];
            if(xx.length()>1024){
                ss[0]=xx.substring(0,1024);
                ss[1]=xx.substring(1024,xx.length());
            }else{
                ss[0]=xx;
            }

            mFingerprint.downChar(BufferEnum.B2, ss[0]);
            Speaking.in("请验证指纹");
            try {
                FrinterTimer frinterTimer = new FrinterTimer(mFingerprint,type);
                NettyConf.fringerTimer = new Timer();
                NettyConf.fringerTimer.schedule(frinterTimer, 20, 3000);
            } catch (Exception ex) {
                Toast.makeText(context, ex.getMessage(), Toast.LENGTH_SHORT).show();
            }

    }

    /**
     * 人脸识别通道
     * type 分为login和out
     * */
    Timer rlsbtimer;
    public void commonCoach2(final SfrzR jlxx,final String type){
        ZdUtil.ispz=true;
        isback=false;
        initrlCamera();
        rlsbtimer = new Timer();
        TimerTask task=new TimerTask() {
            @Override
            public void run() {
                String xx = jlxx.getXx();
                xx=new String(ByteUtil.hexStringToByte(xx));
                Log.e("TAG","教练下载图片路径："+xx);
                String sfzh = jlxx.getSfzh();
                //判断文件夹是否存在
                RlsbUtil.isexistAndBuild(fileurl);
                //教练原始照片路径
                String jlzp=fileurl+sfzh+".jpg";
                if(RlsbUtil.isFileExist(jlzp)==false){
                    //没有教练照片去下载
                    downFile(xx,sfzh,jlzp,type);
                }else {
                    //有教练照片直接抓拍验证
                    rlsb(jlzp,sfzh, type);
                }
            }
        };
        rlsbtimer.schedule(task,2000);
    }


/**
 * 初始化指纹识别
 * */
    public void initFingerprint(final int baudrate) {
        mBaseInitTask = new BaseInitTask(this) {

            @Override
            protected Boolean doInBackground(String... params) {

                boolean result = false;

                if (mFingerprint != null) {

                    if(baudrate==-1) {
                        result = mFingerprint.init();
                        Log.e("TAG","指纹初始化co:"+result);

                    } else {
                        result = mFingerprint.init(baudrate);
                    }
                }

                return result;
            }

        };
        mBaseInitTask.execute();
    }

    /**
     * 初始化人脸识别摄像头
     * */
    FaceDetJni FaceDet;
    List<FD_FSDKFace> faceResult;
    public void initrlCamera(){
        faceResult = new ArrayList<>();
        FaceDet = new FaceDetJni();

        mUSBMonitor = new USBMonitor(this, mOnDeviceConnectListener);
        final List<DeviceFilter> filters = DeviceFilter.getDeviceFilters(this, R.xml.device_filter);
        mUSBMonitor.setDeviceFilter(filters);
        mUSBMonitor.register();//start
        refreshControls();
        showCamera();
        Speaking.in("正在人脸识别，请对准摄像头");
        Timer initbdtimer = new Timer();
        TimerTask task=new TimerTask() {
            @Override
            public void run() {
                Log.e("TAG","进行初始化比对");
                landmarks_path =RlsbUtil.getAssetsCacheFile(LoginCoachActivity.this,"face_landmarks_5_cilab.dat");
                facenet_path = RlsbUtil.getAssetsCacheFile(LoginCoachActivity.this,"facenet_cilab.dat");
                train_path =RlsbUtil.getAssetsCacheFile(LoginCoachActivity.this,"complex_training.txt");
                FaceDet.FaceDetInit(landmarks_path, facenet_path, train_path);
            }
        };
        initbdtimer.schedule(task,2000);
        RlsbUtil.addtimer(initbdtimer);

    }

    /**
     * 强制登出dialog
     *
     * */
    private void showliuyanDialog(){
        final AlertDialog builder = new AlertDialog.Builder(this,R.style.CustomDialog).create(); // 先得到构造器
        builder.show();
        builder.getWindow().setContentView(R.layout.dialog_appoint_edt);
        builder.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);//解决不能弹出键盘
        LayoutInflater factory = LayoutInflater.from(this);
        View view = factory.inflate(R.layout.dialog_appoint_edt, null);
        builder.getWindow().setContentView(view);
        final EditText edt_content = (EditText) view.findViewById(R.id.edt_content);
        TextView tv_title = (TextView) view.findViewById(R.id.tv_title);
        Button bt_cacnel = (Button) view.findViewById(R.id.bt_cacnel);
        Button bt_sure = (Button) view.findViewById(R.id.bt_sure);
        tv_title.setText("登出验证");

        //取消
        bt_cacnel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                builder.dismiss();
            }
        });

        //确定
        bt_sure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                 yzmm = edt_content.getText().toString().trim();
                if(!yzmm.equals("")){
                    builder.dismiss();
                    loading = LoadingDialogUtils.createLoadingDialog(context, "正在登出...");
                    ZdUtil.matchPassword(1,yzmm);

                }else {
                    Toast.makeText(context,"请输入登出密码",Toast.LENGTH_SHORT).show();
                }
            }
        });
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

                // 将摄像头进行分配 全部显示在左摄像头上
                if(ctrlBlock.getVenderId() == 2 ||ctrlBlock.getVenderId() == 3&& mUVCCameraL == null) {
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
                        Helper.fileSavedProcess(LoginCoachActivity.this, snapshotFileNameL);
                    } catch (Exception ex) {
                    } finally {
                        snapshotOutStreamL = null;
                    }
                }
            }
            buffer = null;
        }
    };


    // 刷新UI控件状态
    private void refreshControls() {
        try {
            findViewById(R.id.textViewUVCPromptL).setVisibility(mUVCCameraL != null ? View.GONE : View.VISIBLE);
            invalidateOptionsMenu();
        } catch (Exception e){}
    }


    /**
     * 判断文件是否存在并保存比对原照片
     * */
    public boolean isfiled(String mageurl,String name){
        File file = new File(mageurl);
        if (file.exists()&&file.length() > 0) {
            boolean ret = FaceDet.FD_FSDK_FaceDetection(mageurl, faceResult);
            Log.e("TAG","获取原始照片轮廓结果=" + ret);
            if(ret==true){
                //成功获取轮廓并保存
                boolean mIsSave = FaceDet.CaptureFaceMuti(mageurl, name);
                Log.e("TAG","保存原始照片结果=" + ret);

                return true;
            }else {
                return false;
            }
        }else {
            return false;
        }
    }

    /**
     * 下载文件
     * */
    public void downFile(String url, final String sfzh, final String jlzp, final String type){

        //下载文件
        final DownloadManager dManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        Uri uri = Uri.parse(url);
        DownloadManager.Request request = new DownloadManager.Request(uri);
        // 设置下载路径和文件名
        request.setDestinationInExternalPublicDir("jlypic", sfzh+".jpg");
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
                //下载状态查询
//                DownloadManager.Query query = new DownloadManager.Query().setFilterById(refernece);
//                Cursor c = dManager.query(query);if (c != null && c.moveToFirst()) {
//                    int status = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
//                    switch (status) {
//                        case DownloadManager.STATUS_PENDING:
//                            break;
//                        case DownloadManager.STATUS_PAUSED:
//                            break;
//                        case DownloadManager.STATUS_RUNNING:
//                            break;
//                        case DownloadManager.STATUS_SUCCESSFUL:
//                            Log.e("TAG","下载成功");
//                            //下载完成操作，保存原照片 身份证号用来区别
//                            rlsb(jlzp, sfzh,type);
//                            break;
//                        case DownloadManager.STATUS_FAILED:
//                            Log.e("TAG","下载失败");
//                            Speaking.in("照片下载失败");
//                            break;
//                    }
//                    if (c != null) {
//                        c.close();
//                    }
//                }

                if (refernece == myDwonloadID) {
                    //下载完成操作，保存原照片 身份证号用来区别
                    Log.e("TAG","下载教练照片成功");
                    rlsb(jlzp, sfzh,type);

                }else {
                    Log.e("TAG","照片下载失败");
                    Speaking.in("照片下载失败");
                }
            }
        };
        registerReceiver(receiver, filter);
    }

    /**
     * 人脸识别成功后处理教练登录或教练登出
     * */
    Timer pztimer;
    TimerTask pztask;
    int isfinishphoto = 0;//60秒停止比对
    public void rlsb(final String jlzp, String sfzh, final String type){
        try {
            Thread.sleep(800);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //保存教练原始照片
        boolean save_ok=isfiled(jlzp, sfzh);
        isback=true;
        //只有当原始照片保存成功才进行人脸识别
        if(save_ok==true&&isCameraL==true){
            pztimer = new Timer();
            pztask=new TimerTask() {
                @Override
                public void run() {
                    if(isfinishphoto<20){
                        isfinishphoto++;
                        String path = captureSnapshot();
                        boolean stopcamera=compare(path);
                        if(stopcamera==true){
                            //关闭摄像头
                            pztimer.cancel();
                            closeCamera();
                            ZdUtil.ispz=false;
                            final Timer rlcltimer = new Timer();
                            TimerTask task=new TimerTask() {
                                @Override
                                public void run() {
                                    //识别正确人脸
                                    if(type.equals("login")){
                                        //登录处理
                                        rlcltimer.cancel();
                                        //保存教练证件照
                                        editor.putString("coachphoto",jlzp);
                                        editor.commit();
                                        coachLogin();
                                    }else if(type.equals("out")){
                                        //登出处理
                                        rlcltimer.cancel();
                                        coachOut1();
                                    }
                                }
                            };
                            rlcltimer.schedule(task,200);
                            RlsbUtil.addtimer(rlcltimer);
                        }
                    }else {
                        //如果超过60秒则自动关闭页面
                        pztimer.cancel();
                        ZdUtil.ispz=false;
                        closeCamera();
                        finish();
                    }

                }
            };
            pztimer.schedule(pztask,200,3000);
            RlsbUtil.addtimer(pztimer);

//            while (stopcamera==false){
//
//                String path = captureSnapshot();
//                stopcamera=compare(path);
//                if(stopcamera==true){
//                    //匹配成功就关闭页面
//                    closeCamera();
//                    ZdUtil.ispz=false;
//                final Timer rlcltimer = new Timer();
//                TimerTask task=new TimerTask() {
//                    @Override
//                    public void run() {
//                        //识别正确人脸
//                        if(type.equals("login")){
//                            //登录处理
//                            coachLogin();
////                            rlcltimer.cancel();
//                        }else if(type.equals("out")){
//                            //登出处理
//                            coachOut1();
////                            rlcltimer.cancel();
//                        }
//                    }
//                };
//                rlcltimer.schedule(task,200);
//                RlsbUtil.addtimer(rlcltimer);
//                }
//            }

        }else {
            //保存原始图片失败
            Speaking.in("人脸识别失败");
        }

    }

    /**
     * 比对照片返回结果
     * */
    String bdpic="";//比对成功后的照片
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
        Log.e("TAG","获取抓拍照片轮廓结果=" + ret);
        if(ret==true){
            float score = FaceDet.FaceDetect(newcameraurl);
            int score1=(int)(score*100);
            Log.e("TAG","对比结果=" + score1);
            if(score1>=NettyConf.rlsb_jd){
                bdpic=newcameraurl;
                return true;
            }else {
                boolean delete = RlsbUtil.delete(newcameraurl);
                //filepath-->图片绝对路径
                getContentResolver().delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, MediaStore.Images.Media.DATA + "=?", new String[]{newcameraurl});
                return false;
            }
        }else {
            boolean delete = RlsbUtil.delete(newcameraurl);
            //filepath-->图片绝对路径
            getContentResolver().delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, MediaStore.Images.Media.DATA + "=?", new String[]{newcameraurl});
            return false;
        }
    }

    /**
     * 显示拍照预览框
     * */
    public void showCamera(){
        layout_showphoto.setVisibility(View.VISIBLE);
        if(mUSBMonitor!=null){
            //注册了
            releaseCameraL();
            mUSBMonitor.unregister();
        }
        mUSBMonitor.register();
    }

    /**
     * 关闭拍照预览框
     * */
    public void closeCamera(){
        if(mUSBMonitor.isRegistered()){
            //注册了
            releaseCameraL();
            mUSBMonitor.unregister();
        }
        //发送主线程关闭摄像头预览框
        Message msg = new Message();
        msg.arg1=15;
        handler.sendMessage(msg);
    }

    /***
     * 显示本地教练证件照
     * */
    public void showCoachPhoto(){
        String url=coachsp.getString("coachphoto","");;
        image_coach.setImageURI(Uri.fromFile(new File(url)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e("TAG","onResume");
        if(NettyConf.jlstate!=1){
            mRFID.init();
            CardTimer cardTimer=new CardTimer(mRFID,"jlcard");
            NettyConf.cardtimer=new Timer();
            NettyConf.cardtimer.schedule(cardTimer,300,2000);
            Log.e("TAG","onResume里的报读");
            Speaking.in("教练员请刷卡");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if(pztask!=null){
            pztask.cancel();
            ZdUtil.ispz=false;
        }
        if(pztimer!=null){
            pztimer.cancel();
            ZdUtil.ispz=false;
        }
        if(rlsbtimer!=null){
            rlsbtimer.cancel();
            ZdUtil.ispz=false;
        }
        //关闭USB摄像头
        if (mUSBMonitor != null) {
            releaseCameraL();
            mUSBMonitor.unregister();
//            mUSBMonitor.destroy();
            mUSBMonitor = null;
        }
        //删除人脸识别初始化
        if(FaceDet!=null){
            FaceDet.FaceDetDeInit();
        }
    }

    /**
     * 关闭页面调用
     * */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        //清除计时器
        if(timer!=null){
            timer.cancel();
        }
        if(NettyConf.fringerTimer!=null){
            NettyConf.fringerTimer.cancel();
            NettyConf.fringerTimer=null;
        }
        if(NettyConf.cardtimer!=null){
            NettyConf.cardtimer.cancel();
            NettyConf.cardtimer=null;
        }
        //关闭指纹识别功能
        if(mFingerprint!=null){
            mFingerprint.free();
        }
        //关闭刷卡
        if(mRFID!=null){
            mRFID.free();
        }
        //解绑下载照片广播
        if(receiver!=null){
            unregisterReceiver(receiver);
        }

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
        if(pztimer!=null){
            pztimer.cancel();
            ZdUtil.ispz=false;
        }
        NettyConf.handlersmap.remove("logincoach");

//        RlsbUtil.deltimers();
    }

    @Override
    public USBMonitor getUSBMonitor() {
        return mUSBMonitor;
    }

    @Override
    public void onDialogResult(boolean b) {

    }

    /**
     * 返回键监听
     * */
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if(isback==true){
             finish();
            }
            return false;
        }else {
            return super.onKeyDown(keyCode, event);
        }
    }
}
