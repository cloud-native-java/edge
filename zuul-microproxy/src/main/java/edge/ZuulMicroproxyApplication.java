package edge;

import com.google.common.util.concurrent.RateLimiter;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.cloud.netflix.zuul.filters.RouteLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;


@EnableDiscoveryClient
@EnableZuulProxy // <1>
@SpringBootApplication
@EnableConfigurationProperties(RateLimiterProperties.class)
public class ZuulMicroproxyApplication {

    @Bean
    CommandLineRunner commandLineRunner(RouteLocator routeLocator) {
        return args -> routeLocator.getRoutes().forEach(r -> LogFactory.getLog(getClass()).info(r.toString()));
    }

    // this could be request or session scoped, as well.
    @Bean
    RateLimiter rateLimiter(RateLimiterProperties rateLimiterProperties) {
        return RateLimiter.create(rateLimiterProperties.getPermitsPerSecond());
    }

    public static void main(String args[]) {
        SpringApplication.run(ZuulMicroproxyApplication.class, args);
    }
}

@Component
@ConfigurationProperties(prefix = "ratelimiter")
class RateLimiterProperties {

    private double permitsPerSecond = 1 / 30.0;

    public double getPermitsPerSecond() {
        return permitsPerSecond;
    }
}

@Component
class ThrottlingZuulFilter extends ZuulFilter {

    private final Log log = LogFactory.getLog(getClass());
    private final HttpStatus tooManyRequests = HttpStatus.TOO_MANY_REQUESTS;
    private final RateLimiter rateLimiter;

    @Autowired
    public ThrottlingZuulFilter(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    public String filterType() {
        return "pre";
    }

    @Override
    public int filterOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() {
        try {
            log.info("in " + this.getClass().getName());
            RequestContext currentContext = RequestContext.getCurrentContext();
            HttpServletResponse response = currentContext.getResponse();
            if (!rateLimiter.tryAcquire()) {
                response.setContentType(MediaType.TEXT_PLAIN_VALUE);
                response.setStatus(this.tooManyRequests.value());
                response.getWriter().append(this.tooManyRequests.getReasonPhrase());
                currentContext.setSendZuulResponse(false);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return null;
    }
}

// post from some folks at Netlfix. they say they also do more than just a token bucket. they
// also detect malicious activity from a particular user and feed that back into the Zuul filter as well

@Component
class RequestHeaderLoggingZuulFilter extends ZuulFilter {

    private Log log = LogFactory.getLog(getClass());

    @Override
    public String filterType() {
        return "pre";
    }

    @Override
    public int filterOrder() {
        return 0;
    }

    @Override
    public boolean shouldFilter() {
        return RequestContext.getCurrentContext().getZuulEngineRan();
    }

    @Override
    public Object run() {
        log.info("in " + this.getClass().getName());
        HttpServletRequest request = RequestContext.getCurrentContext().getRequest();
        Collections.list(request.getHeaderNames()).forEach(k -> log.debug(k + '=' + request.getHeader(k)));
        return null;
    }
}

