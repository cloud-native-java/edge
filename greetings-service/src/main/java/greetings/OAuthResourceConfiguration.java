package greetings;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableOAuth2Client;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;

@Profile("secure") // <1>
@Configuration
@EnableResourceServer // <2>
@EnableOAuth2Client // <3>
class OAuthResourceConfiguration {
}
