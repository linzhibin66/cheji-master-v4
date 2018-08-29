package com.dgcheshang.cheji.Bean;

/**
 * 读取卡扇区的密码
 */
public class CardSecret {

	private static final String sec0="4009669128AA";
	private static final String sec1="4009669128BB";
	private static final String sec2="4009669128CC";
	private static final String sec3="4009669128DD";
	private static final String sec4="4009669128EE";
	private static final String sec5="4009669128FF";
	private static final String sec6="4009669128AB";
	private static final String sec7="4009669128AC";
	private static final String sec8="4009669128AD";
	private static final String sec9="4009669128AE";
	private static final String sec10="4009669128AF";
	private static final String sec11="4009669128BA";
	private static final String sec12="4009669128BB";
	private static final String sec13="4009669128BC";
	private static final String sec14="4009669128BD";
	private static final String sec15="4009669128BE";


	public String getContent(int number){
		if(number==0){
			return sec0;
		}else if(number==1){
			return sec1;
		}else if(number==2){
			return sec2;
		}else if(number==3){
			return sec3;
		}else if(number==4){
			return sec4;
		}else if(number==5){
			return sec5;
		}else if(number==6){
			return sec6;
		}else {
			return "FFFFFFFFFFFF";
		}

	}

}
