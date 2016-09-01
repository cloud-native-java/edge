package resource;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.security.oauth2.resource.UserInfoRestTemplateCustomizer;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class DiscoveryAwareResourceServerAutoConfiguration {


    @Bean
    public UserInfoRestTemplateCustomizer userInfoRestTemplateCustomizer(
            LoadBalancerInterceptor loadBalancerInterceptor) {

        Log log = LogFactory.getLog(getClass());
        log.debug("configuring a " + DiscoveryClient.class.getName() + "-aware " +
                OAuth2RestTemplate.class.getName());

        return (OAuth2RestTemplate template) -> {
            List<ClientHttpRequestInterceptor> list = new ArrayList<>(
                    template.getInterceptors());
            list.add(loadBalancerInterceptor);
            template.setInterceptors(list);
        };
    }

}
