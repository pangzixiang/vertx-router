package io.github.pangzixiang.whatsit.vertx.router.handler;

import io.github.pangzixiang.whatsit.vertx.router.model.TargetService;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.proxy.handler.ProxyHandler;
import io.vertx.httpproxy.HttpProxy;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

import static io.github.pangzixiang.whatsit.vertx.router.VertxRouterVerticle.CONNECTION_MAP;

@AllArgsConstructor
@Slf4j
public class ListenerWebsocketHandler implements Handler<RoutingContext> {

    private final Vertx vertx;

    @Override
    public void handle(RoutingContext routingContext) {
        HttpClient proxyClient = vertx.createHttpClient();
        Future<ServerWebSocket> serverWebSocketFuture = routingContext.request().toWebSocket();
        serverWebSocketFuture.onSuccess(serverWebSocket -> {
            String connectionId = UUID.randomUUID().toString();

            MultiMap headers = serverWebSocket.headers();
            String host = headers.get("host");
            String serviceName = headers.get("name");
            int port;
            try {
                port = Integer.parseInt(headers.get("port"));
            } catch (NumberFormatException e) {
                serverWebSocket.close();
                return;
            }

            serverWebSocket.closeHandler(unused -> {
                TargetService targetService = (TargetService) vertx.sharedData().getLocalMap(CONNECTION_MAP).get(serviceName);
                if (targetService != null) {
                    Map<String, ProxyHandler> maps = targetService.getProxyHandlers();
                    maps.remove(connectionId);
                    targetService.setProxyHandlers(maps);
                    vertx.sharedData().getLocalMap(CONNECTION_MAP).put(serviceName, targetService);
                    log.info("Connection [{}] closed, remove proxy for service [{}] to target server [{}:{}]",
                            connectionId, serviceName, targetService.getHosts().get(connectionId), targetService.getPorts().get(connectionId));
                }
            });

            if (StringUtils.isNoneEmpty(host, serviceName)) {
                HttpProxy httpProxy = HttpProxy.reverseProxy(proxyClient);
                httpProxy.origin(port, host);
                ProxyHandler proxyHandler = ProxyHandler.create(httpProxy);
                TargetService targetService = (TargetService) vertx.sharedData().getLocalMap(CONNECTION_MAP).get(serviceName);
                if (targetService == null) {
                    targetService = new TargetService().add(connectionId, host, port, proxyHandler);
                } else {
                    targetService.add(connectionId, host, port, proxyHandler);
                }
                vertx.sharedData().getLocalMap(CONNECTION_MAP).put(serviceName, targetService);
                log.info("Connection [{}] established, added proxy for service [{}] to target server [{}:{}]",
                        connectionId, serviceName, host, port);
            } else {
                serverWebSocket.close();
                return;
            }
            serverWebSocket.handler(buffer -> {
                log.info("Connection {} received message {}", connectionId, buffer);
            });
        }).onFailure(throwable -> {
            routingContext.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).end();
        });
    }
}
