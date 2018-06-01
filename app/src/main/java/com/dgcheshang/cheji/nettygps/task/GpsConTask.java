package com.dgcheshang.cheji.nettygps.task;

import android.util.Log;

import com.dgcheshang.cheji.nettygps.conf.Params;
import com.dgcheshang.cheji.nettygps.init.GpsClient;

import java.util.TimerTask;

/***********************************************
 * @项目名称：cheji-master-3.6 - st
 * @文件名称：ConTask
 * @文件描述：链接服务器任务
 * @文件作者：joxhome
 * @创建时间：2018/5/31 10:14
 ***********************************************/
public class GpsConTask extends TimerTask {
    @Override
    public void run() {
        if(Params.gpsconstate==0){
            GpsClient gpsClient=new GpsClient();
            new Thread(gpsClient).start();
        }else{
            Log.e("TAG","取消");
            this.cancel();
        }
    }
}
