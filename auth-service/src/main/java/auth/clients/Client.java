package auth.clients;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public class Client {

    @Id
    @GeneratedValue
    private Long id;

    private String clientId, secret;

    private String scopes = "openid", authorizedGrantTypes = "password,client_credentials,refresh_token";

    private String authorities = "ROLE_USER,ROLE_ADMIN";

    public Client(String clientId, String clientSecret) {
        this.clientId = clientId;
        this.secret = clientSecret;
    }

    Client() {
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
