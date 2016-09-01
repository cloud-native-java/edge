package feign;

import org.apache.catalina.servlet4preview.http.HttpServletRequest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.util.StringUtils;

import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;




// todo: https://jfconavarrete.wordpress.com/2014/09/15/make-spring-security-context-available-inside-a-hystrix-command/


/**
 * this works with Feign only so long as {@code hystrix.command.default.execution.isolation.strategy=SEMAPHORE}
 * is specified somewhere. Feign delegates to Hystrix for isolation against possibly errant calls.
 * The default strategy for Hystrix is to use thread-based isolation, which means that the Feign request
 * is executed in a separate thread the incoming {@link HttpServletRequest}, which means that
 * none of Spring MVC's apparata are available.
 *
 * This will only work if {@code hystrix.command.default.execution.isolation.strategy} is set to {@code SEMAPHORE}.
 */
@Configuration
@ConditionalOnWebApplication
public class FeignOAuthTokenRelayConfiguration {

    private static final ThreadLocal<String> AUTH_HEADER = new ThreadLocal<>();

    @Bean
    @ConditionalOnClass(EnableResourceServer.class)
    public ServletRequestListener authorizationHeaderListener() {
        return new ServletRequestListener() {

            @Override
            public void requestInitialized(ServletRequestEvent sre) {
                HttpServletRequest request = HttpServletRequest.class.cast(
                        sre.getServletRequest());
                String auth = request.getHeader("Authorization");
                if (StringUtils.hasText(auth)) {
                    AUTH_HEADER.set(auth);
                }
            }

            @Override
            public void requestDestroyed(ServletRequestEvent sre) {
                AUTH_HEADER.remove();
            }
        };
    }

    @Bean
    @ConditionalOnClass(RequestInterceptor.class)
    public RequestInterceptor oAuthFeignRequestInterceptor() {
        return template -> template.header("Authorization", AUTH_HEADER.get());
    }
}
