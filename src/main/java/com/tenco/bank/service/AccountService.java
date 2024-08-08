package com.tenco.bank.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tenco.bank.dto.DepositDTO;
import com.tenco.bank.dto.SaveDTO;
import com.tenco.bank.dto.WithdrawalDTO;
import com.tenco.bank.dto.transferDTO;
import com.tenco.bank.handler.exception.DataDeliveryException;
import com.tenco.bank.handler.exception.RedirectException;
import com.tenco.bank.repository.interfaces.AccountRepository;
import com.tenco.bank.repository.interfaces.HistoryRepository;
import com.tenco.bank.repository.model.Account;
import com.tenco.bank.repository.model.History;
import com.tenco.bank.utils.Define;

@Service
public class AccountService {

	private final AccountRepository repository;
	private final HistoryRepository historyRepository;

	@Autowired // 생략 가능 - DI 처리
	public AccountService(AccountRepository repository, HistoryRepository historyRepository) {
		this.repository = repository;
		this.historyRepository = historyRepository;
	}

	/**
	 * 계좌 생성 기능
	 * 
	 * @param dto
	 * @param id
	 */
	// 트랜 잭션 처리
	@Transactional
	public void createAccount(SaveDTO dto, Integer principalId) {
		int result = 0;

		try {
			result = repository.insert(dto.toAccount(principalId));

		} catch (DataAccessException e) {
			throw new DataDeliveryException("잘못된 요청입니다.", HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (Exception e) {
			throw new RedirectException("알 수 없는 오류 입니다.", HttpStatus.SERVICE_UNAVAILABLE);
		}
		if (result == 0) {
			throw new DataDeliveryException("정상 처리 되지 않았습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
		}

	}

	public List<Account> readAccountListByUserId(Integer userId) {
		List<Account> accountListEntity = null;

		try {
			accountListEntity = repository.findByUserId(userId);
		} catch (DataAccessException e) {
			throw new DataDeliveryException("잘못된 처리 입니다", HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (Exception e) {
			throw new RedirectException("알수없는 오류", HttpStatus.SERVICE_UNAVAILABLE);
		}

		return accountListEntity;
	}

	// 1. 계좌 존재 여부 확인 -- select
	// 2. 본인 계좌 여부 확인 -- 객체 상태값에서 비교
	// 3. 계좌 비밀번 확인 -- 객체 상태값에서 일치 여부 확인
	// 4. 잔액 여부 확인 -- 객체 상태값에서 확인
	// 5. 출금 처리 -- update
	// 6. 거래내역 등록 -- insert(history)
	// 7. 트랜잭션 처리

	@Transactional
	public void updateAccountWithdrawal(WithdrawalDTO dto, Integer principalId) {
		int result = 0;
		// 1
		Account accountEntity = repository.findByNumber(dto.getWAccountNumber());
		if (accountEntity == null) {
			throw new DataDeliveryException(Define.NOT_EXIST_ACCOUNT, HttpStatus.BAD_REQUEST);
		}
		// 2
		accountEntity.checkOwner(principalId);

		// 3
		accountEntity.checkPassword(dto.getWAccountPassword());

		// 4
		accountEntity.checkBalance(dto.getAmount());

		// 5 accountEntity 객체의 잔액을 변경하고 업데이트 처리한다.
		accountEntity.withdraw(dto.getAmount());
		// update 처리
		repository.updateById(accountEntity);

		// 6 - 거래내역 등록
		History history = new History();
		history.setAmount(dto.getAmount());
		history.setWBalance(accountEntity.getBalance());
		history.setDBalance(null);
		history.setWAccountId(accountEntity.getId());
		history.setDAccountId(null);

		int rowResultCount = historyRepository.insert(history);
		if (rowResultCount != 1) {
			throw new DataDeliveryException(Define.FAILED_PROCESSING, HttpStatus.INTERNAL_SERVER_ERROR);
		}

	}

	@Transactional
	public void updateAccountDeposit(DepositDTO dto, Integer prinpalId) {
		// 1. 계좌 존재 여부 확인
		Account accountEntity = repository.findByNumber(dto.getDAccountNumber());
		if (accountEntity == null) {
			throw new DataDeliveryException(Define.NOT_EXIST_ACCOUNT, HttpStatus.BAD_REQUEST);
		}

		// 2. 입금 처리 -- 잔액변경 업데이트
		accountEntity.deposit(dto.getAmount());
		repository.updateById(accountEntity);

		// 3. 거래내역 등록 -- insert(history)
		History history = new History();
		history.setAmount(dto.getAmount());

		history.setDAccountId(accountEntity.getId());
		history.setDBalance(accountEntity.getBalance());

		history.setWBalance(null);
		history.setWAccountId(null);
		

		int rowResultCount = historyRepository.insert(history);
		if (rowResultCount != 1) {
			throw new DataDeliveryException(Define.FAILED_PROCESSING, HttpStatus.INTERNAL_SERVER_ERROR);
		}

	}
	
	// 이체 기능 만들기
	// 1. 출금 계좌 존재 여부 - select
	// 2. 입금 계좌 존재 여부 - select
	// 3. 출금 계좌 본인 소유 확인 -- 객체상태값 세션에 아이디 비교
	// 4. 출금 계좌 비밀 번호 확인 -- 객체상태값 dto비밀번호와 비교
	// 5. 출금 계좌 잔액 여부 확인 -- 객체상태값 dto 계좌 와 비교
	
	// 6. 출금 계좌 객체 상태값 변경 처리 (잔액 다운 처리)
	// 7. 입금 계좌 객체 상태값 변경 처리 (잔액 증가 처리)  
	
	// 8. 출금 계좌 객체 -- update 처리
	// 9. 출금 계좌 객체 -- update 처리
	
	// 10. 거래내역 등록 처리
	// 11. 트랜잭션 처리
	
	@Transactional
	public void updateAccountTransfer(transferDTO dto, Integer principalId) {
		Account wAccount = repository.findByNumber(dto.getWAccountNumber()); // 출금 계좌
		Account dAccount = repository.findByNumber(dto.getDAccountNumber()); //입금 계좌 
		// 1
		// 2
		if (dAccount == null || wAccount == null) {
			throw new DataDeliveryException(Define.NOT_EXIST_ACCOUNT, HttpStatus.BAD_REQUEST);
		}
		// 3, 4, 5,
		wAccount.checkOwner(principalId);
		wAccount.checkPassword(dto.getPassword());
		wAccount.checkBalance(dto.getAmount());
		
		// 6
		wAccount.withdraw(dto.getAmount());
		// 7
		dAccount.deposit(dto.getAmount());
		
		//8
		repository.updateById(dAccount);
		//9
		repository.updateById(wAccount);
		
		//10
		History history = new History();
		history.setAmount(dto.getAmount());

		history.setDAccountId(dAccount.getId());
		history.setDBalance(dAccount.getBalance());

		history.setWBalance(wAccount.getBalance());
		history.setWAccountId(wAccount.getId());
		

		int rowResultCount = historyRepository.insert(history);
		if (rowResultCount != 1) {
			throw new DataDeliveryException(Define.FAILED_PROCESSING, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
}
