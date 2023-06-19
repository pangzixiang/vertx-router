package io.github.pangzixiang.whatsit.vertx.router.handler;

import io.github.pangzixiang.whatsit.vertx.router.model.TargetService;
import io.github.pangzixiang.whatsit.vertx.router.options.VertxRouterVerticleOptions;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.proxy.handler.ProxyHandler;
import io.vertx.httpproxy.HttpProxy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

import static io.github.pangzixiang.whatsit.vertx.router.VertxRouterVerticle.CONNECTION_MAP;

@Slf4j
public class ListenerWebsocketHandler implements Handler<RoutingContext> {

    private final Vertx vertx;
    private final String instanceId;
    private final HttpClient proxyClient;

    private static final String LISTENER_REGISTER_CONNECTION_REGISTER_LOCK_NAME = "vertx-router-register-connection-lock-" + UUID.randomUUID();

    private static final String LISTENER_REGISTER_CONNECTION_REMOVE_LOCK_NAME = "vertx-router-remove-connection-lock-" + UUID.randomUUID();

    public ListenerWebsocketHandler(Vertx vertx, VertxRouterVerticleOptions vertxRouterVerticleOptions, String instanceId) {
        this.vertx = vertx;
        this.instanceId = instanceId;
        this.proxyClient = vertx.createHttpClient(Objects.requireNonNullElse(vertxRouterVerticleOptions.getProxyHttpClientOptions(),
                new HttpClientOptions().setMaxPoolSize(10).setPoolEventLoopSize(10)));
    }

    @Override
    public void handle(RoutingContext routingContext) {
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
                vertx.sharedData().getLock(LISTENER_REGISTER_CONNECTION_REMOVE_LOCK_NAME).onSuccess(lock -> {
                    TargetService targetService = (TargetService) vertx.sharedData().getLocalMap(CONNECTION_MAP).get(serviceName);
                    if (targetService != null) {
                        log.info("Connection [{}] closed, remove proxy for service [{}] to target server [{}:{}] (instance={})",
                                connectionId, serviceName, host, port, instanceId);
                        vertx.sharedData().getLocalMap(CONNECTION_MAP).put(serviceName, targetService.remove(connectionId));
                    }
                    lock.release();
                }).onFailure(throwable -> {
                    log.error("Unable to remove connection [{}] for proxy service [{}] to target server [{}:{}] as failed to get register lock (instance={})",
                            connectionId, serviceName, host, port, instanceId, throwable);
                });
            });

            if (StringUtils.isNoneEmpty(host, serviceName)) {
                HttpProxy httpProxy = HttpProxy.reverseProxy(proxyClient);
                httpProxy.origin(port, host);
                ProxyHandler proxyHandler = ProxyHandler.create(httpProxy);
                vertx.sharedData().getLock(LISTENER_REGISTER_CONNECTION_REGISTER_LOCK_NAME).onSuccess(lock -> {
                    TargetService targetService = (TargetService) vertx.sharedData().getLocalMap(CONNECTION_MAP).getOrDefault(serviceName, new TargetService());
                    targetService.add(connectionId, host, port, proxyHandler);
                    vertx.sharedData().getLocalMap(CONNECTION_MAP).put(serviceName, targetService);
                    log.info("Connection [{}] established, added proxy for service [{}] to target server [{}:{}] (instance={})",
                            connectionId, serviceName, host, port, instanceId);
                    lock.release();
                }).onFailure(throwable -> {
                    log.error("Unable to register connection [{}] for proxy service [{}] to target server [{}:{}] as failed to get register lock (instance={})",
                            connectionId, serviceName, host, port, instanceId, throwable);
                    serverWebSocket.close();
                });
            } else {
                serverWebSocket.close();
            }
        }).onFailure(throwable -> {
            log.error("Unable to upgrade http request to Websocket (instance={})", instanceId, throwable);
            routingContext.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).end();
        });
    }
}
