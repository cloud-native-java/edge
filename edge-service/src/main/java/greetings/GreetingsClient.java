package greetings;

import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Map;

@FeignClient(serviceId = "greetings-service")
interface GreetingsClient {

	@RequestMapping(method = RequestMethod.GET, value = "/greet/{name}")
	Map<String, String> greet(@PathVariable("name") String name);
}
