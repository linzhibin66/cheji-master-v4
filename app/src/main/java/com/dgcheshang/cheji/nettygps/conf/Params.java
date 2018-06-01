package com.dgcheshang.cheji.nettygps.conf;

import java.util.HashMap;
import java.util.Map;

/***********************************************
 * @项目名称：cheji-master-3.6 - st
 * @文件名称：Params
 * @文件描述：
 * @文件作者：joxhome
 * @创建时间：2018/5/31 10:11
 ***********************************************/
public class Params {
    public final static String gpshost="59.37.17.67";//gps host
    public final static int gpsport=13010;//gps port

    //接收分包缓存数据
    public static Map<String,Object> fbdata=new HashMap<String,Object>();

    public static int gpsconstate=0;//连接状态 0为未链接  1为连接中
    public static int gpszcstate=0;//需初始化
    public static int gpsjqstate=0;

    //鉴权码
    public static String gpsauthCode="";//需初始化

    public static int gpsjg=15;//gps发送间隔  需初始化
}
