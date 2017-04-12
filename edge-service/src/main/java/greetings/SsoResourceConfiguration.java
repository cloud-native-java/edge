package greetings;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;

// <1>
@Profile("sso")
@Configuration
@EnableResourceServer
class SsoResourceConfiguration extends ResourceServerConfigurerAdapter {

 @Override
 public void configure(HttpSecurity http) throws Exception {
  http.antMatcher("/api/**").authorizeRequests().anyRequest().authenticated();
 }
}
