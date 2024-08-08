package com.tenco.bank.handler.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

// 인증처리 예외 (인증된 사용자인지 아닌지)
@Getter
public class UnAuthorizedException extends RuntimeException {
	
	private HttpStatus status;
	
	// throw UnAuthorizedException(   ,   )
	public UnAuthorizedException(String message, HttpStatus status) {
		super(message);
		this.status = status;
	}
	
}
