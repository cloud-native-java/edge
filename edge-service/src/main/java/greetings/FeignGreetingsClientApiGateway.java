package greetings;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Profile("feign")
@RestController
@RequestMapping("/api")
class FeignGreetingsClientApiGateway {

 private final GreetingsClient greetingsClient;

 @Autowired
 FeignGreetingsClientApiGateway(GreetingsClient greetingsClient) {
  this.greetingsClient = greetingsClient;
 }

 @RequestMapping(method = RequestMethod.GET, value = "/feign/{name}")
 Map<String, String> feign(@PathVariable String name) {
  return this.greetingsClient.greet(name);
 }

}
