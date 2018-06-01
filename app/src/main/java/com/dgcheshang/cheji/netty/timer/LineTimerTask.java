package com.dgcheshang.cheji.netty.timer;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.dgcheshang.cheji.Tools.IsMediaPlayer;
import com.dgcheshang.cheji.netty.conf.NettyConf;
import com.dgcheshang.cheji.netty.util.ZdUtil;

import org.apache.commons.lang3.StringUtils;

import java.util.TimerTask;

/**
 * Created by Administrator on 2017/9/19.
 */

public class LineTimerTask extends TimerTask {
    boolean isexam;
    public LineTimerTask(boolean isexam) {
        this.isexam=isexam;
    }

    @Override
    public void run() {
        String type=ZdUtil.xlSpeakMsg();
        String[] ss=type.split(",");
        for(String s:ss){
            if(StringUtils.isNotEmpty(s)){
                Log.e("TAG","返回类型："+type);
                String musicurl="/sdcard/chejidoal/lukao"+(Integer.parseInt(s)+1)+".ogg";
                if(isexam==true){
                    Message msg = new Message();
                    msg.arg1=1;
                    Bundle bundle = new Bundle();
                    bundle.putString("type",s);
                    msg.setData(bundle);
                    Handler handler = (Handler) NettyConf.handlersmap.get("startexam");
                    if(handler!=null){
                        handler.sendMessage(msg);
                    }
                }
                IsMediaPlayer.isplay(musicurl);
            }
        }

    }
}
