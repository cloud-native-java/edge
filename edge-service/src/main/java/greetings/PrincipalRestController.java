package greetings;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@Profile("sso")
@RestController
class PrincipalRestController {

 @RequestMapping("/user")
 public Principal user(Principal principal) {
  return principal;
 }
}
