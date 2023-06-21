package io.github.pangzixiang.whatsit.vertx.router;

import io.github.pangzixiang.whatsit.vertx.router.exception.TargetServerNotFoundException;
import io.github.pangzixiang.whatsit.vertx.router.model.TargetService;
import io.github.pangzixiang.whatsit.vertx.router.options.VertxRouterVerticleOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.proxy.handler.ProxyHandler;
import io.vertx.httpproxy.HttpProxy;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiFunction;

import static io.github.pangzixiang.whatsit.vertx.router.VertxRouterVerticle.*;

@Slf4j
public class ProxyServerVerticle extends AbstractVerticle {
    private final String instanceId = UUID.randomUUID().toString();

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        VertxRouterVerticleOptions vertxRouterVerticleOptions = (VertxRouterVerticleOptions)
                getVertx().sharedData().getLocalMap(VERTX_ROUTER_SHARE_MAP_NAME).get(VERTX_ROUTER_OPTIONS_KEY_NAME);

        HttpServer proxyServer = getVertx().createHttpServer(vertxRouterVerticleOptions.getProxyServerOptions());

        Router proxyRouter = Router.router(getVertx());
        proxyRouter.route().handler(routingContext -> {
            log.debug("Proxy Server received request from {} {} [instance={}]", routingContext.request().method(), routingContext.normalizedPath(), instanceId);
            routingContext.next();
        });
        HttpClient proxyClient = getVertx().createHttpClient(Objects.requireNonNullElse(vertxRouterVerticleOptions.getProxyHttpClientOptions(),
                new HttpClientOptions().setMaxPoolSize(10).setPoolEventLoopSize(10)));
        HttpProxy httpProxy = HttpProxy.reverseProxy(proxyClient);

        BiFunction<String, Map<String, SocketAddress>, Future<SocketAddress>> customOriginServerSelector = vertxRouterVerticleOptions.getCustomOriginServerSelector();
        httpProxy.originSelector(httpServerRequest -> {
            String serviceName = httpServerRequest.params().get("serviceName");
            TargetService targetService = (TargetService) vertx.sharedData().getLocalMap(CONNECTION_MAP).get(serviceName);
            if (targetService == null) {
                log.info("target service [{}] not found for URI [{} {}]", serviceName, httpServerRequest.method(), httpServerRequest.uri());
                return Future.failedFuture(new TargetServerNotFoundException(serviceName));
            }
            Map<String, SocketAddress> socketAddressMap = targetService.getSocketAddressMap();
            if (socketAddressMap.isEmpty()) {
                log.info("target service [{}] not found for URI [{} {}]", serviceName, httpServerRequest.method(), httpServerRequest.uri());
                return Future.failedFuture(new TargetServerNotFoundException(serviceName));
            }
            Future<SocketAddress> socketAddressFuture;
            if (customOriginServerSelector != null) {
                socketAddressFuture = customOriginServerSelector.apply(serviceName, socketAddressMap);
            } else {
                socketAddressFuture = Future.succeededFuture(socketAddressMap.values().stream().toList().
                        get(Math.floorMod(System.currentTimeMillis(), socketAddressMap.size())));
            }


            return socketAddressFuture.onSuccess(socketAddress -> {
                log.info("Proxy request [{} {}] from [{}] to [{}:{}] (instance={})", httpServerRequest.method(),
                        httpServerRequest.uri(), httpServerRequest.host(),
                        socketAddress.host(), socketAddress.port(), instanceId);
            });
        });

        ProxyHandler proxyHandler = ProxyHandler.create(httpProxy);

        proxyRouter.route("/:serviceName/*").handler(proxyHandler);

        proxyServer.requestHandler(proxyRouter);

        Future<HttpServer> proxyServerFuture = proxyServer.listen(vertxRouterVerticleOptions.getProxyServerPort());
        proxyServerFuture.onSuccess(httpServer -> {
            log.info("Vertx Router Proxy Server started at port {} (instance={})", httpServer.actualPort(), instanceId);
            startPromise.complete();
        }).onFailure(startPromise::fail);
    }
}
