package com.example;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public class ClientDetails {

	@Id
	@GeneratedValue
	private Long id;

	private String clientId, secret;
	private String scopes, authorizedGrantTypes; // todo some way to store arrays?

	public ClientDetails(
			String clientId,
			String secret,
			String scopes,
			String authorizedGrantTypes) {
		this.clientId = clientId;
		this.secret = secret;
		this.scopes = scopes;
		this.authorizedGrantTypes = authorizedGrantTypes;
	}

	ClientDetails() {
	}

	public Long getId() {
		return id;
	}

	public String getClientId() {
		return clientId;
	}

	public String getSecret() {
		return secret;
	}

	public String getScopes() {
		return scopes;
	}

	public String getAuthorizedGrantTypes() {
		return authorizedGrantTypes;
	}
}
