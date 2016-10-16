package greetings;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.event.HeartbeatEvent;
import org.springframework.cloud.netflix.zuul.RoutesRefreshedEvent;
import org.springframework.cloud.netflix.zuul.filters.RouteLocator;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
class RoutesListener {

    private Log logger = LogFactory.getLog(getClass());
    private final RouteLocator routeLocator;
    private final DiscoveryClient discoveryClient;

    @EventListener(RoutesRefreshedEvent.class)
    public void onRoutesRefreshedEvent(RoutesRefreshedEvent event) {
        this.logger.info("onRoutesRefreshedEvent()");
        this.routeLocator.getRoutes().forEach(this.logger::info);
    }

    @EventListener(HeartbeatEvent.class)
    public void onHeartbeatEvent(HeartbeatEvent event) {
        this.logger.info("onHeartbeatEvent()");
        this.discoveryClient.getServices().forEach( this.logger::info );
    }

    @Autowired
    public RoutesListener(DiscoveryClient dc, RouteLocator routeLocator) {
        this.routeLocator = routeLocator;
        this.discoveryClient = dc;
    }
}
