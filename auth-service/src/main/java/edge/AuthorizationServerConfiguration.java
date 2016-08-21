package edge;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.oauth2.config.annotation.builders.ClientDetailsServiceBuilder;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;

// <1>
@Configuration
@EnableAuthorizationServer
class AuthorizationServerConfiguration extends AuthorizationServerConfigurerAdapter {

    private final JpaClientDetailsService clientDetailsService;

    private final AuthenticationManager authenticationManager;

    @Autowired
    public AuthorizationServerConfiguration(AuthenticationManager authenticationManager,
                                            JpaClientDetailsService jpaClientDetailsService) {
        this.authenticationManager = authenticationManager;
        this.clientDetailsService = jpaClientDetailsService;
    }

    @Override
    public void configure(ClientDetailsServiceConfigurer clients)
            throws Exception {

//        clients.and().clients(clientDetailsService).build();
     /*   clients
                .inMemory()
                    .withClient("acme")
                    .secret("acmesecret")
                    .scopes("openid")
                    .authorizedGrantTypes("password");*/
    }

    @Override
    public void configure(AuthorizationServerEndpointsConfigurer endpoints)
            throws Exception {
         endpoints.authenticationManager(this.authenticationManager);
    }
}
