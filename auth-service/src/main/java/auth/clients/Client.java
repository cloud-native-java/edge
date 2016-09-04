package auth.clients;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public class Client {

    @Id
    @GeneratedValue
    private Long id;

    private String clientId;
    private String secret;
    private String scopes = "openid";
    private String authorizedGrantTypes = "authorization_code,refresh_token,password";
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
