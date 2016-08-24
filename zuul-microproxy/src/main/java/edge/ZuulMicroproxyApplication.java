package edge;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.cloud.netflix.zuul.filters.RouteLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

@EnableZuulProxy // <1>
@SpringBootApplication
public class ZuulMicroproxyApplication {

    @Bean
    CommandLineRunner commandLineRunner(RouteLocator routeLocator) {
        return args -> routeLocator.getRoutes().forEach(r -> {
            System.out.println(r.toString());
        });
    }

    public static void main(String args[]) {
        SpringApplication.run(ZuulMicroproxyApplication.class, args);
    }
}

@Component
class DeviceDelayFilter extends ZuulFilter {

    @Override
    public String filterType() {
        return null;
    }

    @Override
    public int filterOrder() {
        return 0;
    }

    @Override
    public boolean shouldFilter() {
        return false;
    }

    @Override
    public Object run() {
        RequestContext ctx = RequestContext.getCurrentContext();
        HttpServletRequest request = ctx.getRequest();
        ctx.getZuulRequestHeaders().forEach((k, v) -> System.out.println(k + '=' + v));
//        log.info(String.format("%s request to %s", request.getMethod(), request.getRequestURL().toString()));

        return null;

    }
}

