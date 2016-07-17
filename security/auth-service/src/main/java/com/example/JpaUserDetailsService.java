package com.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

// <1>
@Service
class JpaUserDetailsService implements UserDetailsService {

	private final AccountRepository accountRepository;

	@Autowired
	public JpaUserDetailsService(AccountRepository accountRepository) {
		this.accountRepository = accountRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String username)
			throws UsernameNotFoundException {

		return this.accountRepository
				.findByUsername(username)
				.map(account -> {

					boolean active = account.isActive();

					return new User(account.getUsername(), account
							.getPassword(), active, active, active, active,
							AuthorityUtils.createAuthorityList("ROLE_ADMIN",
									"ROLE_USER"));
				})
				.orElseThrow(
						() -> new UsernameNotFoundException(
								"username not found!"));
	}
}
