package com.dgcheshang.cheji;

import android.app.Application;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.tencent.bugly.crashreport.CrashReport;


/**
 * Created by Administrator on 2017/4/28 0028.
 */
public class CjApplication extends Application {
    private static CjApplication instance;
    private static RequestQueue queue ;
    public static CjApplication getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        CrashReport.initCrashReport(getApplicationContext(), "daf625736b", false);
        instance=this;

        queue = Volley.newRequestQueue(getApplicationContext()); // 实例化RequestQueue对象

    }

    public static RequestQueue getHttpQueue() {
        return queue;
    }

}
