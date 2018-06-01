package com.dgcheshang.cheji.nettygps.po;

import com.dgcheshang.cheji.netty.util.ByteUtil;

public class GpsHeader implements java.io.Serializable{
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	// 消息ID
	private String msgid;
	// 消息体属性
	private String bodyprop;
	// 终端手机号
	private String mobileno;
	// 消息流水号
	private int msgserno;
	// 消息总包数
	private int msgpackcnt;
	// 包序号
	private int packsortno;

	public String getMsgid() {
		return msgid;
	}
	public void setMsgid(String msgid) {
		this.msgid = msgid;
	}
	public String getBodyprop() {
		return bodyprop;
	}
	public void setBodyprop(String bodyprop) {
		this.bodyprop = bodyprop;
	}
	public String getMobileno() {
		return mobileno;
	}
	public void setMobileno(String mobileno) {
		this.mobileno = mobileno;
	}
	public int getMsgserno() {
		return msgserno;
	}
	public void setMsgserno(int msgserno) {
		this.msgserno = msgserno;
	}
	public int getMsgpackcnt() {
		return msgpackcnt;
	}
	public void setMsgpackcnt(int msgpackcnt) {
		this.msgpackcnt = msgpackcnt;
	}
	public int getPacksortno() {
		return packsortno;
	}
	public void setPacksortno(int packsortno) {
		this.packsortno = packsortno;
	}

	/**
	 * 转换成字节数组
	 * @return
	 */

	public byte[] getHeaderBytes(){
		byte[] bs=new byte[0];
		//消息ID
		byte[] temp= ByteUtil.hexStringToByte(this.getMsgid());

		bs=ByteUtil.byteMerger(bs, temp);

		//消息体属性
		temp=ByteUtil.str2Tobytes(this.bodyprop);

		bs=ByteUtil.byteMerger(bs, temp);

		//终端手机号
		temp=ByteUtil.str2Bcd(this.mobileno);
		if(temp.length<6){
			String hex=ByteUtil.bytesToHexString(temp);
			for(int i=temp.length;i<6;i++){
				hex="00"+hex;
			}
			temp=ByteUtil.hexStringToByte(hex);
		}

		bs=ByteUtil.byteMerger(bs, temp);

		//消息流水号
		short i=(short) this.getMsgserno();
		temp=ByteUtil.shortToByteArray(i);

		bs=ByteUtil.byteMerger(bs, temp);

		//看分包总数是否为0
		if(this.msgpackcnt>1){
			i=(short) this.msgpackcnt;
			temp=ByteUtil.shortToByteArray(i);

			bs=ByteUtil.byteMerger(bs, temp);

			i=(short) this.packsortno;
			temp=ByteUtil.shortToByteArray(i);

			bs=ByteUtil.byteMerger(bs, temp);
		}

		return bs;
	}

}
