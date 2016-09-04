package auth;

import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.filter.OAuth2ClientAuthenticationProcessingFilter;
import org.springframework.security.oauth2.client.filter.OAuth2ClientContextFilter;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeResourceDetails;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.*;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.filter.CompositeFilter;

import javax.servlet.Filter;
import java.security.Principal;
import java.util.*;


// TODO http://blog.jdriven.com/2016/09/securing-application-landscape-spring-cloud-security-part-1/

@SpringBootApplication
@EnableDiscoveryClient
@EnableResourceServer
@EnableOAuth2Client
public class SocialAuthServiceApplication {

    @Configuration
    @EnableAuthorizationServer
    public static class AuthorizationServer extends AuthorizationServerConfigurerAdapter {

        private final ClientDetailsService clientDetailsService;

        @Override
        public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
            clients.withClientDetails(clientDetailsService);
        }

        @Autowired
        public AuthorizationServer(ClientDetailsService clientDetailsService) {
            this.clientDetailsService = clientDetailsService;
        }
    }

    @RestController
    public static class PrincipalRestController {

        @RequestMapping({"/user", "/me"})
        public Map<String, String> user(Principal principal) {
            Map<String, String> map = new HashMap<>();
            map.put("name", principal.getName());
            return map;
        }
    }

    @Order(6)
    @Configuration
    public static class WebSecurityConfiguration extends WebSecurityConfigurerAdapter {

        private final OAuth2ClientContext oAuth2ClientContext;

        @Autowired
        public WebSecurityConfiguration(OAuth2ClientContext oAuth2ClientContext) {
            this.oAuth2ClientContext = oAuth2ClientContext;
        }

        @Bean
        @ConfigurationProperties("facebook")
        ClientResources facebook() {
            return new ClientResources();
        }

        @Bean
        @ConfigurationProperties("github")
        ClientResources github() {
            return new ClientResources();
        }

        @Bean
        FilterRegistrationBean oauth2ClientFilterRegistration(
                OAuth2ClientContextFilter filter) {
            FilterRegistrationBean registration = new FilterRegistrationBean();
            registration.setFilter(filter);
            registration.setOrder(-100);
            return registration;
        }

        @Override
        protected void configure(HttpSecurity http) throws Exception {

            // @formatter:off
            http.antMatcher("/**")
                    .authorizeRequests().antMatchers("/", "/login**", "/webjars/**").permitAll()
                    .anyRequest().authenticated().and()
                    .exceptionHandling()
                    .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/")).and()
                    .logout().logoutSuccessUrl("/").permitAll().and()
                    .csrf().csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()).and()
                    .addFilterBefore(ssoFilter(), BasicAuthenticationFilter.class);
            // @formatter:on
        }

        private Filter ssoFilter(ClientResources client, String path) {
            OAuth2ClientAuthenticationProcessingFilter filter = new OAuth2ClientAuthenticationProcessingFilter(path);
            OAuth2RestTemplate template = new OAuth2RestTemplate(client.getClient(),
                    this.oAuth2ClientContext);
            filter.setRestTemplate(template);
            filter.setTokenServices(new UserInfoTokenServices(
                    client.getResource().getUserInfoUri(), client.getClient().getClientId()));
            return filter;
        }

        private Filter ssoFilter() {
            CompositeFilter filter = new CompositeFilter();
            List<Filter> filters = new ArrayList<>();
            filters.add(ssoFilter(facebook(), "/login/facebook"));
            filters.add(ssoFilter(github(), "/login/github"));
            filter.setFilters(filters);
            return filter;
        }
    }

    @Configuration
    @EnableResourceServer
    public static class ResourceServerConfiguration extends ResourceServerConfigurerAdapter {

        @Override
        public void configure(HttpSecurity http) throws Exception {
            // @formatter:off
            http.antMatcher("/me").authorizeRequests().anyRequest().authenticated();
            // @formatter:on
        }
    }

    public static class ClientResources {

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

    public static void main(String[] args) {
        SpringApplication.run(SocialAuthServiceApplication.class, args);
    }
}