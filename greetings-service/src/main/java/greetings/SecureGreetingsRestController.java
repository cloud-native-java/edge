package greetings;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Collections;
import java.util.Map;

@Profile("secure")
@RestController
@RequestMapping(method = RequestMethod.GET, value = "/greet/{name}")
public class SecureGreetingsRestController {

    private Log log = LogFactory.getLog(getClass());

    @RequestMapping
    Map<String, String> hi(@PathVariable String name, Principal p) {
        log.info(p == null ? "(no principal)" : String.format("principal: %s.", p.getName()));
        return Collections.singletonMap("greeting", "Hello, " + name + "!");
    }

}


