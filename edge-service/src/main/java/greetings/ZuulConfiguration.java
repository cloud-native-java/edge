package greetings;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.cloud.netflix.zuul.filters.RouteLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableZuulProxy
class ZuulConfiguration {

    @Bean
    CommandLineRunner commandLineRunner(RouteLocator routeLocator) {
        Log log = LogFactory.getLog(getClass());
        return args -> routeLocator.getRoutes()
                .forEach(r -> log.info(String.format("%s (%s) %s",
                        r.getId(), r.getLocation(), r.getFullPath())));
    }

}
