package greetings;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
//@formatter:off
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.config.annotation.web
        .configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web
        .configuration.ResourceServerConfigurerAdapter;
//@formatter:on

// <1>
@Profile("secure")
@Configuration
@EnableResourceServer
class SecureResourceConfiguration extends ResourceServerConfigurerAdapter {

 @Override
 public void configure(HttpSecurity http) throws Exception {
  http.antMatcher("/api/**").authorizeRequests() // <2>
   .anyRequest().authenticated();
 }
}
