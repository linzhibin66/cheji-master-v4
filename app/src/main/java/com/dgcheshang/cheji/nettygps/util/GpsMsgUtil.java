package com.dgcheshang.cheji.nettygps.util;


import com.dgcheshang.cheji.netty.clientreply.CxcsR;
import com.dgcheshang.cheji.netty.po.Header;
import com.dgcheshang.cheji.netty.po.MsgExtend;
import com.dgcheshang.cheji.netty.po.ParamsSz;
import com.dgcheshang.cheji.netty.po.ParamsSzAll;
import com.dgcheshang.cheji.netty.po.Zdjq;
import com.dgcheshang.cheji.netty.po.Zdzc;
import com.dgcheshang.cheji.netty.po.ZdzcR;
import com.dgcheshang.cheji.netty.proputil.MsgID;
import com.dgcheshang.cheji.netty.serverreply.CommonR;
import com.dgcheshang.cheji.netty.util.ByteUtil;
import com.dgcheshang.cheji.netty.util.CommonUtil;
import com.dgcheshang.cheji.nettygps.conf.Params;
import com.dgcheshang.cheji.nettygps.po.GpsHeader;
import com.dgcheshang.cheji.nettygps.po.GpsMsgAll;
import com.dgcheshang.cheji.nettygps.po.GpsZdzcR;
import com.dgcheshang.cheji.nettygps.task.GpsHandleFbdata;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;

/**
 * 计时平台使用
 * @author Administrator
 *
 */
public class GpsMsgUtil {

	/**
	 * 心跳
	 */
	public static String getXthf(String mobile){
		byte[] b2=new byte[0];
		GpsHeader h=new GpsHeader();
		h.setMsgid("0002");
		h.setBodyprop("0000000000000000");
		h.setMobileno(mobile);
		h.setMsgserno(0);
		byte[] b1=h.getHeaderBytes();
		String hexmsg=getMsg(b1, b2);
		return hexmsg;
	}

	/**
	 * 获取完整的回复信息
	 * @param b1
	 * @param b2
	 * @return
	 */
	public static String getMsg(byte[] b1,byte[] b2){
		byte[] temp=ByteUtil.byteMerger(b1, b2);
		int code=ByteUtil.getValidateCode(temp);
		byte[] b3=new byte[1];
		b3[0]=new Integer(code).byteValue();
		temp=ByteUtil.byteMerger(temp, b3);
		String hex=ByteUtil.bytesToHexString(temp);

		StringBuffer sb=new StringBuffer();
		for(int i=0;i<hex.length();i=i+2){
			sb.append(hex.substring(i, i+2)+",");
		}
		hex=sb.toString().replaceAll("7D", "7D,01");
		hex=hex.replaceAll("7E", "7D,02");
		hex=hex.replaceAll(",", "");
		hex="7E"+hex+"7E";

		return hex;
	}

	/**
	 * 获取
	 * @param header
	 * @param jg
	 * @return
	 */
	public static CommonR getCommonRhs(GpsHeader header,String jg){
		//通用应答
		CommonR cr=new CommonR();
		cr.setJg(Integer.valueOf(jg));
		cr.setLsh(header.getMsgserno());
		cr.setMsgid(header.getMsgid());
		return cr;
	}

	/**
	 * 解析数据
	 * @param msg
	 * @return
	 */
	public static GpsMsgAll getMsgAll(String msg){
		try{
			GpsMsgAll ma=new GpsMsgAll();
			ma.setHexString(msg);
			//解析头部
			//获取去掉分割符消息的字节数组
			byte[] bytes=GpsByteToCls.hexStringTobyte(msg);

			//效验码检测
			boolean flag=ByteUtil.validateCode(bytes);

			if(!flag){
				//效验码错误
				ma.setCode("1");
				ma.setErrormsg("效验码错误！");
				return ma;
			}
			//效验码成功
			//获取消息头
			GpsHeader header=GpsByteToCls.getHeader(bytes);
			if(header==null){
				ma.setCode("2");
				ma.setErrormsg("消息头解析失败！");
				return ma;
			}

			//消息头成功解析赋值
			ma.setHeader(header);

			//获取消息体
			byte[] body=null;
			if(header.getMsgpackcnt()==0){
				body=ByteUtil.subBytes(bytes, 12, bytes.length-13);
			}else{
				body=ByteUtil.subBytes(bytes, 16, bytes.length-17);
			}

			int sjcd=Integer.parseInt(header.getBodyprop().substring(6,header.getBodyprop().length()),2);
			if(sjcd!=0&&sjcd!=body.length){
				//数据长度错误,返回通用错误
				ma.setCode("3");
				ma.setErrormsg("消息体长度跟头部指定长度不符！");
				return ma;
			}

			//判断是否分包分包返回
			if(header.getBodyprop().substring(2,3).equals("1")){
				ma.setObject(body);
				ma.setCode("4");
				ma.setErrormsg("分包信息");
				return ma;
			}

			//对消息体进行解析
			Object o=getBodyObject(header,body);
			if(o==null){
				//消息体解析失败
				ma.setCode("5");
				ma.setErrormsg("消息体解析失败！");
			}else{
				//消息体解析成功
				ma.setObject(o);
				ma.setCode("0");
			}

			return ma;
		}catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}


	//解析消息体
	public static Object getBodyObject(GpsHeader header,byte[] body){
		try{
			Object o="";
			if(header.getMsgid().equals(MsgID.getValue("zdzc"))){
				//终端注册
				Zdzc zdzc=GpsByteToCls.getZdzc(body);
				return zdzc;
			}else if(header.getMsgid().equals("8100")){
				//终端注册应答
				GpsZdzcR zr=GpsByteToCls.getZdzcR(body);
				return zr;
			}else if(header.getMsgid().equals(MsgID.getValue("zdjq"))){
				//终端鉴权处理
				Zdjq zdjq=GpsByteToCls.getZdjq(body);
				return zdjq;
			}else if(header.getMsgid().equals("0001")){
				//通用应答计时终端下发的指令
				CommonR cr=GpsByteToCls.getCommonR(body);
				cr.setZdid(header.getMobileno());
				cr.setSj(CommonUtil.getBdtime());
				return cr;
			}else if(header.getMsgid().equals("8103")){
				//终端参数设置解析
				ParamsSz pz=GpsByteToCls.getParamsSz(body);
				return pz;
			}else if(header.getMsgid().equals("7103")){
				//版本级别终端参数设置
				ParamsSzAll pz=GpsByteToCls.getParamsSzAll(body);
				return pz;
			}else if(header.getMsgid().equals("8106")){
				//查询指定终端参数
				return GpsByteToCls.getCxcs(body);
			}else if(header.getMsgid().equals("0104")){
				//查询终端参数回复
				CxcsR cr=GpsByteToCls.getCxcsR(body);
				return cr;
			}else if(header.getMsgid().equals("8105")){
				//终端控制指令解析
				return GpsByteToCls.getZdkz(body);
			}else if(header.getMsgid().equals("8202")){
				//位置信息跟踪控制
				return GpsByteToCls.getGzkz(body);
			}else if(header.getMsgid().equals("8001")){
				//监督平台信息回复
				CommonR cr=GpsByteToCls.getCommonR(body);
				cr.setZdid(header.getMobileno());
				cr.setSj(CommonUtil.getBdtime());
				return cr;
			}else if(header.getMsgid().equals("8003")){
				//补传分包请求解析
				return GpsByteToCls.getBcfb(body);
			}else if(header.getMsgid().equals("7003")){
				//升级指令
				return GpsByteToCls.getUpgrade(body);
			}else if(header.getMsgid().equals("8604")){
				//设置电子围栏
				return GpsByteToCls.getDzwl(body);
			}else if(header.getMsgid().equals("8605")){
				//清除电子围栏
				return GpsByteToCls.getClearJlcd(body);
			}else if(header.getMsgid().equals("7105")){
				return GpsByteToCls.getDeviceInfo(body);
			}else if(header.getMsgid().equals("0900")||header.getMsgid().equals("8900")){
				//数据上行透传
				MsgExtend me=GpsByteToCls.getMsgExtend(body);
				return me;
			}
			return o;
		}catch(Exception e){
			e.printStackTrace();
			return null;
		}

	}



	/**
	 * 分包分析0.保存成功，1，数据接收完毕 2.补发分包消息
	 */
	public static Map<String,Object> getFbxx(GpsHeader header, byte[] body, String hs){
		Map<String,Object> map=new HashMap<String, Object>();
		byte[] sbody=null;//合成后的消息体
		String msg="0";
		String xhs="";

		//存进redis并设置存活时间
		String key=header.getMsgid()+"_"+header.getMsgserno();

		Params.fbdata.put(key,body);
		//存活线程启动
		new Timer().schedule(new GpsHandleFbdata(key),200*1000);

		//有分包要进行处理，查看是否接收完毕
		//开始流水号
		int k=header.getMsgserno()-header.getPacksortno()+1;
		//结束流水号加1
		int j=k+header.getMsgpackcnt();
		//组合字节数组变量
		byte[] sb=new byte[0];
		boolean fg=true;
		for(int i=k;i<j;i++){
			String gkey=header.getMsgid()+"_"+i;
			byte[] temp= (byte[]) Params.fbdata.get(gkey);
			if(temp==null){
				fg=false;
				break;
			}else{
				sb=ByteUtil.byteMerger(sb, temp);
			}
		}

		if(fg){
			//组合完成
			msg="1";
			sbody=sb;
		}else{
			if(header.getMsgpackcnt()==header.getPacksortno()){
				//组合失败后看当前缺失的包
				StringBuffer sbf=new StringBuffer();
				for(int i=k;i<j;i++){
					String gkey=header.getMsgid()+"_"+i;
					byte[] temp= (byte[]) Params.fbdata.get(gkey);
					if(temp==null){
						sbf.append(i-k+1+",");
					}
				}
				msg="2";
				xhs=sbf.toString().substring(0,sbf.length()-1);
			}
		}

		map.put("msg", msg);
		map.put("sbody", sbody);
		map.put("xhs", xhs);
		return map;
	}
}
