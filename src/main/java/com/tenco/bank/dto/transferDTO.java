package com.tenco.bank.dto;

import lombok.Data;

@Data
public class transferDTO {

	private Long amount; // 거래금액
	private String wAccountNumber; //  출금 계좌 번호
	private String dAccountNumber; // 입금 계좌 번호
	private String password; // 출금 계좌 비밀 번호
	
}
