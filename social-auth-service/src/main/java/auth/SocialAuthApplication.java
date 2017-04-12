package auth;

import auth.accounts.Account;
import auth.accounts.AccountRepository;
import auth.clients.Client;
import auth.clients.ClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.oauth2.resource.ResourceServerProperties;
import org.springframework.boot.autoconfigure.security.oauth2.resource.UserInfoTokenServices;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.filter.OAuth2ClientAuthenticationProcessingFilter;
import org.springframework.security.oauth2.client.filter.OAuth2ClientContextFilter;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeResourceDetails;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.*;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.filter.CompositeFilter;

import javax.servlet.Filter;
import java.security.Principal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@EnableDiscoveryClient
@EnableOAuth2Client
@SpringBootApplication
public class SocialAuthApplication {

 public static void main(String[] args) {
  SpringApplication.run(SocialAuthApplication.class, args);
 }
}

@RestController
class PrincipalRestController {

 @RequestMapping({ "/user", "/me" })
 Map<String, String> user(Principal principal) {
  Map<String, String> map = new LinkedHashMap<>();
  map.put("name", principal.getName());
  return map;
 }
}

@Configuration
@EnableResourceServer
class ResourceServerConfiguration extends ResourceServerConfigurerAdapter {

 @Override
 public void configure(HttpSecurity http) throws Exception {
  // @formatter:off
        http.antMatcher("/me").authorizeRequests().anyRequest().authenticated();
        http.antMatcher("/user").authorizeRequests().anyRequest().authenticated();
        // @formatter:on
 }
}

@Configuration
@EnableAuthorizationServer
class AuthorizationServerConfiguration extends WebSecurityConfigurerAdapter
 implements AuthorizationServerConfigurer {

 private final OAuth2ClientContext oauth2ClientContext;

 private final ClientDetailsService clientDetailsService;

 @Autowired
 public AuthorizationServerConfiguration(
  OAuth2ClientContext oauth2ClientContext,
  ClientDetailsService clientDetailsService) {
  super();
  this.oauth2ClientContext = oauth2ClientContext;
  this.clientDetailsService = clientDetailsService;
 }

 @Override
 public void configure(AuthorizationServerEndpointsConfigurer endpoints)
  throws Exception {
 }

 @Override
 protected void configure(HttpSecurity http) throws Exception {
  // @formatter:off
        http.antMatcher("/**").authorizeRequests()
                .antMatchers("/", "/login**", "/webjars/**").permitAll().anyRequest()
                .authenticated().and().exceptionHandling()
                .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/")).and().logout()
                .logoutSuccessUrl("/").permitAll().and().csrf()
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()).and()
                .addFilterBefore(ssoFilter(), BasicAuthenticationFilter.class);
        // @formatter:on
 }

 @Bean
 FilterRegistrationBean oauth2ClientFilterRegistration(
  OAuth2ClientContextFilter filter) {
  FilterRegistrationBean registration = new FilterRegistrationBean();
  registration.setFilter(filter);
  registration.setOrder(-100);
  return registration;
 }

 @Bean
 @ConfigurationProperties("github")
 ClientResources github() {
  return new ClientResources();
 }

 @Bean
 @ConfigurationProperties("facebook")
 ClientResources facebook() {
  return new ClientResources();
 }

 private Filter ssoFilter() {
  CompositeFilter filter = new CompositeFilter();
  List<Filter> filters = Arrays.asList(
   ssoFilter(facebook(), "/login/facebook"),
   ssoFilter(github(), "/login/github"));
  filter.setFilters(filters);
  return filter;
 }

 private Filter ssoFilter(ClientResources client, String path) {

  OAuth2ClientAuthenticationProcessingFilter filter = new OAuth2ClientAuthenticationProcessingFilter(
   path);

  OAuth2RestTemplate template = new OAuth2RestTemplate(client.getClient(),
   oauth2ClientContext);
  filter.setRestTemplate(template);
  filter.setTokenServices(new UserInfoTokenServices(client.getResource()
   .getUserInfoUri(), client.getClient().getClientId()));
  return filter;
 }

 @Override
 public void configure(AuthorizationServerSecurityConfigurer security)
  throws Exception {
 }

 @Override
 public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
  clients.withClientDetails(this.clientDetailsService);
 }
}

@Component
class DataCommandLineRunner implements CommandLineRunner {

 private final AccountRepository accountRepository;

 private final ClientRepository clientRepository;

 @Autowired
 public DataCommandLineRunner(AccountRepository accountRepository,
  ClientRepository clientRepository) {
  this.accountRepository = accountRepository;
  this.clientRepository = clientRepository;
 }

 @Override
 public void run(String... args) throws Exception {

  Stream
   .of("dsyer,cloud", "pwebb,boot", "mminella,batch", "rwinch,security",
    "jlong,spring")
   .map(s -> s.split(","))
   .forEach(
    tuple -> accountRepository.save(new Account(tuple[0], tuple[1], true)));

  Stream.of("html5,secret", "android,secret").map(x -> x.split(","))
   .forEach(x -> clientRepository.save(new Client(x[0], x[1])));
 }
}

class ClientResources {

 @NestedConfigurationProperty
 private AuthorizationCodeResourceDetails client = new AuthorizationCodeResourceDetails();

 @NestedConfigurationProperty
 private ResourceServerProperties resource = new ResourceServerProperties();

 public AuthorizationCodeResourceDetails getClient() {
  return client;
 }

 public ResourceServerProperties getResource() {
  return resource;
 }
}
