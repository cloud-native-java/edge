package com.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableOAuth2Client;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

//import org.springframework.security.oauth2.client.OAuth2ClientContext;
//import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
// https://jfconavarrete.wordpress.com/2014/09/15/make-spring-security-context-available-inside-a-hystrix-command/

// <1>
@EnableResourceServer
@EnableOAuth2Client
@EnableDiscoveryClient
@EnableFeignClients
@SpringBootApplication
public class GreetingsClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(GreetingsClientApplication.class, args);
    }
}


@RestController
class GreetingsClientRestController {

    private final GreetingsClient greetingsClient;

    @Autowired
    public GreetingsClientRestController(GreetingsClient greetingsClient) {
        this.greetingsClient = greetingsClient;
    }

    @RequestMapping(method = RequestMethod.GET, value = "/hi")
    Map<String, String> read() {
        return this.greetingsClient.greet("josh");
    }
}

@FeignClient(serviceId = "greetings-service")
interface GreetingsClient {

    @RequestMapping(method = RequestMethod.GET, value = "/hi/{name}")
    Map<String, String> greet(@PathVariable("name") String name);
}
