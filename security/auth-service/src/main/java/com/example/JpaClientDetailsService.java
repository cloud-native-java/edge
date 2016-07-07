package com.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.ClientRegistrationException;
import org.springframework.security.oauth2.provider.client.BaseClientDetails;
import org.springframework.stereotype.Service;

@Service
public class JpaClientDetailsService implements ClientDetailsService {

	private final ClientDetailsRepository clientDetailsRepository;

	@Autowired
	public JpaClientDetailsService(ClientDetailsRepository clientDetailsRepository) {
		this.clientDetailsRepository = clientDetailsRepository;
	}

	@Override
	public ClientDetails loadClientByClientId(String clientId)
			throws ClientRegistrationException {

		// <1>
		return this.clientDetailsRepository
				.findByClientId(clientId)
				// <2>
				.map(cd -> new BaseClientDetails(
						cd.getClientId(),
						null,
						cd.getScopes(),
						cd.getAuthorizedGrantTypes(),
						null))
				// <2>
				.orElseThrow(() -> new ClientRegistrationException(
						String.format("no client %s registered", clientId)));
	}
}
