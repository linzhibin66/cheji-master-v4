package com.dgcheshang.cheji.Activity;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.InputType;
import android.util.Log;
import android.os.RemoteException;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.dgcheshang.cheji.R;
import com.dgcheshang.cheji.Tools.LoadingDialogUtils;
import com.dgcheshang.cheji.Tools.Speaking;
import com.dgcheshang.cheji.netty.conf.NettyConf;
import com.dgcheshang.cheji.netty.serverreply.DeviceInfo;
import com.dgcheshang.cheji.netty.timer.CardTimer;
import com.dgcheshang.cheji.netty.timer.LoadingTimer;
import com.dgcheshang.cheji.netty.util.GatewayService;
import com.dgcheshang.cheji.netty.util.ZdUtil;
import com.dgcheshang.cheji.nettygps.conf.Params;
import com.dgcheshang.cheji.nettygps.util.GpsUtil;
import com.rscja.customservices.ICustomServices;
import com.rscja.deviceapi.RFIDWithISO14443A;

import org.apache.commons.lang3.StringUtils;
import java.util.List;
import java.util.Timer;

import io.netty.channel.ChannelHandlerContext;


/**
 * 端口IP连接
 * */
public class MainActivity extends BaseInitActivity  implements View.OnClickListener{

    public Context context=MainActivity.this;

    public static final int SETTING_SUCCESS = 0;//设置成功后
    public static boolean isGPS=false;//判断GPS是否启动
    public final int SETTING=0;
    EditText edt_sheng,edt_shi,edt_carnumb,edt_phonenumb,edt_duankou,edt_ip;
    EditText edt_gps_ip,edt_gps_duankou;
    String[] carcolor=new String[]{"请选择","蓝色","黄色","黑色","白色","绿色","其他"};//车牌颜色：1:蓝色 2:黄色 3:黑色 4:白色 5:绿色 9:其他

    String[] carnumber=new String[]{"粤","京","津","冀","晋","蒙","辽","吉","黑","沪","苏","浙","皖","闽","赣","鲁","豫","鄂","湘","桂","琼","渝","川","贵","云","藏","陕","甘","青","宁","新"};
    int color=0;
    int jianchen=0;//省份简称
    String uid;
    TextView tv_show_uid;
    View layout_admin;
    Spinner sp_carcolor,sp_carnumber;
    RFIDWithISO14443A mRFID;
    Button bt_sure,bt_cancel,bt_have_set,bt_init,bt_gps_sure,bt_gps_cancel;
    SharedPreferences.Editor editor;
    private Dialog loading;
    LoadingTimer loadingTimer;
    Timer timer;
    ICustomServices mCustomServices;
    private  Handler handler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            if(msg.arg1==1){
                //鉴权
                jqHmandle(msg);

            }else if(msg.arg1==2){
                //注销
                handleCancel(msg);
            }else if(msg.arg1==6){
                if(NettyConf.cardtimer!=null){
                    NettyConf.cardtimer.cancel();
                    NettyConf.cardtimer=null;
                }
                //刷卡成功后返回uid
                String adminuid = msg.getData().getString("adminuid");
                tv_show_uid.setVisibility(View.VISIBLE);
                tv_show_uid.setText("UID:"+adminuid);
                adminLogin(adminuid);

            }else if(msg.arg1==7){
                //注册失败
                if(loadingTimer!=null) {
                    loadingTimer.cancel();
                }
                if(timer!=null) {
                    timer.cancel();
                }
                LoadingDialogUtils.closeDialog(loading);
            }else if (msg.arg1==10){
                //强制登出返回回来处理
                int yzjg = msg.getData().getInt("yzjg",1);
                if(yzjg==0){
                    LoadingDialogUtils.closeDialog(loading);
                    edt_ip.setInputType(InputType.TYPE_CLASS_TEXT);
                    edt_duankou.setInputType(InputType.TYPE_CLASS_TEXT);
                    edt_gps_ip.setInputType(InputType.TYPE_CLASS_TEXT);
                    edt_gps_duankou.setInputType(InputType.TYPE_CLASS_TEXT);
                    edt_sheng.setInputType(InputType.TYPE_CLASS_TEXT);
                    edt_shi.setInputType(InputType.TYPE_CLASS_TEXT);
                    edt_carnumb.setInputType(InputType.TYPE_CLASS_TEXT);
                    edt_phonenumb.setInputType(InputType.TYPE_CLASS_TEXT);
                    sp_carcolor.setClickable(true);
                    sp_carnumber.setClickable(true);
                    sp_carnumber.setEnabled(true);
                    sp_carcolor.setEnabled(true);
                    layout_admin.setVisibility(View.VISIBLE);
                }else {
                    loading.cancel();
                    Speaking.in("密码验证失败");
                }
            }else if(msg.arg1==11){
                //获取参数设置
                DeviceInfo deviceInfo = (DeviceInfo) msg.getData().get("deviceInfo");
                edt_phonenumb.setText(deviceInfo.getMobile());
                int cpys = Integer.parseInt(deviceInfo.getCpys());//车牌颜色
                if(cpys!=9){
                    sp_carcolor.setSelection(cpys, true);
                }else {
                    sp_carcolor.setSelection(6, true);
                }
                color=cpys;
                String cp = deviceInfo.getCp();//车牌号
                String jian = cp.substring(0, 1);//获取简称
                for(int i =0;i<carnumber.length;i++){
                    if(carnumber[i].equals(jian)){
                        jianchen=i;
                        sp_carnumber.setSelection(i,true);
                    }
                }
                String s=cp.substring(1);//去掉第一个
                String s1=s.substring(0,s.length()-1);
                edt_carnumb.setText(s1);

                if(StringUtils.isNotEmpty(deviceInfo.getModel())){
                    NettyConf.model=deviceInfo.getModel();
                }
                if(StringUtils.isNotEmpty(deviceInfo.getTermno())){
                    NettyConf.termno=deviceInfo.getTermno();
                }

                if(StringUtils.isNotEmpty(deviceInfo.getSyid())){
                    NettyConf.shengID=deviceInfo.getSyid();
                    edt_sheng.setText(deviceInfo.getSyid());
                }
                if(StringUtils.isNotEmpty(deviceInfo.getSxyid())){
                    NettyConf.shiID=deviceInfo.getSxyid();
                    edt_shi.setText(deviceInfo.getSxyid());
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        NettyConf.handlersmap.put("main",handler);
        initView();

        CardTimer.isstop=false;
        if(NettyConf.cardtimer!=null){
            NettyConf.cardtimer.cancel();
            NettyConf.cardtimer=null;
        }
        try {
            mRFID = RFIDWithISO14443A.getInstance();
        } catch (Exception e) {
        }

    }

    /**
     * 初始化布局
     * */
    private void initView() {
        SharedPreferences jianquan = getSharedPreferences("jianquan", Context.MODE_PRIVATE);
        editor = jianquan.edit();
        //管理员卡号
        SharedPreferences uidsp = getSharedPreferences("uid", Context.MODE_PRIVATE);
        uid = uidsp.getString("uid", NettyConf.uid);

        View layout_back = findViewById(R.id.layout_back);//返回按钮
        tv_show_uid = (TextView) findViewById(R.id.tv_show_uid);//uid显示
        layout_admin = findViewById(R.id.layout_admin);//管理员布局显示
        bt_sure = (Button) findViewById(R.id.bt_sure);//确定按钮
        bt_cancel = (Button) findViewById(R.id.bt_cancel);//注销按钮
        bt_init = (Button) findViewById(R.id.bt_init);//初始化按钮
        bt_gps_sure = (Button) findViewById(R.id.bt_gps_sure);//gps连接按钮
        bt_gps_cancel = (Button) findViewById(R.id.bt_gps_cancel);//gps注销按钮
        bt_have_set = (Button) findViewById(R.id.bt_have_set);//获取设置权限
        edt_ip = (EditText) findViewById(R.id.edt_ip);//IP
        edt_duankou = (EditText) findViewById(R.id.edt_duankou);//端口
        edt_gps_ip = (EditText) findViewById(R.id.edt_gps_ip);//gps_ip
        edt_gps_duankou = (EditText) findViewById(R.id.edt_gps_duankou);//gps端口
        edt_sheng = (EditText) findViewById(R.id.edt_sheng);//省域
        edt_shi = (EditText) findViewById(R.id.edt_shi);//市域
        edt_carnumb = (EditText) findViewById(R.id.edt_carnumb);//车牌号
        edt_phonenumb = (EditText) findViewById(R.id.edt_phonenumb);//手机号
        sp_carcolor = (Spinner) findViewById(R.id.Spinner_carcolor);//车牌颜色
        sp_carnumber = (Spinner) findViewById(R.id.spinner_carnumber);//车牌省简称
        tv_show_uid.setVisibility(View.INVISIBLE);
        layout_admin.setVisibility(View.INVISIBLE);
        edt_ip.setInputType(InputType.TYPE_NULL);
        edt_duankou.setInputType(InputType.TYPE_NULL);
        edt_sheng.setInputType(InputType.TYPE_NULL);
        edt_shi.setInputType(InputType.TYPE_NULL);
        edt_carnumb.setInputType(InputType.TYPE_NULL);
        edt_phonenumb.setInputType(InputType.TYPE_NULL);
        edt_gps_ip.setInputType(InputType.TYPE_NULL);
        edt_gps_duankou.setInputType(InputType.TYPE_NULL);
        ArrayAdapter<String> numAdapter=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item, carnumber);
        numAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp_carnumber.setAdapter(numAdapter);
        ArrayAdapter<String> colorAdapter=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item, carcolor);
        colorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp_carcolor.setAdapter(colorAdapter);
        sp_carcolor.setEnabled(false);
        sp_carnumber.setEnabled(false);
        sp_carcolor.setClickable(false);
        sp_carnumber.setClickable(false);
        //赋值

        if(NettyConf.host.equals("0")||NettyConf.port==0){
            edt_ip.setText("");
            edt_duankou.setText("");
        }else {
            edt_ip.setText(NettyConf.host);
            edt_duankou.setText(String.valueOf(NettyConf.port));
        }
        edt_gps_ip.setText(Params.gpshost);
        edt_gps_duankou.setText(Params.gpsport+"");
        edt_sheng.setText(NettyConf.shengID);
        edt_shi.setText(NettyConf.shiID);
        edt_phonenumb.setText(NettyConf.mobile);
        if(NettyConf.jqstate==1){
            bt_sure.setText("已连接");
        }
        edt_phonenumb.setText(NettyConf.mobile);
        if(!NettyConf.cp.equals("")){
            String jian = NettyConf.cp.substring(0, 1);
            for(int i =0;i<carnumber.length;i++){
                if(carnumber[i].equals(jian)){
                    jianchen=i;
                    sp_carnumber.setSelection(i,true);
                }
            }
            String s=NettyConf.cp.substring(1);//去掉第一个
            String s1=s.substring(0,s.length()-1);
            edt_carnumb.setText(s1);
        }
        if(!NettyConf.cpys.equals("")){
            int cpys = Integer.valueOf(NettyConf.cpys);
            //车牌赋值
            if(cpys!=9){
                sp_carcolor.setSelection(cpys, true);
            }else {
                sp_carcolor.setSelection(6, true);
            }
            color=cpys;
        }
        //车牌颜色选项监听
        sp_carcolor.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(position==6){
                    color=9;
                }else {
                    color = position;
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        //车牌简称选项监听
        sp_carnumber.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                jianchen=position;
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        bt_sure.setOnClickListener(this);
        layout_back.setOnClickListener(this);
        bt_cancel.setOnClickListener(this);
        bt_have_set.setOnClickListener(this);
        bt_init.setOnClickListener(this);
        bt_gps_cancel.setOnClickListener(this);
        bt_gps_sure.setOnClickListener(this);
    }

    /**
     * 点击监听
     * */
    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.bt_sure://确定
                //判断是否进行设置
                String sheng = edt_sheng.getText().toString().trim();
                String shi = edt_shi.getText().toString().trim();
                String carnumb = edt_carnumb.getText().toString().trim();
                String host = edt_ip.getText().toString().trim();
                String duankou = edt_duankou.getText().toString().trim();
                String phonenumb = edt_phonenumb.getText().toString().trim();
                String gps_ip = edt_gps_ip.getText().toString().trim();
                String gps_duankou = edt_gps_duankou.getText().toString().trim();
                if(carnumb.equals("")||sheng.equals("")||shi.equals("")||host.equals("")||duankou.equals("")||phonenumb.equals("")||color==0){
                    Toast.makeText(context,"信息未填写完整",Toast.LENGTH_SHORT).show();
                }else {
                    //保存车辆信息
                    SharedPreferences zdcssp = context.getSharedPreferences("zdcs", Context.MODE_PRIVATE);
                    SharedPreferences.Editor edit = zdcssp.edit();
                    String jian = carnumber[jianchen].trim();
                    edit.putString("0083",jian+carnumb+"学");//车牌号码
                    edit.putString("0081",sheng);//省域
                    edit.putString("0082",shi);//市域
                    edit.putString("0084",color+"");//车牌颜色
                    edit.putString("0048",phonenumb);//电话号码
                    edit.putString("0013",host);//ip
                    edit.putString("0018",duankou);//ip

                    edit.putString("model",NettyConf.model);
                    edit.putString("termno",NettyConf.termno);
                    edit.putString("0081",NettyConf.shengID);
                    edit.putString("0082",NettyConf.shiID);
                    edit.putString("0017",gps_ip);
                    edit.putString("0019",gps_duankou);
                    edit.commit();

                    NettyConf.cp=jian+carnumb+"学";
                    NettyConf.cpys=color+"";
                    NettyConf.shengID=sheng;
                    NettyConf.shiID=shi;
                    NettyConf.mobile=phonenumb;
                    NettyConf.host=host;
                    if(StringUtils.isNotEmpty(duankou)) {
                        NettyConf.port = Integer.valueOf(duankou);
                    }
                    loading = LoadingDialogUtils.createLoadingDialog(context, "正在连接...");
                    loadingTimer = new LoadingTimer(loading);
                    timer = new Timer();
                    timer.schedule(loadingTimer,NettyConf.controltime);
                    putData();
                }

                break;

            case R.id.layout_back://返回
                setHomekeylock();
                finish();
                break;

            case R.id.bt_cancel://注销

                if(NettyConf.xystate==0&&NettyConf.jlstate==0){//判断是否学员跟教练员都登出
                    showCancelDialog("1","您确定要注销连接吗？");
                }else {
                    Toast.makeText(context,"当前还有未登出状态，请先登出",Toast.LENGTH_SHORT).show();
                }

                break;
            case R.id.bt_have_set://可以设置
                haveSetDialog();
                break;

            case R.id.bt_init://获取下拉参数
                ZdUtil.getDeviceInfo();
                break;

            case R.id.bt_gps_sure://开启gps连接
                Object o=GatewayService.getGatewayChannel("sptChannel");
                if(o!=null){
                    ChannelHandlerContext clientChannel = (ChannelHandlerContext) o;
                    clientChannel.close();
                }else {
                    GpsUtil.conServer();
                }
                break;

            case R.id.bt_gps_cancel://注销gps连接
                showCancelDialog("2","您确定要注销GPS连接吗？");
                break;
        }
    }



    /**
     * 连接接口
     * */
    private void putData() {
        Object o=GatewayService.getGatewayChannel("serverChannel");
        if(o!=null){
            ChannelHandlerContext clientChannel = (ChannelHandlerContext) o;
            clientChannel.close();
        }else {
            ZdUtil.conServer();
        }
    }

    /**
     * 判断服务是否启动,context上下文对象 ，className服务的name
     **/
    public static boolean isServiceRunning(Context mContext, String className) {

        boolean isRunning = false;
        ActivityManager activityManager = (ActivityManager) mContext
                .getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> serviceList = activityManager
                .getRunningServices(30);

        if (!(serviceList.size() > 0)) {
            return false;
        }

        for (int i = 0; i < serviceList.size(); i++) {
            if (serviceList.get(i).service.getClassName().equals(className) == true) {
                isRunning = true;
                break;
            }
        }
        return isRunning;
    }

    /**
     * 控制返回键无效
     * */
    public boolean onKeyDown(int keyCode,KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0){
//            //这里重写返回键
//            return true;
//            showOutDialog();
        }
        return false;

    }
    /**
     * 获取设置权限Dialog
     * */
    private void haveSetDialog() {
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
        tv_title.setText("密码验证");

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
                String yzmm = edt_content.getText().toString().trim();
                if(!yzmm.equals("")){
                    builder.dismiss();
                    loading = LoadingDialogUtils.createLoadingDialog(context, "正在验证...");
                    ZdUtil.matchPassword(9,yzmm);


                }else {
                    Toast.makeText(context,"请输入验证密码",Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    /**
     * 退出dialog
     * */
    private void showOutDialog(){
        final AlertDialog.Builder normalDialog = new AlertDialog.Builder(context);
        normalDialog.setTitle("提示");
        normalDialog.setMessage("您确定要退出本应用吗？");
        normalDialog.setPositiveButton("确定",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();

                        int currentVersion = android.os.Build.VERSION.SDK_INT;
                        if (currentVersion > android.os.Build.VERSION_CODES.ECLAIR_MR1) {
                            Intent startMain = new Intent(Intent.ACTION_MAIN);
                            startMain.addCategory(Intent.CATEGORY_HOME);
                            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(startMain);
                            System.exit(0);
                        } else {// android2.1
                            android.app.ActivityManager am = (android.app.ActivityManager) getSystemService(ACTIVITY_SERVICE);
                            am.restartPackage(getPackageName());
                        }
                    }
                });
        normalDialog.setNegativeButton("取消",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        // 显示
        normalDialog.show();
    }

    /**
     * 注销dialog
     * */
    private void showCancelDialog(final String type, String msg){
        final AlertDialog.Builder normalDialog = new AlertDialog.Builder(context);
        normalDialog.setTitle("提示");
        normalDialog.setMessage(msg);
        normalDialog.setPositiveButton("确定",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if(type.equals("1")){
                            ZdUtil.sendZdzx();
                        }else {
                            //注销gps
                            GpsUtil.sendZdzx();
                        }

                    }
                });
        normalDialog.setNegativeButton("取消",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        // 显示
        normalDialog.show();
    }

    /**
     * 鉴权返回来处理
     * */
    public void jqHmandle(Message msg){
        int jqjg = msg.getData().getInt("jqjg");
        if(jqjg==0){
            bt_sure.setText("已连接");
        }
        if(loadingTimer!=null) {
            loadingTimer.cancel();
        }
        if(timer!=null) {
            timer.cancel();
        }
        LoadingDialogUtils.closeDialog(loading);
    }

    /**
     * 注销返回处理
     *
     * @param msg*/
    public void handleCancel(Message msg){

        Bundle data = msg.getData();
        int zxjg = (int) data.get("zxjg");
        if(zxjg==0){//注销成功
            bt_sure.setText("连接");
        }
    }

    /**
     * 管理人员登录进行设置
     *
     * @param adminuid*/
    public void adminLogin(String adminuid){
        if(!StringUtils.isEmpty(uid)){
            if(uid.contains(adminuid)){
                layout_admin.setVisibility(View.VISIBLE);
                NettyConf.background=1;//刷管理员卡，改变后台监听状态
                edt_ip.setInputType(InputType.TYPE_CLASS_TEXT);
                edt_duankou.setInputType(InputType.TYPE_CLASS_TEXT);
                edt_gps_duankou.setInputType(InputType.TYPE_CLASS_TEXT);
                edt_gps_ip.setInputType(InputType.TYPE_CLASS_TEXT);
                edt_sheng.setInputType(InputType.TYPE_CLASS_TEXT);
                edt_shi.setInputType(InputType.TYPE_CLASS_TEXT);
                edt_carnumb.setInputType(InputType.TYPE_CLASS_TEXT);
                edt_phonenumb.setInputType(InputType.TYPE_CLASS_TEXT);
                sp_carcolor.setClickable(true);
                sp_carnumber.setClickable(true);
                sp_carnumber.setEnabled(true);
                sp_carcolor.setEnabled(true);
                setHomekey();
                return;
            }else{
                Speaking.in("此卡无管理员权限");
            }
        }else{
            Speaking.in("此卡无效");
        }
    }

    /**
     * 释放home键跟菜单键，及下拉通知栏功能
     * */
    boolean b;
    public void setHomekey(){
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
                mCustomServices.setLauncher("com.dgcheshang.cheji","MainActivity");
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            //禁用
            try {
                if (mCustomServices != null) {
                    //启用/禁用home键
                    mCustomServices.setHomeKeyEnable(true);
                    //启用/禁用多任务键
                    mCustomServices.setAppSwitchKeyEnable(true);
                    //禁止下拉菜单
                    mCustomServices.setStatusBarDown(true);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
        }
    };

    boolean b2;
    public void setHomekeylock(){
        if(b){
            unbindService(mServiceConnection);
            b=false;
        }

        Intent intentCust = new Intent();
        intentCust.setAction("com.rscja.CustomService");
        intentCust.setPackage("com.rscja.customservices");
        b2 = bindService(intentCust, mServiceConnectionlock, Context.BIND_AUTO_CREATE);
    }

    ServiceConnection mServiceConnectionlock = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mCustomServices = ICustomServices.Stub.asInterface(iBinder);
            try {
                mCustomServices.setLauncher("com.dgcheshang.cheji","MainActivity");
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            //禁用
            try {
                if (mCustomServices != null) {
                    //启用/禁用home键
                    mCustomServices.setHomeKeyEnable(false);
                    //启用/禁用多任务键
                    mCustomServices.setAppSwitchKeyEnable(false);
                    //禁止下拉菜单
                    mCustomServices.setStatusBarDown(false);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        //刷卡
        mRFID.init();

        if(NettyConf.cardtimer!=null){
            NettyConf.cardtimer.cancel();
            NettyConf.cardtimer=null;
        }

        CardTimer cardTimer=new CardTimer(mRFID,"admincard");
        NettyConf.cardtimer=new Timer();
        NettyConf.cardtimer.schedule(cardTimer,300,2000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(NettyConf.debug){
            Log.e("TAG","ondestory");
        }

        NettyConf.handlersmap.remove("main");
        if(timer!=null){
            timer.cancel();
        }
        if(NettyConf.cardtimer!=null){
            NettyConf.cardtimer.cancel();
            NettyConf.cardtimer=null;
        }
        if(mRFID!=null){
            mRFID.free();
        }
        if(b){
            unbindService(mServiceConnection);
        }

        if(b2){
            unbindService(mServiceConnectionlock);
        }

        NettyConf.background=0;//关闭页面改变后台监听状态
    }
}
