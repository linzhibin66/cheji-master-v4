package com.dgcheshang.cheji.netty.serverreply;

import com.dgcheshang.cheji.netty.util.ByteUtil;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;

import org.apache.commons.lang3.StringUtils;

public class DeviceInfo implements Serializable{
	private int rs;//结果 0，成功 1失败(没后续字段)
	private String mobile;
	private String cp;
	private String cpys;
	private String model;//型号
	private String termno;//终端编号，转36进制可转成序列号
	private String syid;//省域ID
	private String sxyid;//市县域ID

	private String cjh;//车架号

	public int getRs() {
		return rs;
	}

	public void setRs(int rs) {
		this.rs = rs;
	}

	public String getMobile() {
		return mobile;
	}
	public void setMobile(String mobile) {
		this.mobile = mobile;
	}
	public String getCp() {
		return cp;
	}
	public void setCp(String cp) {
		this.cp = cp;
	}
	public String getCpys() {
		return cpys;
	}
	public void setCpys(String cpys) {
		this.cpys = cpys;
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public String getTermno() {
		return termno;
	}

	public void setTermno(String termno) {
		this.termno = termno;
	}

	public String getCjh() {
		return cjh;
	}

	public void setCjh(String cjh) {
		this.cjh = cjh;
	}

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

	public byte[] getDeviceInfoBytes(){
		byte[] bs=new byte[0];

		//终端手机号
		byte[] temp= ByteUtil.str2Bcd(this.mobile);
		if(temp.length<8){
			String hex=ByteUtil.bytesToHexString(temp);
			for(int i=temp.length;i<8;i++){
				hex="00"+hex;
			}
			temp=ByteUtil.hexStringToByte(hex);
		}

		bs=ByteUtil.byteMerger(bs, temp);

		try {
			temp=cp.getBytes("GBK");
			temp=ByteUtil.hexStringTOFinalbyte(ByteUtil.bytesToHexString(temp), 12, 1);
			bs=ByteUtil.byteMerger(bs, temp);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		if(StringUtils.isEmpty(cpys)){
			cpys="2";
		}
		temp=cpys.getBytes();
		bs=ByteUtil.byteMerger(bs, temp);

		return bs;
	}

}
