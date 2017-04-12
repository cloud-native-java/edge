package auth.clients;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.util.Arrays;
import java.util.stream.Collectors;

@Entity
public class Client {

 @Id
 @GeneratedValue
 private Long id;

 private String clientId;

 private String secret;

 private String scopes = from("openid");

 private String authorizedGrantTypes = from("authorization_code",
  "refresh_token", "password");

 private String authorities = from("ROLE_USER", "ROLE_ADMIN");

 private String autoApproveScopes = from(".*");

 public Client(String clientId, String clientSecret) {
  this.clientId = clientId;
  this.secret = clientSecret;
 }

 public Client(String clientId, String secret, String[] scopes,
  String[] authorizedGrantTypes, String[] authorities,
  String[] autoApproveScopes) {
  this.clientId = clientId;
  this.secret = secret;
  this.scopes = from(scopes);
  this.authorizedGrantTypes = from(authorizedGrantTypes);
  this.authorities = from(authorities);
  this.autoApproveScopes = from(autoApproveScopes);
 }

 Client() {
 }

 // <1>
 private static String from(String... arr) {
  return Arrays.stream(arr).collect(Collectors.joining(","));
 }

 public String getScopes() {
  return scopes;
 }

 public String getAuthorizedGrantTypes() {
  return authorizedGrantTypes;
 }

 public String getAuthorities() {
  return authorities;
 }

 public String getAutoApproveScopes() {
  return autoApproveScopes;
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
}
