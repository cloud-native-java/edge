package greetings;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Profile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Created by jlong on 10/10/16.
 */
@Profile("default")
@RestController
@RequestMapping("/api")
class RestTemplateGreetingsClientApiGateway {

    private final RestTemplate restTemplate;

    @Autowired
    RestTemplateGreetingsClientApiGateway(@LoadBalanced RestTemplate restTemplate) { // <1>
        this.restTemplate = restTemplate;
    }

    @RequestMapping(method = RequestMethod.GET, value = "/resttemplate/{name}")
    Map<String, String> restTemplate(@PathVariable String name) {

        ParameterizedTypeReference<Map<String, String>> type =
                new ParameterizedTypeReference<Map<String, String>>() {
                };

        return this.restTemplate.exchange(
                "http://greetings-service/greet/{name}", HttpMethod.GET, null, type, name).getBody();
    }
}
