package com.tenco.bank.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.tenco.bank.dto.SignInDTO;
import com.tenco.bank.dto.SignUpDTO;
import com.tenco.bank.handler.exception.DataDeliveryException;
import com.tenco.bank.repository.model.User;
import com.tenco.bank.service.UserService;
import com.tenco.bank.utils.Define;

import jakarta.servlet.http.HttpSession;

@Controller // IoC에 대상(싱글톤 패턴으로 관리됨)
@RequestMapping("/user") // 대문 처리
public class UserController {
	
	private UserService userService;
	private final HttpSession session;
	
	// DI 처리
	@Autowired 
	public UserController(UserService service, HttpSession session) {
		this.userService = service;
		this.session = session;
	}
	
	
	/**
	 * 회원 가입 페이지 요청
	 * 주소설계 -> http://localhost/user/sign-up 
	 * @return signUp.jsp
	 */
	@GetMapping("/sign-up")
	public String signUpPage() {
		// prefix: /WEB-INF/view/
		//      user/signUp
		// suffix: .jsp
		return "user/signUp";
	}
	/**
	 * 회원 가입 로직 처리 요청
	 * 주소 설계 : http://localhost/user/sign-up 
	 * 	// Get, Post -> sign-up 같은 도메인이라도 구분이 가능하다. 
		// REST API 를 사용하는 이유에 대해한번 더 살펴 보세요  
	 * @param dto
	 * @return
	 */
	@PostMapping("/sign-up")
	public String signUpProc(SignUpDTO dto) {
		if(dto.getUsername() == null || dto.getUsername().isEmpty()) {
			throw new DataDeliveryException(Define.ENTER_YOUR_USERNAME, HttpStatus.BAD_REQUEST);
		}
		if(dto.getPassword() == null || dto.getPassword().isEmpty()) {
			throw new DataDeliveryException(Define.ENTER_YOUR_PASSWORD, HttpStatus.BAD_REQUEST);
		}
		if(dto.getFullname() == null || dto.getFullname().isEmpty()) {
			throw new DataDeliveryException(Define.ENTER_YOUR_FULLNAME, HttpStatus.BAD_REQUEST);
		}
		
		userService.createUser(dto);
		
		return "redirect:/user/sign-in";
	}
	/**
	 * 로그인 화면 요청
	 * 주소설계 : http://localhost:8080/user/sign-in
	 * @return
	 */
	@GetMapping("/sign-in")
	public String signIn() {
		// 인증검사 x
		// 유효성 검사 x
		return "user/signIn";
	}
	
	/**
	 * 로그인 요청 처리
	 * 주소 설계 : http://localhost:8080/user/sign-in
	 * @return
	 */
	@PostMapping("/sign-in")
	public String signProc(SignInDTO dto) {
		// 1.인증 검사 x
		// 2.유효성 검사 o
		if(dto.getUsername() == null || dto.getUsername().isEmpty()) {
			throw new DataDeliveryException(Define.ENTER_YOUR_USERNAME, HttpStatus.BAD_REQUEST);
		}
		if(dto.getPassword() == null || dto.getPassword().isEmpty()) {
			throw new DataDeliveryException(Define.ENTER_YOUR_PASSWORD, HttpStatus.BAD_REQUEST);
		}
		
		// 서비스 호출
		User principal = userService.readUser(dto);
		
		// 세션 메모리에 등록 처리
		session.setAttribute(Define.PRINCIPAL, principal);
		
		// 새로운 페이지로 이동 처리
		// TODO - 계좌 목록 페이지 이동처리
		return "redirect:/account/list";
	} // end of signProc
	
	// 코드 추가
	@GetMapping("/logout")
	public String logout() {
		session.invalidate(); // 로그아웃됨
		
		return "redirect:/user/sign-in";
	}
	
}
