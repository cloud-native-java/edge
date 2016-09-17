package edge;

import org.apache.commons.logging.LogFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.cloud.netflix.zuul.filters.RouteLocator;
import org.springframework.context.annotation.Bean;


/**
 * https://github.com/Netflix/zuul/wiki/How-We-Use-Zuul-At-Netflix
 * http://microservices.io/patterns/apigateway.html
 * Netflix say they say they also do more than just a token bucket.
 * they also detect malicious activity from a particular user and feed that back into the Zuul filter as well
 */
@EnableDiscoveryClient
@EnableZuulProxy // <1>
@SpringBootApplication
public class ZuulMicroproxyApplication {

    @Bean
    CommandLineRunner commandLineRunner(RouteLocator routeLocator) {
        return args -> routeLocator.getRoutes().forEach(r -> LogFactory.getLog(getClass()).info(r.toString()));
    }

    public static void main(String args[]) {
        SpringApplication.run(ZuulMicroproxyApplication.class, args);
    }
}