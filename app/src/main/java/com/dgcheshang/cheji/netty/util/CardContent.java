package com.dgcheshang.cheji.netty.util;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;



/**
 * 汕头版发卡内容
 * @ClassName:   CardContent
 * @Description: 汕头版发卡内容
 * @author:      joxhome
 * @date:        2018年8月9日    上午10:54:28
 */
public class CardContent implements Serializable{

	private static final long serialVersionUID = 1L;
	private String type;//卡类型 X 学员卡 Y 教练卡 16字节
	private String fkrq;//发卡日期 （YYYYMMDD）16字节
	private String zjhm;//证件号码    32字节
	private String zjlx;//证件类型   1、身份证 2、   16字节
	private String xm;//姓名  16字节
	private String tybh;//统一编号 16字节
	private String jxtybh;//驾校统一编号  16字节
	private String jxmc;//驾校名称  64字节
	private String cx;//培训车型或准教车型 16字节
	private String bmsj;//报名日期（YYYYMMDD）16字节
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getFkrq() {
		return fkrq;
	}
	public void setFkrq(String fkrq) {
		this.fkrq = fkrq;
	}
	public String getZjhm() {
		return zjhm;
	}
	public void setZjhm(String zjhm) {
		this.zjhm = zjhm;
	}
	public String getZjlx() {
		return zjlx;
	}
	public void setZjlx(String zjlx) {
		this.zjlx = zjlx;
	}
	public String getXm() {
		return xm;
	}
	public void setXm(String xm) {
		this.xm = xm;
	}
	public String getTybh() {
		return tybh;
	}
	public void setTybh(String tybh) {
		this.tybh = tybh;
	}
	public String getJxtybh() {
		return jxtybh;
	}
	public void setJxtybh(String jxtybh) {
		this.jxtybh = jxtybh;
	}
	public String getJxmc() {
		return jxmc;
	}
	public void setJxmc(String jxmc) {
		this.jxmc = jxmc;
	}
	public String getCx() {
		return cx;
	}
	public void setCx(String cx) {
		this.cx = cx;
	}
	public String getBmsj() {
		return bmsj;
	}
	public void setBmsj(String bmsj) {
		this.bmsj = bmsj;
	}


	public void anlisys(String hexstring){
		try{
			SimpleDateFormat sdf=new SimpleDateFormat("yyyyMMdd");
			String s=hexstring.substring(0, 32);
			s=ByteUtil.clearHexStringback(s);
			this.type=new String(ByteUtil.hexStringToByte(s));

			s=hexstring.substring(32,64);
			s=ByteUtil.clearHexStringback(s);
			s=new String(ByteUtil.hexStringToByte(s));
			this.fkrq=s;

			s=hexstring.substring(64, 128);
			s=ByteUtil.clearHexStringback(s);
			this.zjhm=new String(ByteUtil.hexStringToByte(s));

			s=hexstring.substring(128, 160);
			s=ByteUtil.clearHexStringback(s);
			this.zjlx=new String(ByteUtil.hexStringToByte(s));

			s=hexstring.substring(160, 192);
			s=ByteUtil.clearHexStringback(s);
			this.xm=new String(ByteUtil.hexStringToByte(s),"GBK");

			s=hexstring.substring(192, 224);
			s=ByteUtil.clearHexStringback(s);
			this.tybh=new String(ByteUtil.hexStringToByte(s));

			s=hexstring.substring(224, 256);
			s=ByteUtil.clearHexStringback(s);
			this.jxtybh=new String(ByteUtil.hexStringToByte(s));

			s=hexstring.substring(256, 384);
			s=ByteUtil.clearHexStringback(s);
			this.jxmc=new String(ByteUtil.hexStringToByte(s),"GBK");

			s=hexstring.substring(384, 416);
			s=ByteUtil.clearHexStringback(s);
			this.cx=new String(ByteUtil.hexStringToByte(s));

			s=hexstring.substring(416, 448);
			s=ByteUtil.clearHexStringback(s);
			s=new String(ByteUtil.hexStringToByte(s));
			this.bmsj=s;
		}catch(Exception e){
			e.printStackTrace();
		}
	}


}
