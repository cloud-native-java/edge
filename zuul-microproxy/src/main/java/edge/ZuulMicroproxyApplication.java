package edge;

import com.google.common.util.concurrent.RateLimiter;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.cloud.netflix.zuul.filters.RouteLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;


@EnableDiscoveryClient
@EnableZuulProxy // <1>
@SpringBootApplication
public class ZuulMicroproxyApplication {

    @Bean
    CommandLineRunner commandLineRunner(RouteLocator routeLocator) {
        return args -> routeLocator.getRoutes().forEach(r -> System.out.println(r.toString()));
    }

    // this could be request or session scoped, as well.
    @Bean
    RateLimiter rateLimiter(@Value("${zuul.ratelimit.requests-per-second:10}") int rps) {
        return RateLimiter.create(rps, 0, TimeUnit.SECONDS);
    }

    public static void main(String args[]) {
        SpringApplication.run(ZuulMicroproxyApplication.class, args);
    }
}

@Component
class ThrottlingFilter implements Filter {

    private final HttpStatus tooManyRequests = HttpStatus.TOO_MANY_REQUESTS;
    private final RateLimiter rateLimiter;

    @Autowired
    public ThrottlingFilter(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        if (rateLimiter.tryAcquire()) {
            chain.doFilter(request, response);
        }
        else if (HttpServletResponse.class.isAssignableFrom(response.getClass())) {
            HttpServletResponse r = HttpServletResponse.class.cast(response);
            r.getWriter().append(tooManyRequests.getReasonPhrase());
            r.setContentType(MediaType.TEXT_PLAIN_VALUE);
            r.setStatus(tooManyRequests.value());
        }
    }

    @Override
    public void destroy() {
    }
}


// todo look into token-bucket algorithm to implement rate limiting in Zuul.
// TODO github.com/vladimir-bukhtoyarov/bucket4j looks like a sound implementation based on a
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
        return true;
    }

    @Override
    public Object run() {
        HttpServletRequest request = RequestContext.getCurrentContext().getRequest();
        Collections.list(request.getHeaderNames()).forEach(k -> log.info(k + '=' + request.getHeader(k)));
        log.info("in " + getClass().getName());
        return null;
    }
}
