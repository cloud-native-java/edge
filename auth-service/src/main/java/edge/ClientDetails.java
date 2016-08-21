package edge ;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public class ClientDetails {

	@Id
	@GeneratedValue
	private Long id;

	private String clientId, secret;
	private String scopes, authorizedGrantTypes;
	private String authorities = "ROLE_USER,ROLE_ADMIN";

	public ClientDetails(String clientId, String secret, String scopes,
	                     String authorizedGrantTypes) {
		this.clientId = clientId;
		this.secret = secret;
		this.scopes = scopes;
		this.authorizedGrantTypes = authorizedGrantTypes;
	}

	public ClientDetails(String clientId, String secret, String scopes,
	                     String authorizedGrantTypes, String authorities) {
		this(clientId, secret, scopes, authorizedGrantTypes);
		this.authorities = authorities;
	}

	ClientDetails() {
	}

	public String getAuthorities() {
		return authorities;
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
