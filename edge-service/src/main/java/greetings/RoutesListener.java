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

	private final RouteLocator routeLocator;
	private final DiscoveryClient discoveryClient;

	private Log logger = LogFactory.getLog(getClass());

	@Autowired
	public RoutesListener(DiscoveryClient dc, RouteLocator rl) {
		this.routeLocator = rl;
		this.discoveryClient = dc;
	}

	// <1>
	@EventListener(HeartbeatEvent.class)
	public void onHeartbeatEvent(HeartbeatEvent event) {
		this.logger.info("onHeartbeatEvent()");
		this.discoveryClient.getServices().stream().map(x -> " " + x)
				.forEach(this.logger::info);
	}

	// <2>
	@EventListener(RoutesRefreshedEvent.class)
	public void onRoutesRefreshedEvent(RoutesRefreshedEvent event) {
		this.logger.info("onRoutesRefreshedEvent()");
		this.routeLocator.getRoutes().stream().map(x -> " " + x).forEach(this.logger::info);
	}
}
