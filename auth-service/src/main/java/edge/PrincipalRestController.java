package edge ;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
class PrincipalRestController {

	// <1>
	@RequestMapping("/user")
	Principal principal(Principal principal) {
		return principal;
	}
}
