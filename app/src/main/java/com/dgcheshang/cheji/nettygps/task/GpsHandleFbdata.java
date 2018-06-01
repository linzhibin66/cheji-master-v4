package com.dgcheshang.cheji.nettygps.task;

import com.dgcheshang.cheji.nettygps.conf.Params;

import java.util.TimerTask;

/**
 * Created by Administrator on 2017/5/18.
 */

public class GpsHandleFbdata extends TimerTask {
    private String key;

    public GpsHandleFbdata(String key) {
        this.key = key;
    }

    @Override
    public void run() {
        Params.fbdata.remove(key);
    }
}
