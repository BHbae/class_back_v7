package com.tenco.bank.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import com.tenco.bank.dto.KakaoProfile;
import com.tenco.bank.dto.OAuthToken;
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
	
	@Value("${tenco.key}")
	private String tencoKey;
	
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
	
	
	@GetMapping("/kakao")
	public String getMethodName(@RequestParam(name = "code") String code) {
		
		System.out.println("code : " + code);
		
		// POST - 카카오 토큰 요청 받기
		// Header, body 구성
		RestTemplate rt1 = new RestTemplate();
		
		//헤더 구성
		HttpHeaders hesder1 = new HttpHeaders();
		hesder1.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");
		
		// 바디 구성
		MultiValueMap<String, String> params1 = new LinkedMultiValueMap<String, String>();
		params1.add("grant_type", "authorization_code");
		params1.add("client_id", "0f85dc1db16a7fb4cff7b2ff3fbe847f");
		params1.add("redirect_uri", "http://localhost:8080/user/kakao");
		params1.add("code", code);
		
		// 헤더와 바디의 결합
		HttpEntity<MultiValueMap<String, String>> reqkakoMessage = new HttpEntity<>(params1, hesder1);
		
		// 통신 요청
		ResponseEntity<OAuthToken> response1 =
		rt1.exchange("https://kauth.kakao.com/oauth/token", HttpMethod.POST, reqkakoMessage, OAuthToken.class);
		
		System.out.println("response : " + response1.getBody().toString());
		
		
		// 카카오 리소스 서버 사용자 정보 가져오기
		RestTemplate rt2 = new RestTemplate();
		
		// 헤더
		HttpHeaders headers2 = new HttpHeaders();
		// 반드시 Bearer 값 다음에 공백 한칸 추가!!
		headers2.add("Authorization", "Bearer " + response1.getBody().getAccessToken());
		headers2.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");
		
		// HTTp Entity 만들기
		HttpEntity<MultiValueMap<String, String>> reqKakoInfo = new HttpEntity<>(headers2);
		
		// 통신 요청
		ResponseEntity<KakaoProfile> response2 
					= rt2.exchange("https://kapi.kakao.com/v2/user/me", HttpMethod.POST, reqKakoInfo, KakaoProfile.class);
		
		
		KakaoProfile kakaoProfile = response2.getBody();
		System.out.println("kakaoProfile : " + kakaoProfile);
		// ------ 카카오 사용자 정보 응답 완료 --------
		
		// 최초 사용자라면 자동 회원가입 처리 (우리 서버)
		// 회원가입 이력이 있는 사용자라면 바로 세션 처리 (우리 서버)
		// 사전 기반 --> 소셜 사용자는 비밀번호를 입력하는가? 안하는가?
		// 우리서버에 회원 가입시에 --> PASSWORD -> NOT NULL (무조건 만들어 넣어야 함 db 정책)
		
		// 1.회원 가입 데이터 생성
		SignUpDTO signUpDTO = SignUpDTO.builder()
							.username(kakaoProfile.getProperties().getNickname() + "_" + kakaoProfile.getId())
							.fullname("OAuth_" + kakaoProfile.getProperties().getNickname())
							.password(tencoKey)
							.build();
		
		// 2. 우리사이트 최초 소셜 사용자 인지 판별
		User oldUser = userService.searchUsername(signUpDTO.getUsername());
		if(oldUser == null) {
			oldUser = new User();
			// 사용자가 최초 소셜 로그린 사용자임
			oldUser.setUsername(signUpDTO.getUsername());
			oldUser.setPassword(null);
			oldUser.setFullname(signUpDTO.getFullname());
			
			
			// 프로필 이미지여부에 따라 조건식추가
			signUpDTO.setOriginFileName(kakaoProfile.getProperties().getThumbnailImage());
			userService.createUser(signUpDTO);
		}
		
		// 자동 로그린 처리
		session.setAttribute(Define.PRINCIPAL, oldUser);
		
		
		return "redirect:/account/list";
	}
	
	
}
