package com.dgcheshang.cheji.Activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
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
import com.dgcheshang.cheji.CjApplication;
import com.dgcheshang.cheji.Database.DbConstants;
import com.dgcheshang.cheji.Database.DbHandle;
import com.dgcheshang.cheji.R;
import com.dgcheshang.cheji.Tools.Helper;
import com.dgcheshang.cheji.Tools.LoadingDialogUtils;
import com.dgcheshang.cheji.Tools.Speaking;
import com.dgcheshang.cheji.netty.conf.NettyConf;
import com.dgcheshang.cheji.netty.po.Tdata;
import com.dgcheshang.cheji.netty.po.Xydl;
import com.dgcheshang.cheji.netty.proputil.PxkcUtil;
import com.dgcheshang.cheji.netty.serverreply.SfrzR;
import com.dgcheshang.cheji.netty.serverreply.XydcR;
import com.dgcheshang.cheji.netty.serverreply.XydlR;
import com.dgcheshang.cheji.netty.timer.CardTimer;
import com.dgcheshang.cheji.netty.timer.FrinterTimer;
import com.dgcheshang.cheji.netty.timer.LoadingTimer;
import com.dgcheshang.cheji.netty.timer.XsjlTimer;
import com.dgcheshang.cheji.netty.tools.RfidUtil;
import com.dgcheshang.cheji.netty.tools.fingerprint.BaseInitTask;
import com.dgcheshang.cheji.netty.util.ByteUtil;
import com.dgcheshang.cheji.netty.util.CardContent;
import com.dgcheshang.cheji.netty.util.CountDistance;
import com.dgcheshang.cheji.netty.util.ForwardUtil;
import com.dgcheshang.cheji.netty.util.MsgUtilClient;
import com.dgcheshang.cheji.netty.util.RlsbUtil;
import com.dgcheshang.cheji.netty.util.ZdUtil;
import com.rscja.deviceapi.Fingerprint;
import com.rscja.deviceapi.RFIDWithISO14443A;
import com.serenegiant.usb.CameraDialog;
import com.serenegiant.usb.DeviceFilter;
import com.serenegiant.usb.IFrameCallback;
import com.serenegiant.usb.Size;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.UVCCamera;
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
 * 学员登录
 * */
public class LoginStudentActivity extends BaseInitActivity implements View.OnClickListener,CameraDialog.CameraDialogParent{

    Context context=LoginStudentActivity.this;
    private String TAG="LoginStudentActivity";
    public static final int REQUEST_A = 1;
    public static final int LOGIN_STU_SUCCESS = 1;

    ImageView image_shuaka,image_zhiwen,image_shenfen,image_project;
    public Fingerprint mFingerprint;
    private BaseInitTask mBaseInitTask;
    RFIDWithISO14443A mRFID;
    SharedPreferences sp;
    View layout_shenfen;
    ImageView image_stu;
    TextView tv_bianhao,tv_idcard,tv_kechen,tv_carlx,tv_stu_name,tv_logintime,tv_IDcard,tv_stuname,tv_cartype,tv_valid_time;
    SharedPreferences.Editor editor;
    Dialog loading;
    LoadingTimer loadingTimer;
    Timer timer;
    SfrzR xyxx;//全局参数
    String yzmm;
    RfidUtil rfid = new RfidUtil();
    private final Object mSync = new Object();

    TextView tv_zhiwen;

    View layout_showphoto;//拍照框
    String fileurl="/sdcard/jlypic/";//下载学员图片文件夹路径
    BroadcastReceiver receiver;//下载广播

    private USBMonitor mUSBMonitor;					// 用于监视USB设备接入
    private UVCCamera mUVCCameraL;					// 表示左边摄像头设备
    private UVCCamera mUVCCameraR;					// 表示右边摄像头设备

    private OutputStream snapshotOutStreamL;		// 用于左边摄像头拍照
    private String snapshotFileNameL;

    private UVCCameraTextureView mUVCCameraViewL;	// 用于左边摄像头预览
    private Surface mLeftPreviewSurface;
    private static final int PREVIEW_WIDTH = 320;
    private static final int PREVIEW_HEIGHT = 240;
    private static final boolean DEBUG = true;
    private CameraRecorder mp4RecorderL=new CameraRecorder(1);
    private int currentWidth = UVCCamera.DEFAULT_PREVIEW_WIDTH;
    private int currentHeight = UVCCamera.DEFAULT_PREVIEW_HEIGHT;

    public String landmarks_path, facenet_path, train_path;
    boolean isback=true;//点击返回键是否生效
    CardContent cardcontent;
    Handler handler=new  Handler(){
        @Override
        public void handleMessage(Message msg) {
            if(msg.arg1==1){
                //学员登录
                Log.e("TAG","返回登录结果");
                image_shenfen.setBackgroundResource(R.mipmap.login_ok_y);
                handleIn(msg);
            }else if(msg.arg1==2){
                //学员登出
                Bundle data = msg.getData();
                XydcR xydcr = (XydcR) data.getSerializable("xydcr");//学员登录成功后返回来的数据
                handleOut(xydcr);

            }else if(msg.arg1==5){
                //从服务器获取学员信息
                Bundle data = msg.getData();
                xyxx = (SfrzR) data.getSerializable("sfxx");
                //验证IC卡信息
                if(NettyConf.yz_ICcard==0){
                    //不验证IC卡
                    getXyxx(xyxx);
                    }else if(NettyConf.yz_ICcard==1){
                    //验证只提示
                    if(xyxx.getTybh().equals(cardcontent.getTybh())&&xyxx.getSfzh().equals(cardcontent.getZjhm())&&xyxx.getXm().equals(cardcontent.getXm())&&xyxx.getCx().equals(cardcontent.getCx())) {
                    }else {
                        Speaking.in("学员信息不匹配");
                    }
                    getXyxx(xyxx);
                }else if(NettyConf.yz_ICcard==2){
                    //验证IC卡
                    if(xyxx.getTybh().equals(cardcontent.getTybh())&&xyxx.getSfzh().equals(cardcontent.getZjhm())&&xyxx.getXm().equals(cardcontent.getXm())&&xyxx.getCx().equals(cardcontent.getCx())) {
                        getXyxx(xyxx);
                    }else {
                        Speaking.in("学员信息不匹配");
                    }
                }
            }else if(msg.arg1==6){
                //返回获取uid
                String xyuid = msg.getData().getString("xyuid");
                //返回读卡信息
                cardcontent = (CardContent) msg.getData().get("cardcontent");
                image_shuaka.setBackgroundResource(R.mipmap.login_rid_xycard_y);

                String sql="select * from tsfrz where uuid=? and lx=?";
                String[] params={xyuid,"4"+NettyConf.sbtype};
                ArrayList<SfrzR> list= DbHandle.queryTsfrz(sql,params);
                if(list.size()==0){
                    if(ZdUtil.pdNetwork()&&NettyConf.constate==1) {
                        ZdUtil.sendSfrz(xyuid,NettyConf.sbtype,"4");
                    }else {
                        Speaking.in("请连接服务器");
                    }
                }else{
                    xyxx=list.get(0);
                    //验证IC卡信息
                    if(NettyConf.yz_ICcard==0){
                        //不验证IC卡
                        getXyxx(xyxx);
                    }else if(NettyConf.yz_ICcard==1){
                        //验证只提示
                        if(xyxx.getTybh().equals(cardcontent.getTybh())&&xyxx.getSfzh().equals(cardcontent.getZjhm())&&xyxx.getXm().equals(cardcontent.getXm())&&xyxx.getCx().equals(cardcontent.getCx())) {
                        }else {
                            Speaking.in("学员信息不匹配");
                        }
                        getXyxx(xyxx);
                    }else if(NettyConf.yz_ICcard==2){
                        //验证IC卡
                        if(xyxx.getTybh().equals(cardcontent.getTybh())&&xyxx.getSfzh().equals(cardcontent.getZjhm())&&xyxx.getXm().equals(cardcontent.getXm())&&xyxx.getCx().equals(cardcontent.getCx())) {
                            getXyxx(xyxx);
                        }else {
                            Speaking.in("学员信息不匹配");
                        }
                    }
                }
            }else if(msg.arg1==7){//验证指纹成功后
                //学员登录
                image_zhiwen.setBackgroundResource(R.mipmap.login_fingerprint_y);
                loading = LoadingDialogUtils.createLoadingDialog(context, "登录中...");
                studentLogin();
            }else if(msg.arg1==8){
                studentOut2();
            }else if(msg.arg1==9){
                //登出拍照
                studentOut1();
            }else if(msg.arg1==10){
                //强制登出验证返回结果
                int yzjg = msg.getData().getInt("yzjg");
                if(yzjg==0){
                    editor.putString("yzmm",yzmm);//保存验证密码
                    editor.commit();
                    if(!ZdUtil.ispz){
                        qzStudentOut();
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
        setContentView(R.layout.activity_login_student);
        NettyConf.handlersmap.put("loginstudent",handler);
        initView();

        if(NettyConf.fringerTimer!=null){
            NettyConf.fringerTimer.cancel();
            NettyConf.fringerTimer=null;
        }

        if(NettyConf.cardtimer!=null){
            NettyConf.cardtimer.cancel();
            NettyConf.cardtimer=null;
        }

        CardTimer.isstop=false;
        FrinterTimer.ispp=false;

        if(NettyConf.xystate!=1) {
            try {
                mRFID = RFIDWithISO14443A.getInstance();
            }catch(Exception e){
            }
            Speaking.in("请选择培训课程");
        }

    }

    /**
     * 初始化布局
     * */
    private void initView() {
        //保存教练信息
        sp = getSharedPreferences("student", Context.MODE_PRIVATE); //私有数据
        editor = sp.edit();//获取编辑器
        View layout_back = findViewById(R.id.layout_back);
        TextView tv_title = (TextView) findViewById(R.id.tv_title);//标题
        tv_kechen = (TextView) findViewById(R.id.tv_kechen);//选择课程显示
        //学员登录布局
        View layout_studentin = findViewById(R.id.layout_studentin);//学员登录布局
        layout_shenfen = findViewById(R.id.layout_shenfen);//身份证布局
        image_shuaka = (ImageView) findViewById(R.id.image_shuaka);//刷卡图片
        image_zhiwen = (ImageView) findViewById(R.id.image_zhiwen);//指纹图片
        tv_zhiwen = (TextView) findViewById(R.id.tv_zhiwen);//指纹识别
        image_shenfen = (ImageView) findViewById(R.id.image_shenfen);//身份图片
        image_project = (ImageView) findViewById(R.id.image_project);//课堂图片
        tv_bianhao = (TextView) findViewById(R.id.tv_bianhao);//学员编号
        tv_idcard = (TextView) findViewById(R.id.tv_idcard);//身份证号
        tv_stu_name = (TextView) findViewById(R.id.tv_stu_name);//姓名
        tv_carlx = (TextView) findViewById(R.id.tv_carlx);//车型
        layout_shenfen.setVisibility(View.INVISIBLE);
        //学员登出布局
        View layout_studentout = findViewById(R.id.layout_studentout);//学员登出布局
        Button bt_studentout = (Button) findViewById(R.id.bt_studentout);//登出按钮
        Button bt_choose = (Button) findViewById(R.id.bt_choose);//课程选择按钮
        TextView tv_studentcode = (TextView) findViewById(R.id.tv_studentcode);//学员编号
        TextView tv_zxs = (TextView) findViewById(R.id.tv_zxs);//总学时
        TextView tv_ywcxs = (TextView) findViewById(R.id.tv_ywcxs);//已完成学时
        TextView tv_todayxs = (TextView) findViewById(R.id.tv_todayxs);//今日学时
        TextView tv_zlc = (TextView) findViewById(R.id.tv_zlc);//总里程
        TextView tv_ywclc = (TextView) findViewById(R.id.tv_ywclc);//已完成里程
        tv_logintime = (TextView) findViewById(R.id.tv_logintime);//登录时间
        tv_valid_time = (TextView) findViewById(R.id.tv_valid_time);//有效培训时长
        tv_IDcard = (TextView) findViewById(R.id.tv_IDcard);//身份证号
        tv_stuname = (TextView) findViewById(R.id.tv_stuname);//姓名
        tv_cartype = (TextView) findViewById(R.id.tv_cartype);//车型
        image_stu = (ImageView) findViewById(R.id.image_stu);//学员证件照
        Button bt_validtime = (Button) findViewById(R.id.bt_validtime);//有效学时查询
        bt_validtime.setOnClickListener(this);
        View layout_qzout = findViewById(R.id.layout_qzout);//强制登出
        // 判断学员是否登录
        if(NettyConf.xystate==1){//已登录
            layout_studentin.setVisibility(View.GONE);
            layout_studentout.setVisibility(View.VISIBLE);
            layout_qzout.setVisibility(View.VISIBLE);
            tv_title.setText("学员管理");
            showCoachPhoto();
            String xybh = sp.getString("xybh", "");//学员编号
            tv_studentcode.setText(xybh);
            int wcxs = sp.getInt("wcxs", 0);//当前培训部分已完成学时
            if(wcxs<60) {
                tv_ywcxs.setText(wcxs + "分钟");
            }else{
                tv_ywcxs.setText(wcxs/60 + "小时"+wcxs%60+"分钟");
            }
            int zpxxs = sp.getInt("zpxxs",0);//总培训学时
            if(zpxxs<60) {
                tv_zxs.setText(zpxxs + "分钟");
            }else{
                tv_zxs.setText(zpxxs/60 + "小时"+zpxxs%60+"分钟");
            }
            int zpxlc = sp.getInt("zpxlc", 0);//总培训里程
            tv_zlc.setText((zpxlc/10.0)+"公里");
            int wclc = sp.getInt("wclc", 0);//当前培训部分已完成里程
            tv_ywclc.setText((wclc/10.0)+"公里");
            String xzkc = sp.getString("xzkc", "");//选择的课程
            tv_kechen.setText(xzkc);
            String xm = sp.getString("xyxm", "");//姓名
            tv_stuname.setText(xm);
            String xyidcard = sp.getString("xyidcard", "");//身份证
            tv_IDcard.setText(xyidcard);
            String xydltime = sp.getString("xydltime", "");//登录时间
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyMMddHHmmss");
                SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                tv_logintime.setText(sdf2.format(sdf.parse(xydltime)));
            }catch(Exception e2){}
            String cx = sp.getString("cx", "");//车型
            tv_cartype.setText(cx);
            String jrxs = sp.getString("jrxs", "");//今日学时
            int jxs=Integer.valueOf(jrxs);
            if(jxs<60) {
                tv_todayxs.setText(jrxs + "分钟");
            }else if(jxs>240){
                tv_todayxs.setText("4小时0分钟");
            }else{
                tv_todayxs.setText(jxs/60 + "小时"+jxs%60+"分钟");
            }
            int fzpxjlsc = XsjlTimer.fzpxjlsc;
            if(fzpxjlsc<60) {
                tv_valid_time.setText(fzpxjlsc + "分钟");
            }else if(fzpxjlsc>240){
                tv_valid_time.setText("4小时0分钟");
            }else{
                tv_valid_time.setText(fzpxjlsc/60 + "小时"+fzpxjlsc%60+"分钟");
            }

        }else {//未登录
            layout_studentin.setVisibility(View.VISIBLE);
            layout_studentout.setVisibility(View.GONE);
            layout_qzout.setVisibility(View.GONE);
        }

        if(NettyConf.sbtype.equals("4")){
            //人脸模块
            image_zhiwen.setBackgroundResource(R.mipmap.login_face_n);//指纹识别改成显示人脸识别图像
            tv_zhiwen.setText("人脸识别");
        }
        layout_showphoto = findViewById(R.id.layout_showphoto);
        layout_showphoto.setVisibility(View.INVISIBLE);
        //摄像头
        mUVCCameraViewL = (UVCCameraTextureView)findViewById(R.id.camera_view_L);
        mUVCCameraViewL.setAspectRatio(PREVIEW_WIDTH / (float)PREVIEW_HEIGHT);

        refreshControls();

        layout_back.setOnClickListener(this);
        bt_choose.setOnClickListener(this);
        bt_studentout.setOnClickListener(this);
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

            case R.id.bt_choose://课程选择
                Choosedialog();
                break;

            case R.id.bt_studentout://学员登出
                if(!ZdUtil.ispz){
//                    if(NettyConf.sbtype.equals("1")&&NettyConf.logintype_stu!=2){
//                        loading = LoadingDialogUtils.createLoadingDialog(context,"学员登出中(请验证指纹)...");
//                    }
                    if(NettyConf.logintype_stu==1){
                        //人脸登出方式
                        studentOut();
                    }else if(NettyConf.logintype_stu==2||NettyConf.logintype_stu==4||NettyConf.logintype_stu==3){
                        //扫码登出方式
                        loading = LoadingDialogUtils.createLoadingDialog(context,"学员登出中...");
                        ZdUtil.studentOut1();
                    }

                }else {
                    Toast.makeText(context,",正在拍照请稍后操作",Toast.LENGTH_SHORT).show();
                }
                break;

            case R.id.layout_qzout://强制登出
                showliuyanDialog();
                break;

            case R.id.bt_validtime://有效学时
                int shuiji = (int)(Math.random()*(9999-1000+1))+1000;
                String s = String.valueOf(shuiji);
                showyzDialog(s);
                break;
        }
    }

    /**
     * 课程选择完后跳转回来
     * */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode){

            case REQUEST_A:

                switch (resultCode){
                    case ObjectContent1Activity.LOGIN_CONTENT_SUCCESS:
                        String pxnr = data.getStringExtra("pxnr");//培训内容
                        String objecttype = data.getStringExtra("objecttype");//培训第几部分
                        String kcbh = data.getStringExtra("kcbh");
                        if(objecttype.equals("2")){
                            tv_kechen.setText("第二部分——"+pxnr);
                            editor.putString("xzkc","第二部分——"+pxnr);
                            editor.commit();
                        }else if(objecttype.equals("3")){
                            tv_kechen.setText("第三部分——"+pxnr);
                            editor.putString("xzkc","第三部分——"+pxnr);
                            editor.commit();
                        }
                        SharedPreferences coachsp = getSharedPreferences("coach", Context.MODE_PRIVATE);
                        String cx = coachsp.getString("cx", "");
                        String jlcx = PxkcUtil.getValue(cx);
                        String pxck="1"+jlcx+objecttype+kcbh+"0000";//培训课程
                        NettyConf.pxkc=pxck;
                        //选择完成后改变课程选择图片
                        image_project.setBackgroundResource(R.mipmap.login_project_y);
                        //启动刷卡
                        Speaking.in("学员请刷卡");
                        CardTimer cardTimer=new CardTimer(mRFID, "xycard");
                        NettyConf.cardtimer=new Timer();
                        NettyConf.cardtimer.schedule(cardTimer,300,2000);
                        break;
                }
                break;
        }
    }

    /**
     * 学员登录
     * */
    private void studentLogin() {
        try {
            if (ZdUtil.pdGps()) {
                String gnss = ZdUtil.getGnss();
                Xydl xydl = new Xydl();
                xydl.setXybh(NettyConf.xbh);//学员编号
                xydl.setJlbh(NettyConf.jbh);//教练编号

                String ktid = String.valueOf(new Date().getTime());
                ktid = ktid.substring(ktid.length() - 12, ktid.length() - 3);
                NettyConf.ktid = ktid;
                xydl.setKtid(ktid);
                xydl.setPxkc(NettyConf.pxkc);
                xydl.setGnss(gnss);
                byte[] xydlb3 = xydl.getXydlBytes();
                byte[] xydlb2 = MsgUtilClient.getMsgExtend(xydlb3, "0201", "13", "2");
                List<Tdata> list = MsgUtilClient.generateMsg(xydlb2, "0900", NettyConf.mobile, "1");

                if(ZdUtil.pdNetwork()&&NettyConf.constate==1&&NettyConf.jqstate==1) {
                    ForwardUtil.sendData(list, 0,1);
                }else{
                    NettyConf.sendState=false;//发送状态
                    if(NettyConf.debug){
                        Log.e("TAG","缓存学员登陆数据");
                    }
                    editor.putBoolean("sendState",NettyConf.sendState);
                    editor.commit();
                    DbHandle.insertTdatas(list,2);

                    Message msg=new Message();
                    XydlR xr=new XydlR();
                    xr.setJg(1);
                    Bundle bundle = new Bundle();
                    bundle.putSerializable("xydlr",xr);
                    msg.setData(bundle);
                    handleIn(msg);
                }
            } else {
                Log.e(TAG, "gps数据获取失败！");
                Toast.makeText(context, "gps数据获取失败", Toast.LENGTH_SHORT).show();
            }
        }catch (Exception e){
            Log.e(TAG,"学员登陆数据异常:"+e.getMessage());
            Toast.makeText(context,"学员登陆数据异常",Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 学员登出
     * */
    private void studentOut() {
        //验证指纹
        String sql="select * from tsfrz where tybh=? and lx=?";
        String[] params={NettyConf.xbh,"4"+NettyConf.sbtype};
        ArrayList<SfrzR> list= DbHandle.queryTsfrz(sql,params);
        if(list.size()==0){
            studentOut1();
        }else{
            SfrzR xyxx=list.get(0);
            getXyxxout(xyxx);
        }

        //studentOut1();
    }

    /**
     * 登出拍照
     */
    private void studentOut1() {
        //loading = LoadingDialogUtils.createLoadingDialog(context,"正在登出...");
        loadingTimer = new LoadingTimer(loading);
        timer = new Timer();
        timer.schedule(loadingTimer,NettyConf.controltime);
        if(NettyConf.sbtype.equals("1")){
            //指纹识别
            ZdUtil.studentOut1();
        }else {
            //人脸识别
            try {
                if (ZdUtil.pdGps()) {
                    ZdUtil.sendZpsc2("129", "0", "18",ZdUtil.getGnss4(),bdpic);
                } else {
                    Speaking.in("定位数据获取失败");
                }
            }catch(Exception e){
                Speaking.in("教练员登出数据异常");
            }
        }

    }

    /**
     * 登出拍照
     */
    private void qzStudentOut() {
        //loading = LoadingDialogUtils.createLoadingDialog(context,"正在登出...");
        loadingTimer = new LoadingTimer(loading);
        timer = new Timer();
        timer.schedule(loadingTimer,NettyConf.controltime);
        ZdUtil.qzStudentOut();
    }

    private void studentOut2(){
        List<Tdata> list=ZdUtil.studentOut2();

        if(NettyConf.sendState&&NettyConf.constate==1&&NettyConf.jqstate==1){
            ForwardUtil.sendData(list, 1,6);
        }else{
            DbHandle.insertTdatas(list,6);
            //改变学员登出状态
            XydcR xr=new XydcR();
            xr.setJg(1);
            handleOut(xr);
        }

    }

    /**
     * 第几部分选择
     * */
    private void Choosedialog(){
        final String items[]={"第二部分","第三部分"};
        final String[] pxnr = {"2"};
        AlertDialog.Builder builder=new AlertDialog.Builder(context);  //先得到构造器
        builder.setTitle("部分选择"); //设置标题
        builder.setSingleChoiceItems(items,0,new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //dialog.dismiss();
                if( items[which].equals("第二部分")){
                    pxnr[0] ="2";
                }else {
                    pxnr[0] ="3";
                }
            }
        });
        builder.setPositiveButton("确定",new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
//                Toast.makeText(context, "确定"+pxnr[0], Toast.LENGTH_SHORT).show();
                Intent intent = new Intent();
                intent.setClass(context, ObjectContent1Activity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra("objecttype",pxnr[0]);
                startActivityForResult(intent,REQUEST_A);
            }
        });
        builder.create().show();
    }

    /**
     * 登录处理
     * */
    public synchronized void handleIn(Message msg){
        //取消加载动画
        if(loadingTimer!=null) {
            loadingTimer.cancel();
        }
        if(timer!=null) {
            timer.cancel();
        }

            if(NettyConf.xystate!=1) {

            Bundle data = msg.getData();
            XydlR xydlr = (XydlR) data.getSerializable("xydlr");//学员登录成功后返回来的数据
            if (xydlr.getJg() == 1) {//学员登录成功
                //保存登陆信息
                String temp=(xyxx.getLx()+"").substring(0,1)+NettyConf.sbtype;
                xyxx.setLx(Byte.valueOf(temp));
                DbHandle.insertTsfrz(xyxx);

                String jrxs = "0";
                if (StringUtils.isNotEmpty(xydlr.getFjxx())) {
                    jrxs = xydlr.getFjxx().split(",")[1];
                }

                NettyConf.xystate = 1;
                CountDistance.setTotalMile(0);
                //把总里程存储起来
                SharedPreferences sp = CjApplication.getInstance().getSharedPreferences("student", Context.MODE_PRIVATE); //私有数据
                SharedPreferences.Editor editor = sp.edit();//获取编辑器
                editor.putFloat("zlc",0);
                editor.commit();

                NettyConf.xydltime = ZdUtil.getTime2();
                XsjlTimer.fzpxjlsc=0;

                //发送学员登陆的广播
                Intent xydlIntent = new Intent();
                xydlIntent.setAction("xydl");
                sendBroadcast(xydlIntent);

                //上传拍照数据
                if(NettyConf.sbtype.equals("1")){
                    //指纹识别进入loginactivity页面拍照
                    ZdUtil.sendZpsc("129", "0", "17");
                }else {
                    //人脸识别上传对比成功的照片
                    ZdUtil.sendZpsc2("129","0","17",ZdUtil.getGnss4(),bdpic);
                }

                editor.putString("xybh", NettyConf.xbh);//学员编号
                if (xydlr.getWcxs() != 0) {
                    editor.putInt("wcxs", xydlr.getWcxs());//当前培训部分已完成学时
                }
                if (xydlr.getZpxxs() != 0) {
                    editor.putInt("zpxxs", xydlr.getZpxxs());//总培训学时
                }
                if (xydlr.getZpxlc() != 0) {
                    editor.putInt("zpxlc", xydlr.getZpxlc());//总培训里程
                }
                if (xydlr.getWclc() != 0) {
                    editor.putInt("wclc", xydlr.getWclc());//当前培训部分已完成里程
                }
                editor.putInt("xystate", 1);
                editor.putString("ktid", NettyConf.ktid);
                editor.putString("xydltime", NettyConf.xydltime);//学员登录时间
                editor.putString("xyxm", xyxx.getXm());//姓名
                editor.putString("xyidcard", xyxx.getSfzh());//身份证号
                editor.putString("jrxs", jrxs);//今日学时
                editor.putString("cx", xyxx.getCx());//车型
                editor.putInt("fzpxjlsc",0);
                editor.commit();

                //今日学时监控
                if (StringUtils.isNotEmpty(jrxs)) {
                    NettyConf.jrxxsc = Integer.valueOf(jrxs);
                } else {
                    NettyConf.jrxxsc = 0;
                }

                //返回信息
                Intent intent = new Intent();
                setResult(LOGIN_STU_SUCCESS, intent);

                LoadingDialogUtils.closeDialog(loading);

                Speaking.in("学员登陆成功");

                finish();
            } else {
                //失败则删除认证的缓存信息
                String[] params = {xyxx.getUuid(), String.valueOf(xyxx.getLx())};
                DbHandle.deleteData(DbConstants.T_SFRZ, "uuid=? and lx=?", params);

                LoadingDialogUtils.closeDialog(loading);
                if(StringUtils.isNotEmpty(xydlr.getFjxx())) {
                    Speaking.in(xydlr.getFjxx().split(",")[0]);
                }
                finish();
            }
        }
    }

    /**
     * 登出处理
     * */
    public void handleOut(XydcR xydcr){
        //取消加载动画
        if(loadingTimer!=null){
            loadingTimer.cancel();
        }
        if(timer!=null) {
            timer.cancel();
        }

        if(xydcr.getJg()==1){//学员登出成功
            //发出学员登出广播
            Intent xydcIntent=new Intent();
            xydcIntent.setAction("xydc");
            sendBroadcast(xydcIntent);

            ZdUtil.handleStudentOut();
            //返回登录页面
            Intent intent = new Intent();
            setResult(LOGIN_STU_SUCCESS,intent);

            LoadingDialogUtils.closeDialog(loading);

            finish();
        }else {
            //登出失败
            LoadingDialogUtils.closeDialog(loading);
            Speaking.in("学员登出失败");
        }
    }

    /**
     * 读卡成功后获取学员信息
     * */
    public void getXyxx(final SfrzR xyxx){
            String xx = xyxx.getXx();//学员指纹
            NettyConf.xbh = xyxx.getTybh();
            //获取信息成功后显示身份信息
            layout_shenfen.setVisibility(View.VISIBLE);
            tv_bianhao.setText(xyxx.getTybh());
            tv_idcard.setText(xyxx.getSfzh());
            tv_stu_name.setText(xyxx.getXm());
            tv_carlx.setText(xyxx.getCx());
            if (StringUtils.isNotEmpty(xx)) {
                mRFID.free();
                if (NettyConf.cardtimer != null) {
                    NettyConf.cardtimer.cancel();
                    NettyConf.cardtimer = null;
                }

                //判断选择指纹识别还是人脸识别
                if (NettyConf.sbtype.equals("1")) {
                    //指纹识别
                    commonXy(xx, "xycard");
                } else {
                    //人脸识别
                    if (ZdUtil.ispz == false) {
                        commonXy2(xyxx, "login");
                    } else {
                        Toast.makeText(context, "正在拍照，请稍后...", Toast.LENGTH_SHORT).show();
                    }

                }

            } else {
                //直接无指纹验证进行登陆
                image_zhiwen.setBackgroundResource(R.mipmap.login_fingerprint_y);
                loading = LoadingDialogUtils.createLoadingDialog(context, "正在登录...");
                studentLogin();
            }
    }

    /**
     * 学员登出指纹验证
     */
    public void getXyxxout(final SfrzR xyxx){
        String xx = xyxx.getXx();//学员指纹
        if(StringUtils.isNotEmpty(xx)){
            if(NettyConf.sbtype.equals("1")){
                //指纹识别
                commonXy(xx,"xycardout");
            }else {
                //人脸识别
                if(ZdUtil.ispz==false){

                    commonXy2(xyxx,"out");
                }else {
                    Toast.makeText(context,"正在拍照，请稍后...",Toast.LENGTH_SHORT).show();
                }
            }
        }else{
            //无信息直接登出
            qzStudentOut();
        }
    }

    public void commonXy(String xx,String type){
        try {
            mFingerprint = Fingerprint.getInstance();
            initFingerprint(-1);
            Thread.sleep(1000);
        } catch (Exception e) {
            e.printStackTrace();
        }
        mFingerprint.setReg(5,1);

        mFingerprint.empty();


        String[] ss=new String[2];
        if(xx.length()>1024){
            ss[0]=xx.substring(0,1024);
            ss[1]=xx.substring(1024,xx.length());
        }else{
            ss[0]=xx;
        }

        if(NettyConf.debug){
            Log.e("TAG","指纹数量："+ss.length);
        }

        mFingerprint.downChar(Fingerprint.BufferEnum.B2, ss[0]);

        Speaking.in("请验证指纹");
        try {
            FrinterTimer frinterTimer = new FrinterTimer(mFingerprint,type);
            NettyConf.fringerTimer= new Timer();
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
    public void commonXy2(final SfrzR xyxx,final String type){
        ZdUtil.ispz=true;
        isback=false;
        initrlCamera();
        rlsbtimer = new Timer();
        TimerTask rlsbtask=new TimerTask() {
            @Override
            public void run() {
                String xx = xyxx.getXx();//下载路径
//                xx=new String(ByteUtil.hexStringToByte(xx));
                Log.e("TAG","学员下载图片路径："+xx);
                String sfzh = xyxx.getSfzh();
                //判断文件夹是否存在
                RlsbUtil.isexistAndBuild(fileurl);
                //教练原始照片路径
                String xyzp=fileurl+sfzh+".jpg";
                if(RlsbUtil.isFileExist(xyzp)==false){
                    //没有学员照片去下载
                    downFile(xx,sfzh,xyzp,type);
                }else {
                    //有学员照片直接抓拍验证
                    rlsb(xyzp,sfzh, type,true);
                }
            }
        };
        rlsbtimer.schedule(rlsbtask,2000);
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
        Speaking.in("正在人脸识别，请对准摄像头");
        showCamera();
        Timer initbdtimer = new Timer();
        TimerTask task=new TimerTask() {
            @Override
            public void run() {
                Log.e("TAG","进行初始化比对");
                landmarks_path =RlsbUtil.getAssetsCacheFile(LoginStudentActivity.this,"face_landmarks_5_cilab.dat");
                facenet_path = RlsbUtil.getAssetsCacheFile(LoginStudentActivity.this,"facenet_cilab.dat");
                train_path =RlsbUtil.getAssetsCacheFile(LoginStudentActivity.this,"complex_training.txt");
                FaceDet.FaceDetInit(landmarks_path, facenet_path, train_path);
            }
        };
        initbdtimer.schedule(task,2000);
        RlsbUtil.addtimer(initbdtimer);
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
                    loading = LoadingDialogUtils.createLoadingDialog(context, "正在登出...");
                    ZdUtil.matchPassword(4,yzmm);
                    builder.dismiss();
                }else {
                    Toast.makeText(context,"请输入登出密码",Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * 验证查看有效学时dialog
     *
     * @param shuiji*/

    private void showyzDialog(final String shuiji){
        final AlertDialog builder = new AlertDialog.Builder(this,R.style.CustomDialog).create(); // 先得到构造器
        builder.show();
        builder.getWindow().setContentView(R.layout.dialog_shuiji_edt);
        builder.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);//解决不能弹出键盘
        LayoutInflater factory = LayoutInflater.from(this);
        View view = factory.inflate(R.layout.dialog_shuiji_edt, null);
        builder.getWindow().setContentView(view);
        final EditText edt_content = (EditText) view.findViewById(R.id.edt_content);
        TextView tv_yzm = (TextView) view.findViewById(R.id.tv_yzm);//验证码
        Button bt_cacnel = (Button) view.findViewById(R.id.bt_cacnel);
        Button bt_sure = (Button) view.findViewById(R.id.bt_sure);
        tv_yzm.setText(shuiji);

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
                String yzm = edt_content.getText().toString().trim();
                if(yzm.equals(shuiji)){
                    Intent intent = new Intent();
                    intent.setClass(context,ValidTimeActivity.class);
                    startActivity(intent);
                    builder.dismiss();
                }else {
                    Toast.makeText(context,"输入不正确",Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // 实现快照抓取
    private synchronized String captureSnapshot() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd.HH.mm.ss.SSSS");
        Date currentTime = new Date();
        //判断哪个摄像头在使用，则抓拍哪个

            //左边摄像头在使用
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

                // 将摄像头进行分配
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
                        Helper.fileSavedProcess(LoginStudentActivity.this, snapshotFileNameL);
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
        if (faceResult.size() > 0) {
            faceResult.clear();
        }
        File file = new File(mageurl);
        if (file.exists()&&file.length() > 0) {
            boolean ret = FaceDet.FD_FSDK_FaceDetection(mageurl, faceResult);
            Log.e("TAG","获取学员原始照片轮廓结果=" + ret);
            if(ret==true){
                //成功获取轮廓并保存
                boolean mIsSave = FaceDet.CaptureFaceMuti(mageurl, name);
                Log.e("TAG","保存学员原始照片轮廓结果=" + mIsSave);
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
    public void downFile(String url, final String sfzh, final String xyzp, final String type){
        //下载文件
        try {
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
                    if (refernece == myDwonloadID) {
                        Log.e("TAG","下载学员照片成功");
                        //下载完成操作，保存原照片 身份证号用来区别
                        rlsb(xyzp, sfzh,type,false);

                    }else {
                        isback=true;
                        Log.e("TAG","下载学员照片失败");
                        Speaking.in("照片下载失败");
                    }
                }
            };
            registerReceiver(receiver, filter);
        }catch (Exception ex){
            isback=true;
            Log.e("TAG","下载学员照片失败");
            Speaking.in("照片下载失败");
        }

    }

    /**
     * 人脸识别成功后处理教练登录或教练登出
     * ishave_pic 判断是否有教练照片，有则无需保存特征值，无则保存特征值 true有照片，false没照片
     * */
     Timer pztimer;
    boolean stopcamera=false;
    TimerTask pztask;
    int isfinishphoto=0;//超过60秒自动关闭
    public void rlsb(final String xyzp, final String sfzh, final String type ,boolean ishave_pic){
        try {
            Thread.sleep(800);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //保存学员原始照片,未登录状态保存特征值
        boolean save_ok;
        if(ishave_pic==false){
             save_ok=isfiled(xyzp, sfzh);
        }else {
            save_ok=true;
        }
        isback=true;
        if(save_ok==true &&isCameraL==true){
            pztimer = new Timer();
            pztask=new TimerTask() {
                @Override
                public void run() {
                    if(isfinishphoto<20){
                        isfinishphoto++;
                        String path = captureSnapshot();
                        boolean stopcamera=compare(path,sfzh);
                        if(stopcamera==true){
                            //关闭摄像头
                            pztimer.cancel();
                            closeCamera();
                            ZdUtil.ispz=false;
                            Timer rlcltimer = new Timer();
                            TimerTask task=new TimerTask() {
                                @Override
                                public void run() {
                                    //识别正确人脸
                                    if(type.equals("login")){
                                        //登录处理
                                        NettyConf.student_pic=xyzp;
                                        editor.putString("stuphoto",xyzp);
                                        editor.commit();
                                        studentLogin();
                                    }else if(type.equals("out")){
                                        //登出处理
                                        studentOut1();
                                    }
                                }
                            };
                            rlcltimer.schedule(task,200);
                            RlsbUtil.addtimer(rlcltimer);
                        }
                    }else {
                        //超过60秒自动关闭页面
                        pztimer.cancel();
                        closeCamera();
                        ZdUtil.ispz=false;
                        finish();
                    }

                }
            };
            pztimer.schedule(pztask,200,3000);
            RlsbUtil.addtimer(pztimer);
//            while (stopcamera==false){
//                String path = captureSnapshot();
//                stopcamera=compare(path);
//                if(stopcamera==true){
//                    //关闭摄像头
//                    closeCamera();
//                    ZdUtil.ispz=false;
//                    Timer rlcltimer = new Timer();
//                    TimerTask task=new TimerTask() {
//                        @Override
//                        public void run() {
//                            //识别正确人脸
//                            if(type.equals("login")){
//                                //登录处理
//                                studentLogin();
//                            }else if(type.equals("out")){
//                                //登出处理
//                                studentOut1();
//                            }
//                        }
//                    };
//                    rlcltimer.schedule(task,200);
//                    RlsbUtil.addtimer(rlcltimer);
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
    public boolean compare(String newcameraurl,String sfzh) {
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
        if(ret==true&&faceResult!=null&&faceResult.size()>0){

            String MatchName = FaceDet.FaceDetectMuti(newcameraurl, NettyConf.thd);
            Log.e("TAG","比对照片名字轮廓结果=" + MatchName);
            Log.e("TAG","原始身份证号=" + sfzh);

            if(StringUtils.isNotEmpty(MatchName)){
                bdpic=newcameraurl;
                return true;
            }else {
                boolean delete = RlsbUtil.delete(newcameraurl);
                //filepath-->图片绝对路径
                getContentResolver().delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, MediaStore.Images.Media.DATA + "=?", new String[]{newcameraurl});
                return false;
            }

        }else {
            RlsbUtil.delete(newcameraurl);
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
     * 显示本地学员证件照
     * */
    public void showCoachPhoto(){
//        String url=sp.getString("stuphoto","");
        String url=fileurl+sp.getString("xyidcard", "")+".jpg";
        image_stu.setImageURI(Uri.fromFile(new File(url)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(NettyConf.xystate!=1) {
            if (mRFID!=null&&!mRFID.isPowerOn()) {
                mRFID.init();
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.e("TAG","onstop");
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
        stopcamera=true;
        //关闭USB摄像头
        if (mUSBMonitor != null) {
            releaseCameraL();
            mUSBMonitor.unregister();
//            mUSBMonitor.destroy();
//            mUSBMonitor = null;
        }
    }

    /**
    * 页面关闭后调用
    *
    * */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.e("TAG","onDestroy");
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
        if(mFingerprint!=null){
            mFingerprint.free();
        }
        if(mRFID!=null){
            mRFID.free();
        }

        //关闭USB摄像头
        if (mUSBMonitor != null) {
            mUSBMonitor.unregister();
            mUSBMonitor.destroy();
            mUSBMonitor = null;
        }
        //解绑下载照片广播
        if(receiver!=null){
            unregisterReceiver(receiver);
        }

        //删除指纹识别初始化
        if(FaceDet!=null){
            FaceDet.FaceDetDeInit();
        }
        if(rlsbtimer!=null){
            rlsbtimer.cancel();
        }
        if(pztimer!=null){
            pztimer.cancel();
            ZdUtil.ispz=false;
        }
        stopcamera=true;
        NettyConf.handlersmap.remove("loginstudent");
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
