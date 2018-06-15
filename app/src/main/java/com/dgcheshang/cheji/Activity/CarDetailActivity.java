package com.dgcheshang.cheji.Activity;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.dgcheshang.cheji.R;
import com.dgcheshang.cheji.netty.conf.NettyConf;
import com.dgcheshang.cheji.netty.util.LocationUtil;
import com.dgcheshang.cheji.netty.util.ZdUtil;
import com.dgcheshang.cheji.nettygps.conf.Params;

/**
 * 车辆信息
 * */
public class CarDetailActivity extends BaseInitActivity implements View.OnClickListener{
    Context context=CarDetailActivity.this;

    TextView tv_net_state,tv_con_state,tv_jq_state,tv_gps_state,tv_camera_state,tv_self_ip,tv_zdbh;
    TextView tv_ptgps_lj,tv_ptgps_jq;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_car_detail);
        init();
    }

    /**
     * 初始化布局
     * */
    private void init() {
        View layout_back = findViewById(R.id.layout_back);//返回按键
        TextView tv_phonenum = (TextView) findViewById(R.id.tv_phonenum);//手机号码
        TextView tv_imei = (TextView) findViewById(R.id.tv_imei);//IMEI
        TextView tv_manufacturer = (TextView) findViewById(R.id.tv_manufacturer);//服务商
        TextView tv_model = (TextView) findViewById(R.id.tv_model);//型号
        tv_net_state = (TextView) findViewById(R.id.tv_net_state);//网络状态
        tv_con_state = (TextView) findViewById(R.id.tv_con_state);//连接状态
        tv_jq_state = (TextView) findViewById(R.id.tv_jq_state);//鉴权状态
        tv_gps_state = (TextView) findViewById(R.id.tv_gps_state);//gps状态
        tv_self_ip = (TextView) findViewById(R.id.tv_self_ip);//本机ip
        tv_zdbh = (TextView) findViewById(R.id.tv_zdbh);//终端编号
        tv_camera_state = (TextView) findViewById(R.id.tv_camera_state);//摄像头状态

        tv_ptgps_lj = (TextView) findViewById(R.id.tv_ptgps_lj);//平台gps
        tv_ptgps_jq = (TextView) findViewById(R.id.tv_ptgps_jq);//平台gps
        if(Params.gpsconstate==0){
            tv_ptgps_lj.setText("未连接");
        }else {
            tv_ptgps_lj.setText("已连接");
        }

        if(Params.gpsjqstate==0){
            tv_ptgps_jq.setText("未鉴权");
        }else {
            tv_ptgps_jq.setText("已鉴权");
        }

        String ipAddress = ZdUtil.getIPAddress(context);
        if(ipAddress!=null){
            tv_self_ip.setText(ipAddress);
        }
        tv_zdbh.setText(NettyConf.termno);

        int lianjie = NettyConf.constate;
        int jianquan =NettyConf.jqstate;
        //boolean network = NettyConf.netstate;
        boolean gpsstate = LocationUtil.state;
        boolean camerastate = NettyConf.camerastate;

        if(NettyConf.netstate!=null){
            if(NettyConf.netstate){
                tv_net_state.setText("连接");
            }else {
                tv_net_state.setText("断开");
            }
        }else{
            tv_net_state.setText("监听失败");
        }

        if(gpsstate){
            tv_gps_state.setText("打开");
        }else {
            tv_gps_state.setText("关闭");
        }
        if(lianjie==1){
            tv_con_state.setText("已连接");
        }else {
            tv_con_state.setText("已断开");
        }
        if(jianquan==1){
            tv_jq_state.setText("已鉴权");
        }else {
            tv_jq_state.setText("未鉴权");
        }

        if(camerastate){
            tv_camera_state.setText("连接");
        }else {
            tv_camera_state.setText("断开");
        }
        tv_imei.setText(NettyConf.imei);
        tv_phonenum.setText(NettyConf.mobile);
        tv_model.setText(NettyConf.model);
        layout_back.setOnClickListener(this);
    }

    /**
     * 点击监听
     * */
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.layout_back://返回按钮
                finish();
                break;
        }
    }
}
