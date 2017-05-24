package auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;

//@formatter:off
import org.springframework.security.oauth2
        .config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2
        .config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2
        .config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2
        .config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2
        .provider.ClientDetailsService;
//@formatter:on

@Configuration
@EnableAuthorizationServer
class AuthorizationServerConfiguration extends
 AuthorizationServerConfigurerAdapter {

 private final AuthenticationManager authenticationManager;

 private final ClientDetailsService clientDetailsService;

 @Autowired
 public AuthorizationServerConfiguration(
  AuthenticationManager authenticationManager,
  ClientDetailsService clientDetailsService) {
  this.authenticationManager = authenticationManager;
  this.clientDetailsService = clientDetailsService;
 }

 @Override
 public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
  // <1>
  clients.withClientDetails(this.clientDetailsService);
 }

 @Override
 public void configure(AuthorizationServerEndpointsConfigurer endpoints)
  throws Exception {
  // <2>
  endpoints.authenticationManager(this.authenticationManager);
 }
}
