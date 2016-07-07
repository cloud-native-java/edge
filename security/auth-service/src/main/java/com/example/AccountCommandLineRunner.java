package com.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.stream.Stream;


@Component
class AccountCommandLineRunner implements CommandLineRunner {

	private final AccountRepository accountRepository;

	@Autowired
	public AccountCommandLineRunner(AccountRepository accountRepository) {
		this.accountRepository = accountRepository;
	}

	// <1>
	@Override
	public void run(String... strings) throws Exception {
		Stream.of("dsyer,cloud", "pwebb,boot", "mminella,batch")
				.map(s -> s.split(","))
				.forEach(tuple -> accountRepository.save(new Account(
						tuple[0], tuple[1], true)));
	}
}
