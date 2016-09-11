package edge;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.cloud.netflix.zuul.EnableZuulServer;
import org.springframework.cloud.netflix.zuul.filters.RouteLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

@EnableDiscoveryClient
@EnableZuulProxy // <1>
@SpringBootApplication
public class ZuulMicroproxyApplication {

    @Bean
    CommandLineRunner commandLineRunner(RouteLocator routeLocator) {
        return args -> routeLocator.getRoutes().forEach(r -> System.out.println(r.toString()));
    }

    @Component
    public static class SimpleZuulFilter extends ZuulFilter {

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

            RequestContext.getCurrentContext()
                    .getZuulRequestHeaders().forEach((k, v) -> System.out.println(k + '=' + v));

            return null;
        }
    }

    public static void main(String args[]) {
        SpringApplication.run(ZuulMicroproxyApplication.class, args);
    }
}

