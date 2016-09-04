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
    private String scopes = "openid";

    // https://tools.ietf.org/html/rfc6749
    private String authorizedGrantTypes =
            "implicit," +  // redirects back to JS clients with a token, not just an authorization code
                    "password," + // no need for redirect: username & pw go in, token comes out (eg, mobile)
                    "client_credentials," +   // authenticate the client only (no need to worry about user)
                    "refresh_token"; // don't provide an access token. provide a refresh token which may be subsequently redeemed for an access token.

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
