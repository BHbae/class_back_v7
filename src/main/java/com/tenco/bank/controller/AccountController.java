package com.tenco.bank.controller;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;

import com.tenco.bank.dto.DepositDTO;
import com.tenco.bank.dto.SaveDTO;
import com.tenco.bank.dto.WithdrawalDTO;
import com.tenco.bank.dto.transferDTO;
import com.tenco.bank.handler.AuthInterceptor;
import com.tenco.bank.handler.exception.DataDeliveryException;
import com.tenco.bank.handler.exception.UnAuthorizedException;
import com.tenco.bank.repository.model.Account;
import com.tenco.bank.repository.model.HistoryAccount;
import com.tenco.bank.repository.model.User;
import com.tenco.bank.service.AccountService;
import com.tenco.bank.utils.Define;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller // IoC 대상(싱글톤으로 관리) // I 인버절 o 오프 C 컨트롤 (제어의 역전)
@RequestMapping("/account")
@RequiredArgsConstructor 
public class AccountController {

	// DI 처리
	@Autowired
	private final AccountService service;

	
	/**
	 * 계좌 생성 페이지 요청 주소 설계 : Http://localhost:8080/account/save
	 * 
	 * @return
	 */
	// 계좌 생성 화면 요청
	@GetMapping("/save")
	public String savePage() {

		
		return "account/save";
	}

	@PostMapping("/save")
	public String saveProc(SaveDTO dto, @SessionAttribute(Define.PRINCIPAL) User principal) {
		// 1. 데이터 추출 (파싱 전략)
		
		

		// 3. 유효성 검사
		if (dto.getNumber() == null || dto.getNumber().isEmpty()) {
			throw new DataDeliveryException(Define.ENTER_YOUR_ACCOUNT_NUMBER, HttpStatus.BAD_REQUEST);
		}
		if (dto.getPassword() == null || dto.getPassword().isEmpty()) {
			throw new DataDeliveryException(Define.ENTER_YOUR_PASSWORD, HttpStatus.BAD_REQUEST);
		}
		if (dto.getBalance() == null || dto.getBalance() < 0) {
			throw new DataDeliveryException(Define.ENTER_YOUR_BALANCE, HttpStatus.BAD_REQUEST);
		}

		// 4. 서비스 호출
		service.createAccount(dto, principal.getId());

		return "redirect:/account/list";
	}

	/**
	 * 계좌 목록 화면 요청 주소 설계 : http://localhost:8080/account/list
	 * 
	 * @return list.jsp
	 */
	@GetMapping("/list")
	public String listPage(Model model, @SessionAttribute(Define.PRINCIPAL) User principal) {
		// 1. 인증검사
		
		// 2. 유효성 검사?
		// 3. 서비스 호출
		List<Account> accountList = service.readAccountListByUserId(principal.getId());

		if (accountList.isEmpty()) {
			model.addAttribute("accountList", null);
		} else {
			// JSAP 데이터 넣어주는 방법
			model.addAttribute("accountList", accountList);
		}

		return "account/list";
	}

	@GetMapping("/withdrawal")
	public String withdrawalPage() {
		

		return "account/withdrawal";
	}

	@PostMapping("/withdrawal")
	public String withdrawalProc(WithdrawalDTO dto, @SessionAttribute(Define.PRINCIPAL) User principal) {
		

		// 유효성 검사 ( 자바 코드를 개발) --> 스프링 부트 @Valid 라이브러리가 존재
		if (dto.getAmount() == null) {
			throw new DataDeliveryException(Define.ENTER_YOUR_BALANCE, HttpStatus.BAD_REQUEST);
		}
		if (dto.getAmount().longValue() <= 0) {
			throw new DataDeliveryException(Define.W_BALANCE_VALUE, HttpStatus.BAD_REQUEST);
		}
		if (dto.getWAccountNumber() == null) {
			throw new DataDeliveryException(Define.ENTER_YOUR_ACCOUNT_NUMBER, HttpStatus.BAD_REQUEST);
		}
		if (dto.getWAccountPassword() == null) {
			throw new DataDeliveryException(Define.ENTER_YOUR_PASSWORD, HttpStatus.BAD_REQUEST);
		}

		service.updateAccountWithdrawal(dto, principal.getId());

		return "redirect:/account/list";
	}

	// 입금 페이지
	@GetMapping("/deposit")
	public String updateDeposit() {
		
		return "account/deposit";
	}

	@PostMapping("/deposit")
	public String depositProc(DepositDTO dto, @SessionAttribute(Define.PRINCIPAL) User principal) {
		// 인증 검사
		

		// 유효성 검사
		if (dto.getAmount() == null) {
			throw new DataDeliveryException(Define.D_BALANCE_VALUE, HttpStatus.BAD_REQUEST);
		}
		if (dto.getAmount().longValue() <= 0) {
			throw new DataDeliveryException(Define.D_BALANCE_VALUE, HttpStatus.BAD_REQUEST);
		}
		if (dto.getDAccountNumber() == null) {
			throw new DataDeliveryException(Define.ENTER_YOUR_ACCOUNT_NUMBER, HttpStatus.BAD_REQUEST);
		}

		service.updateAccountDeposit(dto, principal.getId());

		return "redirect:/account/list";
	}

	// 이체 페이지 요청
	@GetMapping("/transfer")
	public String updateTransper() {
		
		return "account/transfer";
	}

	@PostMapping("/transfer")
	public String transfetProc(transferDTO dto, @SessionAttribute(Define.PRINCIPAL) User principal) {
		// 인증 검사
		

		// 유효성 검사
		if (dto.getAmount() == null) {
			throw new DataDeliveryException(Define.D_BALANCE_VALUE, HttpStatus.BAD_REQUEST);
		}
		if (dto.getAmount().longValue() <= 0) {
			throw new DataDeliveryException(Define.D_BALANCE_VALUE, HttpStatus.BAD_REQUEST);
		}
		if (dto.getDAccountNumber() == null) {
			throw new DataDeliveryException(Define.ENTER_YOUR_ACCOUNT_NUMBER, HttpStatus.BAD_REQUEST);
		}
		if (dto.getWAccountNumber() == null) {
			throw new DataDeliveryException(Define.ENTER_YOUR_ACCOUNT_NUMBER, HttpStatus.BAD_REQUEST);
		}
		if (dto.getPassword() == null) {
			throw new DataDeliveryException(Define.ENTER_YOUR_PASSWORD, HttpStatus.BAD_REQUEST);
		}
		service.updateAccountTransfer(dto, principal.getId());

		return "redirect:/account/list";
	}

	/**
	 * 계좌 상세 보기 페이지 주소 설계 : http://localhost:8080/account/detail/1?type=all,
	 * deposit, withdraw
	 * 
	 * @return
	 */
	@GetMapping("/detail/{accountId}")
	public String detail(@PathVariable(name = "accountId") Integer accountId,
			@RequestParam(required = false, name = "type") String type, 
			@RequestParam(name = "page", defaultValue = "1") int page,
			@RequestParam(name = "size", defaultValue = "2") int size,Model model) {
		
		
		
		
		// 유효성 검사
		List<String> validTypes = Arrays.asList("all", "deposit", "withdrawal");
		
		if(!validTypes.contains(type)) {
			throw new DataDeliveryException("유효하지 않은 접근 입니다", HttpStatus.BAD_REQUEST);
		}
		
		// 페이지 개수를 계산하기 위해서 총 페이지 수를 계산해주어야한다.
		int totalRecords = service.countHistoryByAccountAndType(type, accountId);
		
		int totalPages = (int)Math.ceil((double)totalRecords / size);
		
		Account account = service.readAccountById(accountId);
		
		List<HistoryAccount> historyList = service.readHistoryByAccountId(type, accountId, page, size);
		
		model.addAttribute("account", account);
		model.addAttribute("historyList", historyList);
		
		model.addAttribute("currentPage", page); 
		model.addAttribute("totalPages", totalPages);
		model.addAttribute("type", type);
		model.addAttribute("size", size);
		
		return "account/detail";
	}

}
