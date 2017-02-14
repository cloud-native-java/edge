package auth.clients;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.event.HeartbeatEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.ClientRegistrationException;
import org.springframework.security.oauth2.provider.client.BaseClientDetails;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

@Configuration
public class ClientConfiguration {

	private final Set<String> discoveryClientIps = new ConcurrentSkipListSet<>();
	private final DiscoveryClient discoveryClient;

	@Autowired
	public ClientConfiguration(DiscoveryClient discoveryClient) {
		this.discoveryClient = discoveryClient;
		this.refreshBasedOnHeartbeat();
	}

	@EventListener(HeartbeatEvent.class)
	public void refreshBasedOnHeartbeat() {
		List<String> addresses = this.discoveryClient.getInstances("greetings-client")
				.stream().map(si -> "http://" + si.getHost() + ':' + si.getPort() + '/')
				.collect(Collectors.toList());

		synchronized (this.discoveryClientIps) {
			this.discoveryClientIps.clear();
			this.discoveryClientIps.addAll(addresses);
		}
	}

	@Bean
	ClientDetailsService clientDetailsService(DiscoveryClient dc,
			ClientRepository clientRepository) {
		return clientId -> clientRepository
				.findByClientId(clientId)
				.map(
						client -> {
							BaseClientDetails details = new BaseClientDetails(client.getClientId(),
									null, client.getScopes(), client.getAuthorizedGrantTypes(), client
											.getAuthorities());
							details.setClientSecret(client.getSecret());

							// <1>
							// details.setAutoApproveScopes(Arrays.asList(client.getAutoApproveScopes().split(",")));

							// <2>
							String greetingsClientRedirectUri = this.discoveryClientIps
									.stream()
									.findAny()
									.orElseThrow(
											() -> new ClientRegistrationException(
													"couldn't find and bind a greetings-client IP"));

							details.setRegisteredRedirectUri(Collections
									.singleton(greetingsClientRedirectUri));
							return details;
						})
				.orElseThrow(
						() -> new ClientRegistrationException(String.format(
								"no client %s registered", clientId)));
	}
}
