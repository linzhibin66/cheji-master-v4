package com.dgcheshang.cheji.nettygps.task;

import android.util.Log;

import com.dgcheshang.cheji.Database.DbHandle;
import com.dgcheshang.cheji.netty.conf.NettyConf;
import com.dgcheshang.cheji.netty.po.Tdata;
import com.dgcheshang.cheji.netty.util.ByteUtil;
import com.dgcheshang.cheji.netty.util.ForwardUtil;
import com.dgcheshang.cheji.netty.util.GatewayService;
import com.dgcheshang.cheji.netty.util.LocationUtil;
import com.dgcheshang.cheji.nettygps.conf.Params;
import com.dgcheshang.cheji.nettygps.util.GpsMsgUtilClient;
import com.dgcheshang.cheji.nettygps.util.GpsUtil;

import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.TimerTask;

/***********************************************
 * @项目名称：cheji-master-3.6 - st
 * @文件名称：GpsSendTask
 * @文件描述：
 * @文件作者：joxhome
 * @创建时间：2018/5/31 16:45
 ***********************************************/
public class GpsSendTask extends TimerTask {
    @Override
    public void run() {
        if(LocationUtil.state&& Params.gpsjqstate==1) {
            String gnss = GpsUtil.getGnss();
            if(StringUtils.isNotEmpty(gnss)) {
                byte[] b2 = ByteUtil.hexStringToByte(gnss);
                List<Tdata> list = GpsMsgUtilClient.generateMsg(b2, "0200", GpsMsgUtilClient.generateLsh(), NettyConf.mobile, "0");
                Log.e("TAG","转发GPS");
                GatewayService.sendHexMsgToServer("gpsChannel", list);
            }
        }
    }
}
