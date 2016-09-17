package edge;

import com.google.common.util.concurrent.RateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ThrottlingConfiguration {

    // different clients have different SLAs and rates.
    // they express their SLA through (headers, or something else)
    // every 30 seconds a request will be let through
    @Bean
    public RateLimiter rateLimiter() {
        return RateLimiter.create(1.0D / 30.0D);
    }
}
