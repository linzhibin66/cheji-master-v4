package com.dgcheshang.cheji.nettygps.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.util.Log;
import android.widget.Toast;

import com.dgcheshang.cheji.CjApplication;
import com.dgcheshang.cheji.netty.certificate.Sign;
import com.dgcheshang.cheji.netty.conf.NettyConf;
import com.dgcheshang.cheji.netty.po.Gnss;
import com.dgcheshang.cheji.netty.po.GnssExtend;
import com.dgcheshang.cheji.netty.po.Tdata;
import com.dgcheshang.cheji.netty.po.Zdjq;
import com.dgcheshang.cheji.netty.po.Zdzc;
import com.dgcheshang.cheji.netty.timer.ConTimer;
import com.dgcheshang.cheji.netty.timer.DzwlTimerTask;
import com.dgcheshang.cheji.netty.util.AccUtil;
import com.dgcheshang.cheji.netty.util.ByteUtil;
import com.dgcheshang.cheji.netty.util.CountDistance;
import com.dgcheshang.cheji.netty.util.GatewayService;
import com.dgcheshang.cheji.netty.util.LocationUtil;
import com.dgcheshang.cheji.netty.util.MsgUtilClient;
import com.dgcheshang.cheji.netty.util.ZdUtil;
import com.dgcheshang.cheji.nettygps.conf.Params;
import com.dgcheshang.cheji.nettygps.po.GpsGnss;
import com.dgcheshang.cheji.nettygps.po.GpsGnssExtend;
import com.dgcheshang.cheji.nettygps.po.GpsZdzc;
import com.dgcheshang.cheji.nettygps.task.GpsConTask;

import org.apache.commons.lang3.StringUtils;

import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Timer;

public class GpsUtil {

    /**
     * 终端注册
     */
    public static void sendZdzc(){
        try {
            if (StringUtils.isBlank(NettyConf.mobile) || StringUtils.isBlank(NettyConf.cp) || StringUtils.isBlank(NettyConf.cpys) || StringUtils.isBlank(NettyConf.shengID) || StringUtils.isBlank(NettyConf.shiID) || StringUtils.isBlank(NettyConf.host) || NettyConf.port == 0) {
                Toast.makeText(CjApplication.getInstance(), "请填写完整注册参数", Toast.LENGTH_LONG).show();
            } else {
                GpsZdzc zdzc = new GpsZdzc();
                zdzc.setSxyid(NettyConf.shiID);
                zdzc.setSyid(NettyConf.shengID);
                zdzc.setXlh(Long.toString(Long.valueOf(NettyConf.termno), 36).toUpperCase());
                zdzc.setZdcpbz(NettyConf.cp);
                zdzc.setZdcpys(NettyConf.cpys);
                zdzc.setZdxh("YW3000  ");
                zdzc.setZzsid(NettyConf.zzsid);
                byte[] b2 = zdzc.getZdzcbytes();
                List<Tdata> list = GpsMsgUtilClient.generateMsg(b2, "0100", NettyConf.mobile, "0");
                Log.e("TAG",list.get(0).getData());
                GatewayService.sendHexMsgToServer("gpsChannel", list.get(0).getData());
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    /**
     * 发送终端鉴权
     */
    public static void sendZdjqHex(){
        if(StringUtils.isNotEmpty(Params.gpsauthCode)) {
            byte[] b2 = getStringBytes(Params.gpsauthCode);
            List<Tdata> list = GpsMsgUtilClient.generateMsg(b2, "0102", NettyConf.mobile, "0");
            GatewayService.sendHexMsgToServer("gpsChannel", list.get(0).getData());
        }
    }

    /**
     * 终端注销
     */
    public static void sendZdzx(){
        byte[] b2=new byte[0];
        List<Tdata> list=GpsMsgUtilClient.generateMsg(b2,"0003",NettyConf.mobile,"0");
        GatewayService.sendHexMsgToServer("gpsChannel",list);
    }

    /**
     * 包含所有
     * @return
     */
    public static String getGnss(){
        GpsGnss gnss = new GpsGnss();
        if(LocationUtil.state&&NettyConf.location!=null) {

            //全程累计里程
            GpsCountDistance.addDistanceByLocation(NettyConf.location);

            Location location = NettyConf.location;
            gnss.setWd(String.valueOf(location.getLatitude()));
            gnss.setJd(String.valueOf(location.getLongitude()));
            gnss.setFx(String.valueOf((int) location.getBearing()));
            gnss.setSj(String.valueOf(ZdUtil.getLongTime()));
            gnss.setWxdwsd(String.valueOf(location.getSpeed()));

            gnss.setBjbz("000000000000"+ DzwlTimerTask.jcqy+"0000000000000000000");
            DzwlTimerTask.jcqy="0";
            gnss.setZt("0000000000000000000000000000001"+ AccUtil.accState);
            gnss.setXsjlsd(getSpeed());

            GpsGnssExtend ge = new GpsGnssExtend();
            SharedPreferences sp = CjApplication.getInstance().getSharedPreferences("gpscommon", Context.MODE_PRIVATE);
            ge.setLc(sp.getInt("zlc",0)/100);
            ge.setSd((short)(Double.valueOf(getSpeed())*10));
            gnss.setFjxx(ByteUtil.bytesToHexString(ge.getGnssExtendBytes()));

            byte[] bs = gnss.getGnssBytes();
            return ByteUtil.bytesToHexString(bs);
        }
        return null;
    }

    public static byte[] getStringBytes(String s){
        byte[] bs=s.getBytes();
        return bs;
    }

    public static String getSpeed(){
        try {
            double l = NettyConf.location.getSpeed() * 3.6;
            DecimalFormat decimalFormat=new DecimalFormat("0.0");
            return decimalFormat.format(l);
        }catch (Exception e){
            return "0";
        }
    }

    /**
     * 链接服务器
     */
    public static void conServer() {
        if (Params.gpsconstate == 0) {
            new Timer().schedule(new GpsConTask(),200);
        } else if (Params.gpsconstate == 1) {
            if (Params.gpszcstate == 0) {
                //没注册过注册
                GpsUtil.sendZdzc();
            } else if (Params.gpszcstate == 1) {
                //发送终端鉴权
                GpsUtil.sendZdjqHex();
            }
        }
    }
}
