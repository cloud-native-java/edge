package greetings;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Profile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Profile({ "default", "insecure" })
@RestController
@RequestMapping("/api")
class RestTemplateGreetingsClientApiGateway {

 private final RestTemplate restTemplate;

 @Autowired
 RestTemplateGreetingsClientApiGateway(@LoadBalanced RestTemplate restTemplate) { // <1>
  this.restTemplate = restTemplate;
 }

 @GetMapping("/resttemplate/{name}")
 Map<String, String> restTemplate(@PathVariable String name) {

  //@formatter:off
  ParameterizedTypeReference<Map<String, String>> type =
      new ParameterizedTypeReference<Map<String, String>>() {};
  //@formatter:on

  ResponseEntity<Map<String, String>> responseEntity = this.restTemplate
   .exchange("http://greetings-service/greet/{name}", HttpMethod.GET, null,
    type, name);
  return responseEntity.getBody();
 }
}
