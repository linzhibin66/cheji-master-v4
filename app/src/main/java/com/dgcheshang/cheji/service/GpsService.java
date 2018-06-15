package com.dgcheshang.cheji.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;

import com.dgcheshang.cheji.nettygps.conf.Params;
import com.dgcheshang.cheji.nettygps.task.GpsSendTask;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Administrator on 2018/6/5 0005.
 */

public class GpsService extends Service {

    private PowerManager pm;
    private PowerManager.WakeLock wakeLock;
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onStart(Intent intent, int startId) {
        // TODO Auto-generated method stub
        super.onStart(intent, startId);
        //创建PowerManager对象
        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        //保持cpu一直运行，不管屏幕是否黑屏
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CPUKeepRunning");
        wakeLock.acquire();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        TimerTask timer=new GpsSendTask();
        Timer gpsT=new Timer();
        gpsT.schedule(timer,5*1000, Params.gpsjg*1000);
    }
}
