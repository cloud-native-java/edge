package edge;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableAutoConfiguration
public class Config {

 @Bean
 RestTemplate restTemplate() {
  return new RestTemplate();
 }

 @Bean
 RetryTemplate retryTemplate() {
  RetryTemplate retryTemplate = new RetryTemplate();
  ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
  backOffPolicy.setInitialInterval(30 * 1000);
  backOffPolicy.setMaxInterval(180 * 1000);
  retryTemplate.setBackOffPolicy(backOffPolicy);
  return retryTemplate;
 }

}
