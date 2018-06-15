package com.dgcheshang.cheji.nettygps.init;

import com.dgcheshang.cheji.Tools.Speaking;
import com.dgcheshang.cheji.netty.clientreply.SczdzpR;
import com.dgcheshang.cheji.netty.po.MsgAll;
import com.dgcheshang.cheji.netty.po.MsgExtend;
import com.dgcheshang.cheji.netty.util.ForwardUtil;
import com.dgcheshang.cheji.netty.util.GatewayService;
import com.dgcheshang.cheji.netty.util.MsgHandle;
import com.dgcheshang.cheji.netty.util.MsgUtil;
import com.dgcheshang.cheji.netty.util.MsgUtilClient;
import com.dgcheshang.cheji.nettygps.po.GpsMsgAll;
import com.dgcheshang.cheji.nettygps.util.GpsMsgHandle;
import com.dgcheshang.cheji.nettygps.util.GpsMsgUtil;
import com.dgcheshang.cheji.nettygps.util.GpsMsgUtilClient;

import org.apache.commons.lang3.StringUtils;

import java.util.Map;

/**
 * Created by Administrator on 2017/5/19.
 */

public class GpsHandleMsg extends Thread{
    private String msg;

    public GpsHandleMsg(String msg) {
        this.msg = msg;
    }

    public void run(){
        String hexmsg="";
        //组拼原始数据
        msg=("7E"+msg+"7E").toUpperCase();

        GpsMsgAll ma= GpsMsgUtil.getMsgAll(msg);
        if(ma!=null){
            //处理信息
            GpsMsgHandle msgHandle = new GpsMsgHandle();
            GpsMsgUtilClient msgUtilClient = new GpsMsgUtilClient();
            if (ma.getCode().equals("0")) {
                msgHandle.handle(ma);
            } else if (ma.getCode().equals("4")) {
                //分包
                byte[] body = (byte[]) ma.getObject();
                Map<String, Object> map = GpsMsgUtil.getFbxx(ma.getHeader(), body, msg);
                if (map.get("msg").equals("0")) {
                    //保存回复通用应答
                    hexmsg = msgUtilClient.getCommonRC(ma.getHeader(), "0");
                    if (StringUtils.isNotEmpty(hexmsg)) {
                        GatewayService.sendHexMsgToServer("gpsChannel", hexmsg);
                    }
                } else if (map.get("msg").equals("2")) {
                    hexmsg = msgUtilClient.getCommonRC(ma.getHeader(), "0");
                    if (StringUtils.isNotEmpty(hexmsg)) {
                        GatewayService.sendHexMsgToServer("gpsChannel", hexmsg);
                    }
                    //发送补传分包信息
                    String xhs = String.valueOf(map.get("xhs"));
                    String bcmsg = msgUtilClient.getBcfbRequest(ma.getHeader(), xhs);
                    if (StringUtils.isNotEmpty(bcmsg)) {
                        GatewayService.sendHexMsgToServer("gpsChannel", bcmsg);
                    }
                } else if (map.get("msg").equals("1")) {
                    //分包接收完毕,组合包并按流程走
                    //保存回复通用应答
                    hexmsg = msgUtilClient.getCommonRC(ma.getHeader(), "0");
                    if (StringUtils.isNotEmpty(hexmsg)) {
                        GatewayService.sendHexMsgToServer("gpsChannel", hexmsg);
                    }

                    //按流程走
                    body = (byte[]) map.get("sbody");
                    Object o = GpsMsgUtil.getBodyObject(ma.getHeader(), body);
                    ma.setObject(o);

                    msgHandle.handle(ma);
                }
            } else {
                //Speaking.in("获取数据不完整");
                //错误
                hexmsg = msgUtilClient.getCommonRC(ma.getHeader(), "2");
                if (StringUtils.isNotEmpty(hexmsg)) {
                    GatewayService.sendHexMsgToServer("serverChannel", hexmsg);
                }
            }

        }
    }
}
