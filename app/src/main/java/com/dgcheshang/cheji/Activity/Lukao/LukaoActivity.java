package com.dgcheshang.cheji.Activity.Lukao;

import android.app.Dialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import com.dgcheshang.cheji.Activity.BaseInitActivity;
import com.dgcheshang.cheji.Database.DbHandle;
import com.dgcheshang.cheji.R;
import com.dgcheshang.cheji.Tools.IsMediaPlayer;
import com.dgcheshang.cheji.Tools.LoadingDialogUtils;
import com.dgcheshang.cheji.netty.conf.NettyConf;
import com.dgcheshang.cheji.netty.po.Line;
import com.dgcheshang.cheji.netty.timer.LineTimerTask;
import com.dgcheshang.cheji.netty.util.LocationUtil;
import com.dgcheshang.cheji.networkUrl.NetworkUrl;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Timer;
import java.util.zip.ZipInputStream;

/**
 * 模拟路考
 * */
public class LukaoActivity extends BaseInitActivity implements View.OnClickListener{
    Context context=LukaoActivity.this;
    //名字数组
    String[] lukaoname=new String[]{"夜间灯光操作一","夜间灯光操作二","夜间灯光操作三","夜间灯光操作四","夜间灯光操作五","夜间灯光操作六","夜间灯光操作七","夜间灯光操作八","夜间灯光操作九","夜间灯光操作十","夜间灯光操作十一","夜间灯光操作十二","上车准备","起步","变更车道","直线行驶","通过公交车站","通过学校区域","通过路口","通过人行横道","会车","超车","掉头","靠边停车","左转","右转","减速让行","禁止鸣笛","通过拱桥","通过急弯坡路","加减档","考试完成"};
   //夜间灯光训练照片
    int[] imageDenguang=new int[]{R.mipmap.lukao_light1,R.mipmap.lukao_light2,R.mipmap.lukao_light3,R.mipmap.lukao_light4,R.mipmap.lukao_light5,R.mipmap.lukao_light6,R.mipmap.lukao_light7,R.mipmap.lukao_light8,R.mipmap.lukao_light9,R.mipmap.lukao_light10,R.mipmap.lukao_light11,R.mipmap.lukao_light12};
    //日常训练照片
    int[] imagerichang=new int[]{R.mipmap.lukao1,R.mipmap.lukao2,R.mipmap.lukao3,R.mipmap.lukao4,R.mipmap.lukao5,R.mipmap.lukao6,R.mipmap.lukao7,R.mipmap.lukao8,R.mipmap.lukao9,R.mipmap.lukao10,R.mipmap.lukao11,R.mipmap.lukao12,R.mipmap.lukao13,R.mipmap.lukao14,R.mipmap.lukao15,R.mipmap.lukao16,R.mipmap.lukao17,R.mipmap.lukao18,R.mipmap.lukao19,R.mipmap.lukao20};
    String file="/sdcard/chejidoal/";//创建的文件夹
    BroadcastReceiver receiver;//下载广播
    String zippath="/mnt/sdcard/chejidoal/chejimusic.zip";//下载后语音保存全路径
    Dialog loading;
    boolean isback=true;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lukao);
        initView();
    }

    /**
     * 初始化布局
     * */
    private void initView() {
        final SharedPreferences lukaosp = getSharedPreferences("lukao", Context.MODE_PRIVATE);
        View layout_light = findViewById(R.id.layout_light);//灯光训练
        View layout_sum = findViewById(R.id.layout_sum);//日常训练
        View layout_gather = findViewById(R.id.layout_gather);//路线采集
        View layout_change = findViewById(R.id.layout_change);//路线管理
        View layout_moni = findViewById(R.id.layout_moni);//模拟考试
        View layout_back = findViewById(R.id.layout_back);//返回
        TextView tv_gps = (TextView) findViewById(R.id.tv_gps);//gps状态
        final CheckBox checkBox = (CheckBox) findViewById(R.id.checkBox);//开启模拟路考
        boolean isstart = lukaosp.getBoolean("isstart", false);//是否开启路考
        checkBox.setChecked(isstart);
        if(isstart==true){
            Line line = getLine();
            if(line!=null){
                startExam(line);
            }
        }
        //获取gps状态
        boolean state = LocationUtil.state;
        if(state){
            tv_gps.setText("正常");
        }else {
            tv_gps.setText("断开");
        }
        //文件不存在则下载文件
        boolean b = fileIsExists(file);
        if(b==false){
            //判断网络是否正常
            if(NettyConf.netstate){
                //正常下载压缩文件
                downFile(NetworkUrl.Chejimusic);
            }else {
                Toast.makeText(context,"暂无网络，无法下载语音文件",Toast.LENGTH_SHORT).show();
            }
        }

        //开启路考选项监听
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked==true){
                    Line line = getLine();
                    if(line!=null){
                        startExam(line);
                    }else {
                        checkBox.setChecked(false);
                        isChecked=false;
                    }
                }else {
                    if(NettyConf.xltimer!=null){
                        IsMediaPlayer.isRelease();
                        NettyConf.xltimer.cancel();
                        NettyConf.xltimer=null;
                    }
                }
                SharedPreferences.Editor edit = lukaosp.edit();
                edit.putBoolean("isstart",isChecked);
                edit.commit();

            }
        });
        layout_light.setOnClickListener(this);
        layout_sum.setOnClickListener(this);
        layout_back.setOnClickListener(this);
        layout_gather.setOnClickListener(this);
        layout_change.setOnClickListener(this);
        layout_moni.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        Intent intent = new Intent();
        switch (v.getId()){

            case R.id.layout_back://返回
                finish();
                break;

            case R.id.layout_moni://模拟考试
                intent.setClass(context,LukaoExamActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra("list", lukaoname);
                intent.putExtra("imagelist",imagerichang);
                startActivity(intent);
                break;

            case R.id.layout_sum://日常训练
                intent.setClass(context,LukaoListActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra("title","日常训练");
                intent.putExtra("list", lukaoname);
                intent.putExtra("imagelist",imagerichang);
                startActivity(intent);
                break;

            case R.id.layout_light://灯光训练
                intent.setClass(context,LukaoListActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra("title","灯光训练");
                intent.putExtra("list", lukaoname);
                intent.putExtra("imagelist",imageDenguang);
                startActivity(intent);
                break;

            case R.id.layout_gather://路线采集
                intent.setClass(context,LukaoGatherActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra("list",lukaoname);
                intent.putExtra("imagerichang",imagerichang);
                startActivity(intent);
                break;

            case R.id.layout_change://路线管理
                intent.setClass(context,LukaoChangeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra("list",lukaoname);
                intent.putExtra("imagelist",imagerichang);
                startActivity(intent);
                break;
        }
    }

    /**
     * 判断文件夹是否存在
     * */
    public boolean fileIsExists(String strFile)
    {
        try
        {
            File f=new File(strFile);
            if(!f.exists()||f.list().length<32)//判断是否存在文件夹，并且里面文件个数=32个
            {
                File file = new File(strFile + "chejimusic.zip");
                if(!file.exists()){
                    //文件不存在则下载文件
                    return false;
                }else {
                    //文件存在则解压文件
                    Unzip(zippath, "/sdcard/chejidoal/");//解压zip文件
                    return true;
                }

            }

        }
        catch (Exception e)
        {
            return false;
        }

        return true;
    }

    /**
     * 下载语音文件
     * */
    public void downFile(String url) {
        isback=false;
        loading = LoadingDialogUtils.createLoadingDialog(context, "语音初始化中...");
        loading.setCancelable(false);//设置不能按返回键取消
        File destDir = new File("/cdcard/chejidoal");
        //先判断是否有之前下载的文件，有则删除，
        if (!destDir.exists()) {
            destDir.mkdirs();
        } else {
            File[] files = destDir.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isFile()) {
                    File appfile = new File(files[i].getPath());
                    appfile.delete();
                }
            }
            destDir.mkdirs();
        }
        try {
            final DownloadManager dManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            Uri uri = Uri.parse(url);
            DownloadManager.Request request = new DownloadManager.Request(uri);
            // 设置下载路径和文件名
            request.setDestinationInExternalPublicDir("chejidoal", "chejimusic.zip");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
            request.setDescription("语音文件正在下载");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
//        request.setMimeType("application/vnd.android.package-archive");
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

                        Unzip(zippath, "/sdcard/chejidoal/");//解压zip文件

                    }
                }
            };
            registerReceiver(receiver, filter);
        }catch (Exception ex){
            loading.cancel();
            Toast.makeText(context,"语音文件下载失败",Toast.LENGTH_SHORT).show();
            isback=true;
        }

    }

    /**
     * 解压语音文件
     * zipFile要解压的文件全路径/mnt/sdcard/chejidoal/chejimusic.zip
     * targetDir解压后保存的路径/mnt/sdcard/chejidoal/
     * */
    public  void Unzip(String zipFile, String targetDir) {
        int BUFFER = 4096; //这里缓冲区我们使用4KB，
        String strEntry; //保存每个zip的条目名称
        try {
            BufferedOutputStream dest = null; //缓冲输出流
            FileInputStream fis = new FileInputStream(zipFile);
            ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));
            java.util.zip.ZipEntry entry; //每个zip条目的实例
            while ((entry = zis.getNextEntry()) != null) {
                try {
                    Log.i("Unzip: ", "=" + entry);
                    int count;
                    byte data[] = new byte[BUFFER];
                    strEntry = entry.getName();
                    File entryFile = new File(targetDir + strEntry);
                    File entryDir = new File(entryFile.getParent());
                    if (!entryDir.exists()) {
                        entryDir.mkdirs();
                    }
                    FileOutputStream fos = new FileOutputStream(entryFile);
                    dest = new BufferedOutputStream(fos, BUFFER);
                    while ((count = zis.read(data, 0, BUFFER)) != -1) {
                        dest.write(data, 0, count);
                    }
                    dest.flush();
                    dest.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            zis.close();
            //解压完后删除zip文件
            delFile(zipFile);
            loading.cancel();
            isback=true;
        } catch (Exception cwj) {
            cwj.printStackTrace();
        }
    }

    /**
     * 删除zip文件
     * */
    public static void delFile(String fileName){
        File file = new File(fileName);
        if(file.isFile()){
            file.delete();
        }
        file.exists();
    }

    /**
     * 获取保存选择的第几条线路
     * */
    public Line getLine(){
        String sql="select * from line";
        final String[] params=null;
        final ArrayList<Line> list = DbHandle.queryline(sql, params);
        SharedPreferences lukaosp = getSharedPreferences("lukao", Context.MODE_PRIVATE);
        String linename = lukaosp.getString("linename", "");
        if(!linename.isEmpty()&&list.size()>0){
            Line line=null;
            for(int i =0;i<list.size();i++){
                String mc = list.get(i).getMc();
                if(mc.equals(linename)){
                    return list.get(i);
                }
            }
            return line;
        }else {
            Toast.makeText(context,"请先选择模拟考试路线",Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    /**
     * 开启路考
     * */
    public void startExam(Line line){
        if(NettyConf.xltimer!=null){
            NettyConf.xltimer.cancel();
            NettyConf.xltimer=null;
        }
        NettyConf.line=line;
        NettyConf.xltimer = new Timer();
        LineTimerTask lineTask = new LineTimerTask(false);
        NettyConf.xltimer.schedule(lineTask,0,1000);
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
