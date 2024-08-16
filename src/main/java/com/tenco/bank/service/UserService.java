package com.tenco.bank.service;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.tenco.bank.dto.SignInDTO;
import com.tenco.bank.dto.SignUpDTO;
import com.tenco.bank.handler.exception.DataDeliveryException;
import com.tenco.bank.handler.exception.RedirectException;
import com.tenco.bank.repository.interfaces.UserRepository;
import com.tenco.bank.repository.model.User;
import com.tenco.bank.utils.Define;

import lombok.RequiredArgsConstructor;

@Service // IoC 대상( 싱글톤 으로 관리)
@RequiredArgsConstructor
public class UserService {
	
	// DI - 의존 주입 
	@Autowired
	private final UserRepository repository;
	@Autowired
	private final PasswordEncoder passwordEncoder;
	
	// 초기 파라메터 가져오는 방법
	@Value("${file.upload-dir}")
	private String uploadDir;
	
	/**
	 * 
	 * 회원 등록 서비스 기능
	 * 트랜젝션 처리 
	 * @param dto
	 */
	@Transactional // 트랜잭션 처리는 반드시 습관화
	public void createUser(SignUpDTO dto) {
		int result = 0;
		
		if(dto.getMFile() != null && !dto.getMFile().isEmpty()) {
			// 파일 업로드 로직 구현
			String[] fileNames = uploadFile(dto.getMFile());
			
			dto.setOriginFileName(fileNames[0]);
			dto.setUproadFileName(fileNames[1]);
			
		}
		
		try {
			// 회원 가입 요청시 사용자가 던진 비밀번호 값을 암호화 처리 해야함
			String hashPwd = passwordEncoder.encode(dto.getPassword());
			dto.setPassword(hashPwd);
			
			result = repository.insert(dto.toUser());
		} catch (DataAccessException e) {
			throw new DataDeliveryException("중복된 이름을 사용할 수 없습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (Exception e) {
			throw new RedirectException("알 수 없는 오류", HttpStatus.SERVICE_UNAVAILABLE);
		}
		
		if(result != 1) {
			throw new DataDeliveryException("회원가입 실패", HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
	} //  end of createUser
	

	public User readUser(SignInDTO dto) {
		// 유효성 검사는 Controller 에서 먼저 하자.
		User userEntity = null; // 지역 변수 선언
		
		// 기능 수정
		// username 으로만  --> select
		// 2가지의 경우의 수 --> 객체가 존재,null
		// 객체안에 사용자의 password가 존재 한다.(암호화 되어있는값)
		// passwordEncoder 안에 matches 메서드를 사용해서 판별한다
		
		try {
			userEntity = repository.findByUsername(dto.getUsername());
		} catch (DataDeliveryException e) {
			throw new DataDeliveryException("잘못된 처리 입니다.", HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (Exception e) {
			throw new RedirectException("알 수 없는 오류", HttpStatus.SERVICE_UNAVAILABLE);
		}
		
		if(userEntity == null) {
			throw new DataDeliveryException("존재하지 않는 아이디 입니다.", HttpStatus.BAD_REQUEST);
		} 
													
		boolean isPwdMatched = passwordEncoder.matches(dto.getPassword(), userEntity.getPassword());
		
		if(isPwdMatched == false) {
			throw new DataDeliveryException("비밀번호가 틀렸습니다", HttpStatus.BAD_REQUEST);
		}
		
		return userEntity;
	}
	
	// 파일 업로드 구현
	private String[] uploadFile(MultipartFile mFile) {
		
		// 크기
		if(mFile.getSize() > Define.MAX_FILE_SIZE) {
			throw new DataDeliveryException("파일 크기는 20MB 이상 클 수 없습니다", HttpStatus.BAD_REQUEST);
		}
		// 코드 수정
		// File - getAbsolutePath(); : 파일 시스템의 절대 경로를 나타냅니다.
		// (리눅스 또는 MacOS)에 맞춰서 절대 경로가 생성을 시킬 수 있다.
		String saveDirectory = uploadDir;
		System.out.println("saveDirectory : " + saveDirectory);
		
		
		
		// 파일이름 생성(중복 이름 예방)
		String uploadFileName = UUID.randomUUID() + "_" + mFile.getOriginalFilename();
		
		// 파일 전체  경로 + 파일 경로 구분자 + 새로 생성한 파일명
		//String uploadPath = saveDirectory + File.separator + uploadFileName;
		String uploadPath = saveDirectory + File.separator + uploadFileName;
		
		
		
		File destination = new File(uploadPath);
		
		// 반드시 수행
		try {
			mFile.transferTo(destination);
		} catch (IllegalStateException | IOException e) {
			e.printStackTrace();
			throw new DataDeliveryException("파일 업로드 중에 오류가 발생 했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		return new String[] {mFile.getOriginalFilename(), uploadFileName};
	}
	
	/**
	 * username 사용자 존재여부 조회
	 * @param strong username
	 * @return
	 */
	public User searchUsername(String username) {
		return repository.findByUsername(username);
	}
	
}
