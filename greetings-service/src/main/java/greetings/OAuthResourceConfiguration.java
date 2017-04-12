package greetings;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableOAuth2Client;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;

@Configuration
// <1>
@Profile("secure")
// <2>
@EnableResourceServer
// <3>
@EnableOAuth2Client
class OAuthResourceConfiguration {
}
