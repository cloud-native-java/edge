package greetings;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.oauth2.resource.UserInfoRestTemplateFactory;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableOAuth2Client;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Collections;
import java.util.Map;

// <1>
@EnableDiscoveryClient
@SpringBootApplication
public class GreetingsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GreetingsServiceApplication.class, args);
    }
}

@Profile("secure")
@Configuration
@EnableResourceServer
@EnableOAuth2Client
class OAuthResourceConfiguration {
}

@RestController
@RequestMapping(method = RequestMethod.GET, value = "/greet/{name}")
class GreetingsRestController {

    private Log log = LogFactory.getLog(getClass());

    // <2>
    @RequestMapping
    Map<String, String> hi(@PathVariable String name, Principal p) {
        log.info("responded to a direct request.");
        log.info(debugPrincipal(p));
        return this.doHi(name);
    }

    @RequestMapping(headers = "x-forwarded-for")
    Map<String, String> hi(@PathVariable String name,
                           @RequestHeader("x-forwarded-for") String forwardedFor,
                           @RequestHeader("x-forwarded-proto") String proto,
                           @RequestHeader("x-forwarded-host") String host,
                           @RequestHeader("x-forwarded-port") int port,
                           @RequestHeader("x-forwarded-prefix") String prefix,
                           Principal p) {

        log.info(String.format("responded to a proxied request debugPrincipal %s://%s:%s " +
                "with prefix %s for service %s.", proto, host, port, prefix, forwardedFor));
        log.info(debugPrincipal(p));

        return this.doHi(name);
    }

    private String debugPrincipal(Principal p) {
        return p == null ? "no principal." : String.format("principal: %s.", p.getName());
    }

    private Map<String, String> doHi(String name) {
        return Collections.singletonMap("greeting", "Hello, " + name + "!");
    }
}