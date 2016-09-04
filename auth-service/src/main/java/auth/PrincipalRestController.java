package auth;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Collections;
import java.util.Map;

@RestController
class PrincipalRestController {

    //@RequestMapping("/user")  Principal principal(Principal principal) {
    //    return principal;
    //}

    @RequestMapping("/user")
    Map<String, String> principal(Principal principal) {
        return Collections.singletonMap("name", principal.getName());
    }
}