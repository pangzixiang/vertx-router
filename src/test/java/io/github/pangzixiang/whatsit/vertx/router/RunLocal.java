package io.github.pangzixiang.whatsit.vertx.router;

import io.github.pangzixiang.whatsit.vertx.router.algorithm.LeastConnection;
import io.github.pangzixiang.whatsit.vertx.router.options.VertxRouterVerticleOptions;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.*;
import io.vertx.core.net.SelfSignedCertificate;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RunLocal {
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        SelfSignedCertificate selfSignedCertificate = SelfSignedCertificate.create();
        HttpServerOptions sslOptions = new HttpServerOptions().setSsl(true).setKeyCertOptions(selfSignedCertificate.keyCertOptions()).setTrustOptions(selfSignedCertificate.trustOptions());
        VertxRouterVerticleOptions vertxRouterVerticleOptions = new VertxRouterVerticleOptions()
                .setProxyServerPort(8080)
                .setListenerServerPort(9090)
                .setProxyServerInstanceNumber(4)
                .setListenerServerOptions(sslOptions)
                .setProxyServerOptions(sslOptions)
                .setListenerServerInstanceNumber(4)
                .setProxyHttpClientOptions(new HttpClientOptions().setSsl(true).setTrustAll(true))
                .setEnableBasicAuthentication(true)
                .setLoadBalanceAlgorithm(new LeastConnection())
                .setBasicAuthenticationUsername("vertx-router")
                .setBasicAuthenticationPassword("vertx-router-pwd");
        vertx.deployVerticle(new VertxRouterVerticle(vertxRouterVerticleOptions)).onSuccess(unused -> {
            Router router1 = Router.router(vertx);
            router1.route().handler(BodyHandler.create());
            router1.route().handler(routingContext -> {
                log.info("target service 1 received request from {}, headers={}", routingContext.normalizedPath(), routingContext.request().headers());
                routingContext.next();
            });
            router1.route("/test-service/test").handler(routingContext -> {
                routingContext.response().end("done");
            });
            router1.route(HttpMethod.POST, "/test-service/test1").handler(routingContext -> {
                routingContext.response().end(routingContext.body().asString());
            });

            Future<HttpServer> httpServerFuture1 = vertx.createHttpServer(sslOptions)
                    .requestHandler(router1)
                    .listen(0);

            Router router2 = Router.router(vertx);
            router2.route().handler(BodyHandler.create());
            router2.route().handler(routingContext -> {
                log.info("target service 2 received request from {}, headers={}", routingContext.normalizedPath(), routingContext.request().headers());
                routingContext.next();
            });
            router2.route(HttpMethod.POST, "/test-service/test1").handler(routingContext -> {
                routingContext.response().end(routingContext.body().asString());
            });
            router2.route("/test-service/test").handler(routingContext -> {
                routingContext.response().end("done");
            });
            Future<HttpServer> httpServerFuture2 = vertx.createHttpServer(sslOptions)
                    .requestHandler(router2)
                    .listen(0);

            Future.all(httpServerFuture1, httpServerFuture2)
                    .onSuccess(unused1 -> {
                        log.info("Target service started at {}", httpServerFuture1.result().actualPort());
                        HttpClientOptions options = new HttpClientOptions();
                        options.setSsl(true);
                        options.setTrustAll(true);
                        HttpClient httpClient1 = vertx.createHttpClient(options);
                        WebSocketConnectOptions webSocketConnectOptions1 = new WebSocketConnectOptions();
                        webSocketConnectOptions1.setHost("localhost");
                        webSocketConnectOptions1.setPort(9090);
                        webSocketConnectOptions1.setSsl(true);
                        webSocketConnectOptions1.setURI("/register");
                        MultiMap headers1 = MultiMap.caseInsensitiveMultiMap();
                        headers1.add("host", "localhost");
                        headers1.add("port", String.valueOf(httpServerFuture1.result().actualPort()));
                        headers1.add("name", "test-service");
                        headers1.add("Authorization", "Basic dmVydHgtcm91dGVyOnZlcnR4LXJvdXRlci1wd2Q=");
                        webSocketConnectOptions1.setHeaders(headers1);
                        httpClient1.webSocket(webSocketConnectOptions1).onSuccess(unused2 -> {
                            log.info("Target Service1 connected to Proxy service");
//                            vertx.setTimer(10000, l -> httpClient1.close());
                        }).onFailure(throwable -> {
                            log.error("Target Service1 failed to connect to Proxy Service", throwable);
                        });


                        HttpClient httpClient2 = vertx.createHttpClient(options);
                        WebSocketConnectOptions webSocketConnectOptions2 = new WebSocketConnectOptions();
                        webSocketConnectOptions2.setHost("localhost");
                        webSocketConnectOptions2.setPort(9090);
                        webSocketConnectOptions2.setSsl(true);
                        webSocketConnectOptions2.setURI("/register");
                        MultiMap headers2 = MultiMap.caseInsensitiveMultiMap();
                        headers2.add("host", "localhost");
                        headers2.add("port", String.valueOf(httpServerFuture2.result().actualPort()));
                        headers2.add("name", "test-service");
                        headers2.add("Authorization", "Basic dmVydHgtcm91dGVyOnZlcnR4LXJvdXRlci1wd2Q=");
                        webSocketConnectOptions2.setHeaders(headers2);
                        httpClient2.webSocket(webSocketConnectOptions2).onSuccess(unused2 -> {
                            log.info("Target Service2 connected to Proxy service");
//                            vertx.setTimer(15000, l -> httpClient2.close());
                        }).onFailure(throwable -> {
                            log.error("Target Service2 failed to connect to Proxy Service", throwable);
                        });
                    })
                    .onFailure(throwable -> log.error(throwable.getMessage(), throwable));

        });
    }
}
