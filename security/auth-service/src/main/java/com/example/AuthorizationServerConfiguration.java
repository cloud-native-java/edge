package com.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;

// <1>
@EnableAuthorizationServer
@Configuration
class AuthorizationServerConfiguration
		extends AuthorizationServerConfigurerAdapter {

	@Autowired
	private AuthenticationManager authenticationManager;

	@Autowired
	private JpaClientDetailsService jpaClientDetailsService;

	@Override
	public void configure(ClientDetailsServiceConfigurer clients)
			throws Exception {
		//clients.inMemory().clients(this.jpaClientDetailsService);
		//clients.withClientDetails(this.jpaClientDetailsService);
		clients.inMemory()
				.withClient("acme")
				.secret("acmesecret")
				.scopes("openid")
				.authorizedGrantTypes("password");

	}

	@Override
	public void configure(AuthorizationServerEndpointsConfigurer endpoints)
			throws Exception {
		endpoints.authenticationManager(this.authenticationManager);
	}
}
