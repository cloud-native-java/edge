package com.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.stream.Stream;

@Component
class ClientAndAccountsCommandLineRunner implements CommandLineRunner {

	private final AccountRepository accountRepository;
	private final ClientDetailsRepository clientDetailsRepository;

	@Autowired
	public ClientAndAccountsCommandLineRunner(AccountRepository accountRepository,
	                                          ClientDetailsRepository clientDetailsRepository) {
		this.accountRepository = accountRepository;
		this.clientDetailsRepository = clientDetailsRepository;
	}

	// <1>
	@Override
	public void run(String... strings) throws Exception {


		Stream.of("acme", "html5", "android", "ios")
				.map(a -> new String[]{a, a + "secret"})
				.forEach(tuple -> clientDetailsRepository.save(new ClientDetails(
						tuple[0], tuple[1], "openid", "password"
				)));

		Stream.of("jlong,spring", "dsyer,cloud", "pwebb,boot", "mminella,batch")
				.map(s -> s.split(","))
				.forEach(
						tuple -> accountRepository.save(new Account(tuple[0],
								tuple[1], true)));
	}
}
