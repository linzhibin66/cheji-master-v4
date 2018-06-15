package com.dgcheshang.cheji.nettygps.po;

import android.util.Log;

import com.dgcheshang.cheji.netty.conf.NettyConf;
import com.dgcheshang.cheji.netty.util.ByteUtil;

import org.apache.commons.lang3.StringUtils;

import java.io.UnsupportedEncodingException;

/**
 * 终端注册
 * @author Administrator
 *
 */
public class GpsZdzc implements java.io.Serializable{

	private static final long serialVersionUID = 1L;
	private String syid;//省域ID
	private String sxyid;//市县域ID
	private String zzsid;//制造商id
	private String zdxh;//终端型号
	private String xlh;//序列号
	private String zdcpys;//终端车牌颜色
	private String zdcpbz;//终端车牌标志

	public String getSyid() {
		return syid;
	}
	public void setSyid(String syid) {
		this.syid = syid;
	}
	public String getSxyid() {
		return sxyid;
	}
	public void setSxyid(String sxyid) {
		this.sxyid = sxyid;
	}
	public String getZzsid() {
		return zzsid;
	}
	public void setZzsid(String zzsid) {
		this.zzsid = zzsid;
	}
	public String getZdxh() {
		return zdxh;
	}
	public void setZdxh(String zdxh) {
		this.zdxh = zdxh;
	}
	public String getXlh() {
		return xlh;
	}
	public void setXlh(String xlh) {
		this.xlh = xlh;
	}
	public String getZdcpys() {
		return zdcpys;
	}
	public void setZdcpys(String zdcpys) {
		this.zdcpys = zdcpys;
	}
	public String getZdcpbz() {
		return zdcpbz;
	}
	public void setZdcpbz(String zdcpbz) {
		this.zdcpbz = zdcpbz;
	}

	public byte[] getZdzcbytes(){
		byte[] b=new byte[0];

		byte[] temp= ByteUtil.shortToByteArray(Short.valueOf(syid));
		b=ByteUtil.byteMerger(b, temp);

		temp=ByteUtil.shortToByteArray(Short.valueOf(sxyid));
		b=ByteUtil.byteMerger(b, temp);

		temp=zzsid.getBytes();
		temp=ByteUtil.hexStringTOFinalbyte(ByteUtil.bytesToHexString(temp), 5, 1);
		b=ByteUtil.byteMerger(b, temp);

		if(NettyConf.debug){
			Log.e("TAG","终端型号："+zdxh);
		}
		temp=zdxh.getBytes();
		temp=ByteUtil.hexStringTOFinalbyte(ByteUtil.bytesToHexString(temp), 8, 2);
		b=ByteUtil.byteMerger(b, temp);

		if(xlh.length()<7){
            String s="";
			for(int i=0;i<7-xlh.length();i++){
				s+="0";
			}
			xlh=s+xlh;
		}
		temp=xlh.getBytes();
		temp=ByteUtil.hexStringTOFinalbyte(ByteUtil.bytesToHexString(temp), 7, 2);
		b=ByteUtil.byteMerger(b, temp);

		temp=new byte[1];
		if(StringUtils.isEmpty(zdcpys)){
			zdcpys="1";
		}
		temp[0]=Byte.valueOf(zdcpys);
		b=ByteUtil.byteMerger(b, temp);

		try {
			temp=new String(zdcpbz).getBytes("GBK");
			b=ByteUtil.byteMerger(b, temp);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		temp=new byte[1];
		temp[0]=0;
		b=ByteUtil.byteMerger(b, temp);
		return b;
	}
}
