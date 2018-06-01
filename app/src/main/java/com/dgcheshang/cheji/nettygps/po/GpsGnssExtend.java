package com.dgcheshang.cheji.nettygps.po;

import com.dgcheshang.cheji.netty.util.ByteUtil;

/**
 * 位置附件信息
 * @author Administrator
 *
 */
public class GpsGnssExtend {
	private int lc;//里程
	private short sd;//行驶记录仪速度

	public int getLc() {
		return lc;
	}

	public void setLc(int lc) {
		this.lc = lc;
	}

	public short getSd() {
		return sd;
	}

	public void setSd(short sd) {
		this.sd = sd;
	}

	/**
	 * 获取附加信息字节数据
	 * @return
	 */
	public byte[] getGnssExtendBytes(){
		byte[] b=new byte[0];
		byte[] temp;

		//里程
		String id="01";
		temp=ByteUtil.hexStringToByte(id);
		b=ByteUtil.byteMerger(b, temp);

		temp=new byte[1];
		temp[0]=4;
		b= ByteUtil.byteMerger(b, temp);

		temp=ByteUtil.intToByteArray(lc);
		b=ByteUtil.byteMerger(b, temp);


		//行驶仪速度
		id="03";
		temp=ByteUtil.hexStringToByte(id);
		b=ByteUtil.byteMerger(b, temp);

		temp=new byte[1];
		temp[0]=2;
		b=ByteUtil.byteMerger(b, temp);

		temp=ByteUtil.shortToByteArray(sd);
		b=ByteUtil.byteMerger(b, temp);

		return b;
	}
}
