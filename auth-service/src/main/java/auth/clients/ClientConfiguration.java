package auth.clients;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.ClientRegistrationException;
import org.springframework.security.oauth2.provider.client.BaseClientDetails;

@Configuration
public class ClientConfiguration {

    @Bean
    ClientDetailsService clientDetailsService(ClientRepository clientRepository) {
        return clientId -> clientRepository.findByClientId(clientId)
            .map(client -> {
                BaseClientDetails details = new BaseClientDetails(client.getClientId(), null, client.getScopes(),
                        client.getAuthorizedGrantTypes(), client.getAuthorities());
                details.setClientSecret(client.getSecret());
                return details;
            })
            .orElseThrow(() -> new ClientRegistrationException(String.format("no client %s registered", clientId)));
    }
}
