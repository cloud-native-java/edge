package com.example;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


// <1>
interface AccountRepository extends JpaRepository<Account, Long> {

	Optional<Account> findByUsername(String username);
}
