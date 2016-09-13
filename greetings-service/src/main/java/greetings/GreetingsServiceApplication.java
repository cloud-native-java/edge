package greetings;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.web.bind.annotation.*;

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

@RestController
@RequestMapping(method = RequestMethod.GET, value = "/{name}")
class GreetingsRestController {


    private Log log = LogFactory.getLog(getClass());

    // <2>
    @RequestMapping
    Map<String, String> hi(@PathVariable String name) {
        log.info("this is not a proxied request.");
        return this.doHi(name);
    }

    @RequestMapping(headers = "x-forwarded-for")
    Map<String, String> hi(@PathVariable String name,
                           @RequestHeader("x-forwarded-for") String forwardedFor,
                           @RequestHeader("x-forwarded-proto") String proto,
                           @RequestHeader("x-forwarded-host") String host,
                           @RequestHeader("x-forwarded-port") int port,
                           @RequestHeader("x-forwarded-prefix") String prefix) {

        log.info(String.format("processing a proxied request from %s://%s:%s " +
                "with prefix %s for service %s", proto, host, port, prefix, forwardedFor));

        return this.doHi(name);
    }

    private Map<String, String> doHi(String name) {
        return Collections.singletonMap("greeting", "Hello, " + name + "!");
    }
}