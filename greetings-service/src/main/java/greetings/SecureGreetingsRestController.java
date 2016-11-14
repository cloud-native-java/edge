package greetings;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Collections;
import java.util.Map;

@Profile("secure")
@RestController
@RequestMapping(method = RequestMethod.GET, value = "/greet")
public class SecureGreetingsRestController {

    @RequestMapping
    Map<String, String> hi(Principal p) {
        return Collections.singletonMap("greeting", "Hello, " + p.getName() + "!");
    }
}


