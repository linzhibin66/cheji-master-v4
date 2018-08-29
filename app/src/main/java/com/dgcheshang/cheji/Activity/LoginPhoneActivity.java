package com.dgcheshang.cheji.Activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.chainway.facedet.FD_FSDKFace;
import com.chainway.facedet.FaceDetJni;
import com.dgcheshang.cheji.Database.DbConstants;
import com.dgcheshang.cheji.Database.DbHandle;
import com.dgcheshang.cheji.R;
import com.dgcheshang.cheji.Tools.LoadingDialogUtils;
import com.dgcheshang.cheji.Tools.Speaking;
import com.dgcheshang.cheji.netty.conf.NettyConf;
import com.dgcheshang.cheji.netty.po.Jlydl;
import com.dgcheshang.cheji.netty.po.Tdata;
import com.dgcheshang.cheji.netty.po.Xydl;
import com.dgcheshang.cheji.netty.proputil.PxkcUtil;
import com.dgcheshang.cheji.netty.serverreply.JlydcR;
import com.dgcheshang.cheji.netty.serverreply.JlydlR;
import com.dgcheshang.cheji.netty.serverreply.SfrzR;
import com.dgcheshang.cheji.netty.serverreply.XydlR;
import com.dgcheshang.cheji.netty.timer.XsjlTimer;
import com.dgcheshang.cheji.netty.util.ByteUtil;
import com.dgcheshang.cheji.netty.util.CountDistance;
import com.dgcheshang.cheji.netty.util.ForwardUtil;
import com.dgcheshang.cheji.netty.util.MsgUtilClient;
import com.dgcheshang.cheji.netty.util.RlsbUtil;
import com.dgcheshang.cheji.netty.util.ZdUtil;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import sun.misc.BASE64Decoder;

/**
 * 手机号码登录
 * */
public class LoginPhoneActivity extends Activity implements View.OnClickListener{
    Context context =LoginPhoneActivity.this;
    TextView tv_title,tv_pxkc;
    String fileurl="/sdcard/jlypic/";//下载学员图片文件夹路径
    public static final int REQUEST_A = 1;
    public static final int LOGIN_COA_SUCCESS = 0;
    public static final int LOGIN_STU_SUCCESS = 1;
    SharedPreferences coachsp;
    SharedPreferences stusp;
    SharedPreferences.Editor coachedit;
    SharedPreferences.Editor stuedit;
    int whologin;
    SfrzR sfxx;
    EditText edt_phonenumb;
    Dialog loading;
    Button bt_choose;
    BroadcastReceiver receiver;//下载广播
    public String landmarks_path, facenet_path, train_path;
    TextView tv_idcard,tv_name,tv_nametype;
    Handler handler=new  Handler(){
        @Override
        public void handleMessage(Message msg) {
            if(msg.arg1==1){
                //教练员登录成功
                coachhandleIn(msg);

            } else if(msg.arg1==2){
                //学员登录成功
                stuhandleIn(msg);

            } else if(msg.arg1==5){
                //获取教练信息
                Bundle data = msg.getData();
                sfxx = (SfrzR) data.getSerializable("sfxx");

                String xx = sfxx.getXx();//照片路径

                //判断文件夹是否存在
                RlsbUtil.isexistAndBuild(fileurl);
                //教练原始照片路径
                String zp=fileurl+sfxx.getSfzh()+".jpg";

                if(sfxx.getLx()==1){
                    //教练员

                    if(RlsbUtil.isFileExist(zp)==false){
                        //没有教练照片去下载
//                        xx=new String(ByteUtil.hexStringToByte(xx));
                        Log.e("TAG","教练照片路径"+xx);
                        downFile(xx,sfxx,1);

                    }else {
                        getJlxx(sfxx);
                    }

                }else {
                    //学员
                    if(RlsbUtil.isFileExist(zp)==false){
                        //没有学员照片去下载
//                        xx=new String(ByteUtil.hexStringToByte(xx));
                        Log.e("TAG","学员照片路径"+xx);

                        //路径有效，下载照片
                        downFile(xx,sfxx,4);

                    }else {
                        getXyxx(sfxx);
                    }
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_phone);
        NettyConf.handlersmap.put("phonelogin",handler);
        initView();
        initrlCamera();
    }

    /**
     * 初始化布局
     * */
    private void initView() {
        coachsp = getSharedPreferences("coach", Context.MODE_PRIVATE); //私有数据
        coachedit = coachsp.edit();
        stusp = getSharedPreferences("student", Context.MODE_PRIVATE); //私有数据
        stuedit = stusp.edit();//获取编辑器
        whologin = getIntent().getExtras().getInt("whologin", 0);
        View layout_back = findViewById(R.id.layout_back);//返回按钮
        layout_back.setOnClickListener(this);
        tv_title = findViewById(R.id.tv_title);//标题
        tv_pxkc = findViewById(R.id.tv_pxkc);//培训课程
        edt_phonenumb = findViewById(R.id.edt_phonenumb);//手机号码输入框
        tv_idcard = findViewById(R.id.tv_idcard);//身份证号
        tv_name = findViewById(R.id.tv_name);//姓名
        tv_nametype = findViewById(R.id.tv_nametype);//姓名
        View layout_choose = findViewById(R.id.layout_choose);//课程选择按钮
        bt_choose = findViewById(R.id.bt_choose);//选择按钮
        bt_choose.setOnClickListener(this);
        Button bt_login = findViewById(R.id.bt_login);//登录按钮
        bt_login.setOnClickListener(this);
        if(whologin==1){
            tv_title.setText("教练员登录");
            tv_nametype.setText("教练姓名：");
            layout_choose.setVisibility(View.GONE);
        }else {
            tv_title.setText("学员登录");
            Speaking.in("请选择培训课程");
            tv_nametype.setText("学员姓名：");
            layout_choose.setVisibility(View.VISIBLE);
        }
    }

/**
 * 点击事件
 * */
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.layout_back://返回
                finish();
                break;

            case R.id.bt_login://登录按钮

                if(!edt_phonenumb.getText().toString().trim().equals("")){
                    //有输入电话号码
                    if(whologin==1){
                        //教练员登录
                        loading = LoadingDialogUtils.createLoadingDialog(context, "教练员登录中...");
                        login();
                    }else {
                        //学员登录
                        if(!tv_pxkc.getText().toString().trim().equals("")){
                            loading = LoadingDialogUtils.createLoadingDialog(context, "学员登录中...");
                            login();
                        }else {
                            Toast.makeText(context,"请先选择培训课程",Toast.LENGTH_SHORT).show();
                        }
                    }
                }else {
                    Toast.makeText(context,"请先输入手机号或身份证号",Toast.LENGTH_SHORT).show();
                }

                break;

            case R.id.bt_choose://课程选择
                Choosedialog();
                break;
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
                            tv_pxkc.setText("第二部分——"+pxnr);
                            stuedit.putString("xzkc","第二部分——"+pxnr);
                            stuedit.commit();
                        }else if(objecttype.equals("3")){
                            tv_pxkc.setText("第三部分——"+pxnr);
                            stuedit.putString("xzkc","第三部分——"+pxnr);
                            stuedit.commit();
                        }

                        String cx = coachsp.getString("cx", "");
                        String jlcx = PxkcUtil.getValue(cx);
                        String pxck="1"+jlcx+objecttype+kcbh+"0000";//培训课程
                        NettyConf.pxkc=pxck;
                        break;
                }
                break;
        }
    }

    /**
    * 登录按钮
    * */
    private void login(){
        String phonenumb = edt_phonenumb.getText().toString().trim();//获取输入手机号码
        String sql="select * from tsfrz where uuid=? and lx=?";
        if(whologin==1){
            //教练登录
            String[] params={phonenumb,"1"+"4"};
            ArrayList<SfrzR> list= DbHandle.queryTsfrz(sql,params);
            if(list.size()==0){
                if(ZdUtil.pdNetwork()&&NettyConf.constate==1) {
                    ZdUtil.sendSfrz(phonenumb,"4", "1");
                }else {
                    Speaking.in("请连接服务器");
                }
            }else{
                sfxx=list.get(0);
                getJlxx(sfxx);
            }
        }else {
            //学员登录

            String[] params={phonenumb,"4"+"4"};
            ArrayList<SfrzR> list= DbHandle.queryTsfrz(sql,params);
            if(list.size()==0){
                if(ZdUtil.pdNetwork()&&NettyConf.constate==1) {
                    ZdUtil.sendSfrz(phonenumb,"4","4");
                }else {
                    Speaking.in("请连接服务器");
                }
            }else{
                sfxx=list.get(0);
                getXyxx(sfxx);
            }
        }

    }

    /**
     * 成功获取教练信息
     * */
    public void getJlxx(final SfrzR jlxx){
        NettyConf.cx = jlxx.getCx();//车型
        NettyConf.jbh = jlxx.getTybh();//统一编号
        NettyConf.jzjhm = jlxx.getSfzh();//身份证号
        tv_idcard.setText(NettyConf.jzjhm);
        tv_name.setText(jlxx.getXm());
        coachLogin();
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
                        Log.e("TAG","发送教练数据");
                    }
                    ForwardUtil.sendData(list, 0,1);
                }else{
                    if(NettyConf.debug){
                        Log.e("TAG","缓存教练数据");
                    }
                    DbHandle.insertTdatas(list,1);

                    Message msg=new Message();
                    JlydlR jr=new JlydlR();
                    jr.setJg(1);
                    Bundle bundle = new Bundle();
                    bundle.putSerializable("jldlr",jr);
                    msg.setData(bundle);
                    coachhandleIn(msg);
                }
            } else {
                Toast.makeText(context, "gps数据获取失败", Toast.LENGTH_SHORT).show();
            }
        }catch(Exception e){
            Toast.makeText(context,"教练员登陆数据异常",Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 登录处理
     * */
    public synchronized void coachhandleIn(Message msg){

        if(NettyConf.jlstate!=1) {
            Bundle data = msg.getData();
            JlydlR jldlr = (JlydlR) data.getSerializable("jldlr");//教练登录成功后返回来的数据

            if (jldlr.getJg() == 1) {//教练登录成功

                //存入缓存
                String temp=(sfxx.getLx()+"").substring(0,1)+NettyConf.sbtype;
                sfxx.setLx(Byte.valueOf(temp));
                DbHandle.insertTsfrz(sfxx);
                coachedit.putString("jlbh", NettyConf.jbh);//教练编号
                coachedit.putInt("jlstate", 1);
                coachedit.putString("cx", NettyConf.cx);//教练车型
                coachedit.putString("jzjhm", NettyConf.jzjhm);//证件号码
                coachedit.putString("jlxm", sfxx.getXm());//教练姓名
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");//保存年月日
                SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//保存年月日
                coachedit.putString("logintime",sdf.format(new Date(System.currentTimeMillis())));
                coachedit.putString("logintime1",sdf1.format(new Date(System.currentTimeMillis())));
                coachedit.commit();//提交修改
                NettyConf.jlstate = 1;
                //上传拍照数据
                ZdUtil.sendZpsc("129", "0", "20");

                Intent intent = new Intent();
                setResult(LOGIN_COA_SUCCESS, intent);

                Speaking.in("教练登陆成功");
            } else {
                //删除缓存
                String[] params = {sfxx.getUuid(), String.valueOf(sfxx.getLx())};
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
     * 成功获取学员信息
     * */
    public void getXyxx(final SfrzR xyxx){
        NettyConf.xbh=xyxx.getTybh();
        tv_idcard.setText(xyxx.getSfzh());
        tv_name.setText(xyxx.getXm());
        studentLogin();
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
                    stuedit.putBoolean("sendState",NettyConf.sendState);
                    stuedit.commit();
                    DbHandle.insertTdatas(list,2);

                    Message msg=new Message();
                    XydlR xr=new XydlR();
                    xr.setJg(1);
                    Bundle bundle = new Bundle();
                    bundle.putSerializable("xydlr",xr);
                    msg.setData(bundle);
                    stuhandleIn(msg);
                }
            } else {
                Log.e("TAG", "gps数据获取失败！");
                Toast.makeText(context, "gps数据获取失败", Toast.LENGTH_SHORT).show();
            }
        }catch (Exception e){
            Log.e("tag","学员登陆数据异常:"+e.getMessage());
            Toast.makeText(context,"学员登陆数据异常",Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 登录处理
     * */
    public synchronized void stuhandleIn(Message msg){
        if(NettyConf.xystate!=1) {

            Bundle data = msg.getData();
            XydlR xydlr = (XydlR) data.getSerializable("xydlr");//学员登录成功后返回来的数据
            if (xydlr.getJg() == 1) {//学员登录成功
                //保存登陆信息
                String temp=(sfxx.getLx()+"").substring(0,1)+NettyConf.sbtype;
                sfxx.setLx(Byte.valueOf(temp));
                DbHandle.insertTsfrz(sfxx);

                String jrxs = "0";
                if (StringUtils.isNotEmpty(xydlr.getFjxx())) {
                    jrxs = xydlr.getFjxx().split(",")[1];
                }

                NettyConf.xystate = 1;
                CountDistance.setTotalMile(0);
                //把总里程存储起来
                stuedit.putFloat("zlc",0);
                stuedit.commit();

                NettyConf.xydltime = ZdUtil.getTime2();
                XsjlTimer.fzpxjlsc=0;

                //发送学员登陆的广播
                Intent xydlIntent = new Intent();
                xydlIntent.setAction("xydl");
                sendBroadcast(xydlIntent);
                //上传拍照数据
                //别进入loginactivity页面拍照
                ZdUtil.sendZpsc("129", "0", "17");

                stuedit.putString("xybh", NettyConf.xbh);//学员编号
                if (xydlr.getWcxs() != 0) {
                    stuedit.putInt("wcxs", xydlr.getWcxs());//当前培训部分已完成学时
                }
                if (xydlr.getZpxxs() != 0) {
                    stuedit.putInt("zpxxs", xydlr.getZpxxs());//总培训学时
                }
                if (xydlr.getZpxlc() != 0) {
                    stuedit.putInt("zpxlc", xydlr.getZpxlc());//总培训里程
                }
                if (xydlr.getWclc() != 0) {
                    stuedit.putInt("wclc", xydlr.getWclc());//当前培训部分已完成里程
                }
                stuedit.putInt("xystate", 1);
                stuedit.putString("ktid", NettyConf.ktid);
                stuedit.putString("xydltime", NettyConf.xydltime);//学员登录时间
                stuedit.putString("xyxm", sfxx.getXm());//姓名
                stuedit.putString("xyidcard", sfxx.getSfzh());//身份证号
                stuedit.putString("jrxs", jrxs);//今日学时
                stuedit.putString("cx", sfxx.getCx());//车型
                stuedit.putInt("fzpxjlsc",0);
                stuedit.commit();

                //今日学时监控
                if (StringUtils.isNotEmpty(jrxs)) {
                    NettyConf.jrxxsc = Integer.valueOf(jrxs);
                } else {
                    NettyConf.jrxxsc = 0;
                }

                //返回信息
                Intent intent = new Intent();
                setResult(LOGIN_STU_SUCCESS, intent);

                Speaking.in("学员登陆成功");

                finish();
            } else {
                //失败则删除认证的缓存信息
                String[] params = {sfxx.getUuid(), String.valueOf(sfxx.getLx())};
                DbHandle.deleteData(DbConstants.T_SFRZ, "uuid=? and lx=?", params);

                if(StringUtils.isNotEmpty(xydlr.getFjxx())) {
                    Speaking.in(xydlr.getFjxx().split(",")[0]);
                }
                finish();
            }
            loading.cancel();
        }
    }

    /**
     * 下载教练员、学员图片
     * */
    public void downFile(String url, final SfrzR sfxx,final int type){
        //下载文件
        try {
            final DownloadManager dManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            Uri uri = Uri.parse(url);
            DownloadManager.Request request = new DownloadManager.Request(uri);
            // 设置下载路径和文件名
            request.setDestinationInExternalPublicDir("jlypic", sfxx.getSfzh()+".jpg");
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
                        Log.e("TAG","下载照片成功");
                        //下载完成操作，保存原照片 身份证号用来区别
                        if(type==1){
                            //教练员
                            coachedit.putString("coachphoto",fileurl+sfxx.getSfzh()+".jpg");
                            coachedit.commit();
                            isfiled(fileurl+sfxx.getSfzh()+".jpg",sfxx.getSfzh());
                            getJlxx(sfxx);
                        }else {
                            //学员
                            stuedit.putString("stuphoto",fileurl+sfxx.getSfzh()+".jpg");
                            stuedit.commit();
                            isfiled(fileurl+sfxx.getSfzh()+".jpg",sfxx.getSfzh());
                            getXyxx(sfxx);
                        }

                    }else {
                        Log.e("TAG","下载照片失败");
                    }
                }
            };
            registerReceiver(receiver, filter);

        }catch (Exception ex){
            //下载照片失败
            if(type==1){
                //教练员
                getJlxx(sfxx);
            }else {
                //学员
                getXyxx(sfxx);
            }
        }
    }

    /**
     * 初始化人脸识别，保存照片特征值
     * */
    FaceDetJni FaceDet;
    List<FD_FSDKFace> faceResult;
    public void initrlCamera(){
        faceResult = new ArrayList<>();
        FaceDet = new FaceDetJni();
        final Timer initbdtimer = new Timer();
        TimerTask task=new TimerTask() {
            @Override
            public void run() {
                initbdtimer.cancel();
                Log.e("TAG","进行初始化比对");
                landmarks_path =RlsbUtil.getAssetsCacheFile(LoginPhoneActivity.this,"face_landmarks_5_cilab.dat");
                facenet_path = RlsbUtil.getAssetsCacheFile(LoginPhoneActivity.this,"facenet_cilab.dat");
                train_path =RlsbUtil.getAssetsCacheFile(LoginPhoneActivity.this,"complex_training.txt");
                FaceDet.FaceDetInit(landmarks_path, facenet_path, train_path);
            }
        };
        initbdtimer.schedule(task,1000);

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
            Log.e("TAG","获取原始照片轮廓结果=" + ret);
            if(ret==true){
                //成功获取轮廓并保存
                boolean mIsSave = FaceDet.CaptureFaceMuti(mageurl, name);
                Log.e("TAG","保存原始照片结果=" + mIsSave);

                return true;
            }else {
                return false;
            }
        }else {
            return false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //解绑下载照片广播
        if(receiver!=null){
            unregisterReceiver(receiver);
        }

        NettyConf.handlersmap.remove("phonelogin");
    }
}
