package com.dgcheshang.cheji.nettygps.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.util.Log;

import com.dgcheshang.cheji.CjApplication;
import com.dgcheshang.cheji.netty.conf.NettyConf;

/**
 * Created by Hambobo on 2016-07-25.
 */
public class GpsCountDistance {
    private static final double EARTH_RADIUS = 6378.137;
    public static boolean isStart = false;
    static double lastLat;
    static double lastLongt;
    static double lastArc;
    static double totalFMile=0;
    static double totalMile = 0;
    static long lastCalculateTime;

    //将角度转换为弧度
    static double deg2rad(double degree) {
        return degree / 180.0 * Math.PI;
    }

    //将弧度转换为角度
    static double rad2deg(double radian) {
        return radian * 180.0 / Math.PI;
    }

    private static double rad(double d)
    {
        return d * Math.PI / 180.0;
    }

    //设置起始位置信息
    //lat 维度
    //longt 经度
    //arc 方向角度与正北方向的夹角
    public static void  startCount(double lat, double longt, double arc)
    {
        isStart = true;
        lastLat = lat;
        lastLongt = longt;
        lastArc = arc;
        lastCalculateTime = System.currentTimeMillis();
    }

//    lat1 纬度，0~359
//    arc1 角度 0~359
//    return  单位：米
    private static double getDistance(double lat1, double lng1, double arc1, double lat2, double lng2, double arc2)
    {
        double radLat1 = rad(lat1);
        double radLat2 = rad(lat2);
        double arc;
        double a = radLat1 - radLat2;
        double b = rad(lng1) - rad(lng2);
        double s = 2.0 * Math.asin(Math.sqrt(Math.pow(Math.sin(a/2d),2) +
                Math.cos(radLat1)*Math.cos(radLat2)*Math.pow(Math.sin(b/2d),2)));
        s = s * EARTH_RADIUS;
        s = Math.round(s * 10000d) / 10000d; //直线距离计算完成
        s *= 1000d;
        arc = Math.abs(arc2-arc1);
        if (arc > 5)//转换弧度长度
        {
            arc = deg2rad(arc);//转换为弧度计算
            s= (s/2d/Math.sin(arc/2d))*arc;
        }
        return s;
    }

    public static double getDistance2(double lat1, double lng1, double lat2, double lng2)
    {
        double radLat1 = rad(lat1);
        double radLat2 = rad(lat2);
        double a = radLat1 - radLat2;
        double b = rad(lng1) - rad(lng2);
        double s = 2.0 * Math.asin(Math.sqrt(Math.pow(Math.sin(a/2d),2) +
                Math.cos(radLat1)*Math.cos(radLat2)*Math.pow(Math.sin(b/2d),2)));
        s = s * EARTH_RADIUS;
        s = Math.round(s * 10000d) / 10000d; //直线距离计算完成
        s *= 1000d;
        return s;
    }

    public static  void addDistanceByLocation(Location location){
        if(location!=null) {
            addDistance(location.getLatitude(), location.getLongitude(), location.getBearing());
        }
    }
    //添加更新位置信息，累计里程
    //lat 维度
    //longt 经度
    //arc 方向角度与正北方向的夹角
    public static void addDistance(double lat, double longt, double arc)
    {
        if (isStart) {
            double s = getDistance(lastLat, lastLongt, lastArc, lat, longt, arc);
            double second = (System.currentTimeMillis()-lastCalculateTime)/1000d;
            if (s/second < 33)//若计算速度大于120Km/h，当成无效点处理

            {
                if(NettyConf.debug){
                    Log.e("TAG","一次有效里程(m):"+s);
                }
                totalMile += s;
                totalFMile += s;
                SharedPreferences sp = CjApplication.getInstance().getSharedPreferences("gpscommon", Context.MODE_PRIVATE);
                SharedPreferences.Editor edit = sp.edit();
                edit.putInt("zlc",sp.getInt("zlc",0)+(int)s);
            }
            lastLat = lat;
            lastLongt = longt;
            lastArc = arc;
            lastCalculateTime = System.currentTimeMillis();
        }
        else
        {
            isStart = true;
            lastLat = lat;
            lastLongt = longt;
            lastArc = arc;
            totalMile = 0;
            totalFMile=0;
            lastCalculateTime = System.currentTimeMillis();
        }
    }
    //获取累计总里程 单位：米
    public static double getTotalMile()
    {
        if (isStart) {
            return totalMile;
        }
        return 0;
    }

    //获取累计总里程 单位：米
    public static double getTotalFMile()
    {
        if (isStart) {
            return totalFMile;
        }
        return 0;
    }

    //设置总里程信息，默認初始為0  单位：米
    public static void setTotalMile(double mile){
        totalMile = mile;
    }

    //设置总里程信息，默認初始為0  单位：米
    public static void setTotalFMile(double mile){
        totalFMile = mile;
    }

    public static double gps2d(double lat_a, double lng_a, double lat_b, double lng_b)
    {
        double d = 0;
        lat_a=lat_a*Math.PI/180;
        lng_a=lng_a*Math.PI/180;
        lat_b=lat_b*Math.PI/180;
        lng_b=lng_b*Math.PI/180;

        d=Math.sin(lat_a)*Math.sin(lat_b)+Math.cos(lat_a)*Math.cos(lat_b)*Math.cos(lng_b-lng_a);
        d=Math.sqrt(1-d*d);
        d=Math.cos(lat_b)*Math.sin(lng_b-lng_a)/d;
        d=Math.asin(d)*180/Math.PI;
        return d;
    }
}
