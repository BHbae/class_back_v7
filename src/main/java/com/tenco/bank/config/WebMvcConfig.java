package com.tenco.bank.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.tenco.bank.handler.AuthInterceptor;

import lombok.RequiredArgsConstructor;

@Configuration // 1개 이상의 Bean을 등록 할 떄 설정
@RequiredArgsConstructor // <-- 생성자 대신 사용 가능
public class WebMvcConfig implements WebMvcConfigurer {
	
	@Autowired // DI
	private final AuthInterceptor authInterceptor;
	
	// 우리가 만들어 놓은 AuthInterceptor 를 등록 해야 함.
	@Override
	public void addInterceptors(InterceptorRegistry registry) {

		registry.addInterceptor(authInterceptor)
			.addPathPatterns("/account/**")
			.addPathPatterns("/auth/**");
	}
	
	// 코드 추가
	// C:\\work_spring\\uploads <--  서버 컴퓨터 상에 실제 이미지결로지만
	// 프로젝트 상에서(클라이언트가 HTML 소스로 보이는 경로는) /images/upload/**
	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/images/uploads/**")
		.addResourceLocations("file:\\C:\\work_spring\\uploads/");
	}
	
	
	
	@Bean // IoC 대상 (싱글톤 처리)
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
	
}
