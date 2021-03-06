package com.dgcheshang.cheji.netty.timer;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.dgcheshang.cheji.Tools.Speaking;
import com.dgcheshang.cheji.netty.conf.NettyConf;
import com.rscja.deviceapi.Fingerprint;
import com.rscja.deviceapi.exception.ConfigurationException;

import org.apache.commons.lang3.StringUtils;

import java.util.TimerTask;

/**
 * Created by Administrator on 2017/6/23 0023.
 */

public class FrinterTimer extends TimerTask {
    public static boolean ispp=false;//是否在匹配
    private Fingerprint mFingerprint;
    private String type;

    public FrinterTimer(Fingerprint mFingerprint,String type) {
        this.mFingerprint = mFingerprint;
        this.type = type;
    }

    @Override
    public void run() {
        if(!ispp) {
            ispp = true;
            if (!mFingerprint.getImage()) {
                if(NettyConf.debug){
                    Log.e("TAG","获取指纹失败");
                }
                ispp = false;
                return;
            }
            if (mFingerprint.genChar(Fingerprint.BufferEnum.B1)) {
               /* String temp2 = mFingerprint.upChar(Fingerprint.BufferEnum.B1);
                if(NettyConf.debug){
                    Log.e("TAG", "buffer2::"+temp2);
                }*/
                int searchScore = -1;
                int exeCount = 0;
                //int[] result=null;
                do {
                    exeCount++;
                    //检索缓冲区数据
                    //result=mFingerprint.search(Fingerprint.BufferEnum.B1,0,1000);
                    searchScore=mFingerprint.match();
                } while (searchScore==-1 && exeCount < 3);

                if (searchScore!=-1) {
                    //searchScore=result[1];
                    if(NettyConf.debug) {
                        //Log.e("TAG", "id：" + result[0]);
                        Log.e("TAG", "分数：" + searchScore);
                    }
                    mFingerprint.free();

                    this.cancel();
                    NettyConf.fringerTimer=null;

                    Message msg=new Message();

                    if(type.equals("jlcard")){
                        msg.arg1=7;
                        Handler handler = (Handler) NettyConf.handlersmap.get("logincoach");
                        handler.sendMessage(msg);
                    }else if(type.equals("xycard")){
                        msg.arg1=7;
                        Handler handler = (Handler) NettyConf.handlersmap.get("loginstudent");
                        handler.sendMessage(msg);
                    }else if(type.equals("xycardout")){
                        Speaking.in("验证成功");
                        msg.arg1=9;
                        Handler handler = (Handler) NettyConf.handlersmap.get("loginstudent");
                        handler.sendMessage(msg);
                    }else if(type.equals("jlcardout")){
                        Speaking.in("验证成功");
                        msg.arg1=9;
                        Handler handler = (Handler) NettyConf.handlersmap.get("logincoach");
                        handler.sendMessage(msg);
                    }
                } else {
                    Speaking.in("匹配失败");
                    ispp = false;
                }

            }
        }
    }
}
