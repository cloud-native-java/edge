package com.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

//import org.springframework.security.oauth2.client.OAuth2ClientContext;
//import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;

// <1>
//@EnableResourceServer
@EnableDiscoveryClient
@EnableFeignClients
@SpringBootApplication
public class GreetingsClientApplication {
/*

    @Bean
    RequestInterceptor feignOAuthRequestInterceptor(OAuth2ClientContext oauth2ClientContext) {

        Log log = LogFactory.getLog(getClass());

        return template -> {
            String token = "Bearer";
            String header = "Authorization";
            if (template.headers().containsKey(header)) {
                log.warn("The Authorization token has been already set");
            } else if (oauth2ClientContext.getAccessTokenRequest().getExistingToken() == null) {
                log.warn("Can not obtain existing token for request, if it is a non secured request, ignore.");
            } else {
                log.debug(String.format("Constructing Header %s for Token %s", header, token));
                template.header(header, String.format("%s %s", token,
                        oauth2ClientContext.getAccessTokenRequest().getExistingToken().toString()));
            }
        };
    }
*/

    public static void main(String[] args) {
        SpringApplication.run(GreetingsClientApplication.class, args);
    }
}

@RestController
class GreetingsClientRestController {

    @RequestMapping(method = RequestMethod.GET, value = "/hi")
    public Map<String, String> read() {
        return this.greetingsClient.greet("josh");
    }

    private final GreetingsClient greetingsClient;

    @Autowired
    public GreetingsClientRestController(GreetingsClient greetingsClient) {
        this.greetingsClient = greetingsClient;
    }
}

@FeignClient("greetings-service")
interface GreetingsClient {

    @RequestMapping(method = RequestMethod.GET, value = "/hi/{name}")
    Map<String, String> greet(@PathVariable("name") String name);
}
