package greetings;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

@Profile("zuul")
@RestController
@RequestMapping(method = RequestMethod.GET, value = "/greet/{name}")
public class ProxyAwareGreetingsRestController {

	private Log log = LogFactory.getLog(getClass());

	@RequestMapping(headers = "x-forwarded-for")
	Map<String, String> hi(@PathVariable String name,
			@RequestHeader("x-forwarded-for") String forwardedFor,
			@RequestHeader("x-forwarded-proto") String proto,
			@RequestHeader("x-forwarded-host") String host,
			@RequestHeader("x-forwarded-port") int port,
			@RequestHeader("x-forwarded-prefix") String prefix) {

		log.info(String.format("responded to a proxied request debugPrincipal %s://%s:%s "
				+ "with prefix %s for service %s.", proto, host, port, prefix, forwardedFor));

		return Collections.singletonMap("greeting", "Hello, " + name + "!");
	}
}
