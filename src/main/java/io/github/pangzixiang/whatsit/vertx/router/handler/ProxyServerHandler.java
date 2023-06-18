package io.github.pangzixiang.whatsit.vertx.router.handler;

import io.github.pangzixiang.whatsit.vertx.router.exception.TargetServerNotFoundException;
import io.github.pangzixiang.whatsit.vertx.router.model.TargetService;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.proxy.handler.ProxyHandler;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

import static io.github.pangzixiang.whatsit.vertx.router.VertxRouterVerticle.CONNECTION_MAP;

@Slf4j
@AllArgsConstructor
public class ProxyServerHandler implements Handler<RoutingContext> {
    private final Vertx vertx;

    @Override
    public void handle(RoutingContext routingContext) {
        String serviceName = routingContext.pathParam("serviceName");

        TargetService targetService = (TargetService) vertx.sharedData().getLocalMap(CONNECTION_MAP).get(serviceName);
        if (targetService == null) {
            throw new TargetServerNotFoundException(serviceName);
        }

        Map<String, ProxyHandler> proxyHandlers = targetService.getProxyHandlers();
        if (proxyHandlers.isEmpty()) {
            throw new TargetServerNotFoundException(serviceName);
        }
        long startTime = System.currentTimeMillis();
        int random = Math.floorMod(System.currentTimeMillis(), proxyHandlers.size());
        ProxyHandler proxyHandler = proxyHandlers.values().stream().toList().get(random);
        String connectionId = proxyHandlers.keySet().stream().toList().get(random);
        log.info("Start to proxy request [{}] from [{}] to [{}:{}]", routingContext.normalizedPath(), routingContext.request().host(), targetService.getHosts().get(connectionId), targetService.getPorts().get(connectionId));
        routingContext.addEndHandler(result -> {
            if (result.succeeded()) {
                log.info("Succeeded to proxy request [{}] from [{}] to [{}:{}] (time={}ms)",
                        routingContext.normalizedPath(), routingContext.request().host(), targetService.getHosts().get(connectionId), targetService.getPorts().get(connectionId), System.currentTimeMillis() - startTime);
            } else {
                log.error("Failed to proxy request [{}] from [{}] to [{}:{}] (time={}ms)",
                        routingContext.normalizedPath(), routingContext.request().host(), targetService.getHosts().get(connectionId), targetService.getPorts().get(connectionId), System.currentTimeMillis() - startTime, result.cause());
            }
        });
        proxyHandler.handle(routingContext);
    }
}
