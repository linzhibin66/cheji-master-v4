package com.dgcheshang.cheji.netty.timer;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import com.dgcheshang.cheji.Bean.CardSecret;
import com.dgcheshang.cheji.Tools.Speaking;
import com.dgcheshang.cheji.netty.conf.NettyConf;
import com.dgcheshang.cheji.netty.util.CardContent;
import com.dgcheshang.cheji.netty.util.StringUtils;
import com.rscja.deviceapi.RFIDWithISO14443A;
import com.rscja.deviceapi.entity.SimpleRFIDEntity;
import com.rscja.deviceapi.exception.ConfigurationException;

import java.util.TimerTask;

/**
 * Created by Administrator on 2017/6/23.
 */

public class CardTimer extends TimerTask{
    public static boolean isstop=false;
    private RFIDWithISO14443A mRFID;
    private String whocard;

    public CardTimer(RFIDWithISO14443A mRFID, String whocard) {
        this.mRFID = mRFID;
        this.whocard=whocard;
    }

    @Override
    public void run() {
        if(NettyConf.debug){
            Log.e("TAG","刷卡类型："+whocard);
        }
        if (!isstop) {
            isstop=true;
            SimpleRFIDEntity entity = null;
            try {
                try {
                    if (mRFID == null) {
                        mRFID = RFIDWithISO14443A.getInstance();
                        Thread.sleep(1000);
                        mRFID.init();
                    }
                } catch (Exception e) {
                    isstop=false;
                    return;
                }
                entity = mRFID.request();
                if (entity == null) {
                    // 读卡失败
                    if(NettyConf.debug) {
                        Log.e("TAG", "读卡失败");
                    }
                    isstop=false;
                } else {
                    //读卡成功
                    String uid = entity.getId();
                    Log.e("TAG", "uid:" + uid);
                    if(StringUtils.isNotEmpty(uid)) {
                       //读IC卡信息
                        CardContent cardcontent = getCardInfo();
                        isstop = true;
                        Message msg = new Message();
                        msg.arg1 = 6;
                        Bundle bundle = new Bundle();

                        if (whocard.equals("jlcard")) {//教练刷卡
                            if(NettyConf.yz_ICcard!=2){
                                bundle.putString("jluid", uid);
                                bundle.putSerializable("cardcontent", cardcontent);
                                msg.setData(bundle);
                                Handler handler = (Handler) NettyConf.handlersmap.get("logincoach");
                                handler.sendMessage(msg);
                            }else {
                                Speaking.in("无效教练卡");
                                isstop=false;
                            }

                        } else if (whocard.equals("xycard")) {//学员刷卡
                            if(NettyConf.yz_ICcard!=2) {
                                bundle.putString("xyuid", uid);
                                bundle.putSerializable("cardcontent", cardcontent);
                                msg.setData(bundle);
                                Handler handler = (Handler) NettyConf.handlersmap.get("loginstudent");
                                handler.sendMessage(msg);
                            }else {
                                Speaking.in("无效学员卡");
                                isstop=false;
                            }
                        } else if (whocard.equals("admincard")) {//管理员设置
                            bundle.putString("adminuid", uid);
                            bundle.putSerializable("cardcontent", cardcontent);
                            msg.setData(bundle);
                            Handler handler = (Handler) NettyConf.handlersmap.get("main");
                            handler.sendMessage(msg);
                        }
                    }else{
                        isstop=false;
                    }
                }
            } catch (Exception e2) {
                    isstop=false;
            } finally {
                // mRFID.free();
            }
        }
    }

    /**
     * 读取IC卡扇区信息
     *
     * @param */
    public CardContent getCardInfo(){
        CardContent cardcontent = new CardContent();
        try {
//            RFIDWithISO14443A.getInstance().init();
            SimpleRFIDEntity entity = RFIDWithISO14443A.getInstance().request();
            if (entity != null) {
                String skey = "FFFFFFFFFFFF";
                byte[] blockData = new byte[16];
                RFIDWithISO14443A.KeyType nKeyType = RFIDWithISO14443A.KeyType.TypeB;
                char[] recvChars;
                StringBuilder sb = new StringBuilder();
                CardSecret cardSecret = new CardSecret();
                for (int sector = 0; sector < 5; sector++) {
                    skey=cardSecret.getContent(sector);
                    if (RFIDWithISO14443A.getInstance().VerifySector(sector, skey, nKeyType)) {
                        for (int block = 0; block < 3; block++) {
                            if ((sector > 0 || block > 0)) {
                                recvChars = RFIDWithISO14443A.getInstance().M1_ReadData(sector, block);
                                if (recvChars != null && recvChars.length == 16) {
                                    int index = 0;
                                    for (char value : recvChars) {
                                        blockData[index++] = (byte) (value & 0xFF);
                                    }
                                    sb.append(StringUtils.bytesToHexString(blockData));
                                } else {

                                    break;
                                }
                            }
                        }
                    } else {

                        break;
                    }
                }
                String sTemp = sb.toString();
                if(!sTemp.equals("")){
                    cardcontent.anlisys(sTemp);
                    Log.e("TAG",cardcontent.getType()+","+ cardcontent.getCx()+","+cardcontent.getXm()+","+cardcontent.getZjhm()+","+cardcontent.getZjlx()+","+cardcontent.getBmsj()+","+cardcontent.getJxmc()+","+cardcontent.getFkrq()+","+cardcontent.getTybh());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            RFIDWithISO14443A.getInstance().free();
        } catch (ConfigurationException e) {
            e.printStackTrace();
        }
        return cardcontent;
    }

}
