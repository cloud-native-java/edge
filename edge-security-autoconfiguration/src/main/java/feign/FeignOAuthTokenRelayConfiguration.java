package feign;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.filter.OAuth2ClientContextFilter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;


// todo: https://jfconavarrete.wordpress.com/2014/09/15/make-spring-security-context-available-inside-a-hystrix-command/

@Configuration
@ConditionalOnWebApplication
@ConditionalOnBean(OAuth2ClientContextFilter.class)
@ConditionalOnClass({EnableResourceServer.class, RequestInterceptor.class})
public class FeignOAuthTokenRelayConfiguration {

    // this works because we added @EnableOAuth2Client to the the services
    @Bean
    public RequestInterceptor requestInterceptor(OAuth2ClientContext clientContext) {
        return requestTemplate ->
                requestTemplate.header(HttpHeaders.AUTHORIZATION,
                        clientContext.getAccessToken().getTokenType() + ' ' +
                                clientContext.getAccessToken().getValue());
    }
}
