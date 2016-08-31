package com.example;

import feign.RequestInterceptor;
import org.apache.catalina.servlet4preview.http.HttpServletRequest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.util.StringUtils;

import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;

/**
 * this works with Feign only so long as {@code hystrix.command.default.execution.isolation.strategy=SEMAPHORE}
 * is specified somewhere. Feign delegates to Hystrix for isolation against possibly errant calls.
 * The default strategy for Hystrix is to use thread-based isolation, which means that the Feign request
 * is executed in a seaprate thread the incoming {@link HttpServletRequest}, which means that
 * none of Spring MVC's apparata are available.
 */
@Configuration
@ConditionalOnWebApplication
public class FeignOAuthTokenRelayConfiguration {

    private static final ThreadLocal<String> AUTH_HEADER = new ThreadLocal<>();

    @Bean
    @ConditionalOnClass(EnableResourceServer.class)
    ServletRequestListener authorizationHeaderListener() {
        return new ServletRequestListener() {

            private Log log = LogFactory.getLog(getClass());

            @Override
            public void requestDestroyed(ServletRequestEvent sre) {
            }

            @Override
            public void requestInitialized(ServletRequestEvent sre) {
                HttpServletRequest request = HttpServletRequest.class.cast(
                        sre.getServletRequest());
                String auth = request.getHeader("Authorization");
                if (StringUtils.hasText(auth)) {
                    AUTH_HEADER.set(auth);
                    log.debug("stored the auth header..");
                }
            }
        };
    }

    @Bean
    @ConditionalOnClass(RequestInterceptor.class)
    RequestInterceptor oAuthFeignRequestInterceptor() {
        return template -> template.header("Authorization", AUTH_HEADER.get());
    }
}
