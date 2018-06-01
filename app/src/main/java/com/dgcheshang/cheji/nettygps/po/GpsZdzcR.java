package com.dgcheshang.cheji.nettygps.po;

import com.dgcheshang.cheji.netty.util.ByteUtil;

import org.apache.commons.lang3.StringUtils;

public class GpsZdzcR implements java.io.Serializable{

	private static final long serialVersionUID = 1L;
	private int lsh;//应答流水号
	private int jg;//结果   0：成功；1：车辆已被注册；2：数据库中无该车辆；3：终端已被注册4：数据库中无该终端。只有在成功后才返回以下内容
	private String authCode;//鉴权码


	public int getLsh() {
		return lsh;
	}
	public void setLsh(int lsh) {
		this.lsh = lsh;
	}
	public int getJg() {
		return jg;
	}
	public void setJg(int jg) {
		this.jg = jg;
	}

	public String getAuthCode() {
		return authCode;
	}

	public void setAuthCode(String authCode) {
		this.authCode = authCode;
	}

}
