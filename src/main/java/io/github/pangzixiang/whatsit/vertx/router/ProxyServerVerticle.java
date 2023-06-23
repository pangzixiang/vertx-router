package io.github.pangzixiang.whatsit.vertx.router;

import io.github.pangzixiang.whatsit.vertx.router.algorithm.LoadBalanceAlgorithm;
import io.github.pangzixiang.whatsit.vertx.router.exception.TargetServerNotFoundException;
import io.github.pangzixiang.whatsit.vertx.router.model.TargetService;
import io.github.pangzixiang.whatsit.vertx.router.options.VertxRouterVerticleOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.*;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.proxy.handler.ProxyHandler;
import io.vertx.httpproxy.*;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static io.github.pangzixiang.whatsit.vertx.router.VertxRouterVerticle.*;

@Slf4j
public class ProxyServerVerticle extends AbstractVerticle {
    private final String instanceId = UUID.randomUUID().toString();

    private static final String PROXY_HEADERS_X_HANDLE_ID = "x-handle-id";
    private static final String PROXY_HEADERS_X_PROXY_SERVER_HOST = "x-proxy-server-host";
    private static final String PROXY_HEADERS_X_ORIGIN_SERVER_HOST = "x-origin-server-host";
    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        VertxRouterVerticleOptions vertxRouterVerticleOptions = (VertxRouterVerticleOptions)
                getVertx().sharedData().getLocalMap(VERTX_ROUTER_SHARE_MAP_NAME).get(VERTX_ROUTER_OPTIONS_KEY_NAME);

        HttpServer proxyServer = getVertx().createHttpServer(vertxRouterVerticleOptions.getProxyServerOptions());

        Router proxyRouter = Router.router(getVertx());

        HttpClient proxyClient = getVertx().createHttpClient(Objects.requireNonNullElse(vertxRouterVerticleOptions.getProxyHttpClientOptions(),
                new HttpClientOptions().setMaxPoolSize(10).setPoolEventLoopSize(10)));
        HttpProxy httpProxy = HttpProxy.reverseProxy(proxyClient);

        LoadBalanceAlgorithm loadBalanceAlgorithm = vertxRouterVerticleOptions.getLoadBalanceAlgorithm();
        if (loadBalanceAlgorithm == null) {
            String err = "Mandatory field LoadBalanceAlgorithm in VertxRouterVerticleOptions can't be null!";
            startPromise.fail(err);
            return;
        }
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

            return loadBalanceAlgorithm.handle(getVertx(), httpServerRequest, socketAddressMap).onSuccess(socketAddress -> {
                String handleId = httpServerRequest.getHeader(PROXY_HEADERS_X_HANDLE_ID);
                httpServerRequest.headers().add(PROXY_HEADERS_X_ORIGIN_SERVER_HOST, "%s:%s".formatted(socketAddress.host(), socketAddress.port()));
                log.info("Will proxy request [{} {}] from [{}] to [{}:{}] (instance={})(handleId={})", httpServerRequest.method(),
                        httpServerRequest.uri(), httpServerRequest.host(),
                        socketAddress.host(), socketAddress.port(), instanceId, handleId);
            });
        });

        httpProxy.addInterceptor(new ProxyInterceptor() {
            @Override
            public Future<ProxyResponse> handleProxyRequest(ProxyContext context) {
                long startTime = System.currentTimeMillis();
                context.set("startTime", startTime);
                String handleId = UUID.randomUUID().toString();
                context.set(PROXY_HEADERS_X_HANDLE_ID, handleId);
                ProxyRequest proxyRequest = context.request();
                HttpServerRequest proxiedRequest = proxyRequest.proxiedRequest();
                String proxyServerHost = "%s:%s".formatted(proxiedRequest.localAddress().host(), proxiedRequest.localAddress().port());
                context.set(PROXY_HEADERS_X_PROXY_SERVER_HOST, proxyServerHost);
                proxiedRequest.headers().add(PROXY_HEADERS_X_HANDLE_ID, handleId);
                proxyRequest.putHeader(PROXY_HEADERS_X_HANDLE_ID, handleId);
                proxyRequest.putHeader(PROXY_HEADERS_X_PROXY_SERVER_HOST, proxyServerHost);
                log.info("Start to proxy request [{} {}] from [{}] (instance={})(handleId={})",
                        proxyRequest.getMethod(), proxyRequest.getURI(), proxyRequest.proxiedRequest().host(), instanceId, handleId);
                return context.sendRequest().onSuccess(proxyResponse -> {
                    String originServer = proxyResponse.request().proxiedRequest().getHeader(PROXY_HEADERS_X_ORIGIN_SERVER_HOST);
                    context.set(PROXY_HEADERS_X_ORIGIN_SERVER_HOST, originServer);
                    log.info("Succeeded to receive response [{} {}] from origin server [{}] for proxy request [{} {}] (instance={})(handleId={})",
                            proxyResponse.getStatusCode(), proxyResponse.getStatusMessage(), originServer, proxyRequest.getMethod(), proxyRequest.getURI(), instanceId, handleId);
                }).onFailure(throwable -> {
                    log.error("Failed to receive response from origin server for proxy request [{} {}] (instance={})(handleId={})",
                            proxyRequest.getMethod(), proxyRequest.getURI(), instanceId, handleId, throwable);
                });
            }

            @Override
            public Future<Void> handleProxyResponse(ProxyContext context) {
                String handleId = context.get(PROXY_HEADERS_X_HANDLE_ID, String.class);
                String proxyServerHost = context.get(PROXY_HEADERS_X_PROXY_SERVER_HOST, String.class);
                String originServerHost = context.get(PROXY_HEADERS_X_ORIGIN_SERVER_HOST, String.class);
                Long startTime = context.get("startTime", Long.class);
                context.response().putHeader(PROXY_HEADERS_X_HANDLE_ID, handleId);
                context.response().putHeader(PROXY_HEADERS_X_PROXY_SERVER_HOST, proxyServerHost);
                context.response().putHeader(PROXY_HEADERS_X_ORIGIN_SERVER_HOST, originServerHost);
                return context.sendResponse().onSuccess(unused -> {
                    log.info("Succeeded to complete proxy request [{} {}] to origin server [{}] (instance={})(handleId={})(time={}ms)",
                            context.request().getMethod(), context.request().getURI(), originServerHost, instanceId, handleId, System.currentTimeMillis() - startTime);
                }).onFailure(throwable -> {
                    log.error("Failed to complete proxy request [{} {}] to origin server [{}] (instance={})(handleId={})(time={}ms)",
                            context.request().getMethod(), context.request().getURI(), originServerHost, instanceId, handleId, System.currentTimeMillis() - startTime, throwable);
                });
            }
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
