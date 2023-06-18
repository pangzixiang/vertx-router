package io.github.pangzixiang.whatsit.vertx.router;

import io.github.pangzixiang.whatsit.vertx.router.options.VertxRouterVerticleOptions;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.WebSocketConnectOptions;
import io.vertx.ext.web.Router;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RunLocal {
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();

        vertx.deployVerticle(new VertxRouterVerticle(VertxRouterVerticleOptions.builder()
                .proxyServerPort(8080)
                .listenerServerPort(9090)
                .enableBasicAuthentication(true)
                .basicAuthenticationUsername("vertx-router")
                .basicAuthenticationPassword("vertx-router-pwd").build())).onSuccess(unused -> {
            Router router1 = Router.router(vertx);
            router1.route().handler(routingContext -> {
                log.info("target service 1 received request from {}", routingContext.normalizedPath());
                routingContext.next();
            });
            router1.route("/test-service/test").handler(routingContext -> {
                routingContext.response().end("done");
            });

            Future<HttpServer> httpServerFuture1 = vertx.createHttpServer()
                    .requestHandler(router1)
                    .listen(0);

            Router router2 = Router.router(vertx);
            router2.route().handler(routingContext -> {
                log.info("target service 2 received request from {}", routingContext.normalizedPath());
                routingContext.next();
            });
            router2.route("/test-service/test").handler(routingContext -> {
                routingContext.response().end("done");
            });
            Future<HttpServer> httpServerFuture2 = vertx.createHttpServer()
                    .requestHandler(router2)
                    .listen(0);

            Future.all(httpServerFuture1, httpServerFuture2)
                    .onSuccess(unused1 -> {
                        log.info("Target service started at {}", httpServerFuture1.result().actualPort());
                        HttpClient httpClient1 = vertx.createHttpClient();
                        WebSocketConnectOptions webSocketConnectOptions1 = new WebSocketConnectOptions();
                        webSocketConnectOptions1.setHost("localhost");
                        webSocketConnectOptions1.setPort(9090);
                        webSocketConnectOptions1.setURI("/register");
                        MultiMap headers1 = MultiMap.caseInsensitiveMultiMap();
                        headers1.add("host", "localhost");
                        headers1.add("port", String.valueOf(httpServerFuture1.result().actualPort()));
                        headers1.add("name", "test-service");
                        headers1.add("Authorization", "Basic dmVydHgtcm91dGVyOnZlcnR4LXJvdXRlci1wd2Q=");
                        webSocketConnectOptions1.setHeaders(headers1);
                        httpClient1.webSocket(webSocketConnectOptions1).onSuccess(unused2 -> {
                            log.info("Target Service1 connected to Proxy service");
                            vertx.setTimer(5000, l -> httpClient1.close());
                        }).onFailure(throwable -> {
                            log.error("Target Service1 failed to connect to Proxy Service", throwable);
                        });


                        HttpClient httpClient2= vertx.createHttpClient();
                        WebSocketConnectOptions webSocketConnectOptions2 = new WebSocketConnectOptions();
                        webSocketConnectOptions2.setHost("localhost");
                        webSocketConnectOptions2.setPort(9090);
                        webSocketConnectOptions2.setURI("/register");
                        MultiMap headers2 = MultiMap.caseInsensitiveMultiMap();
                        headers2.add("host", "localhost");
                        headers2.add("port", String.valueOf(httpServerFuture2.result().actualPort()));
                        headers2.add("name", "test-service");
                        headers2.add("Authorization", "Basic dmVydHgtcm91dGVyOnZlcnR4LXJvdXRlci1wd2Q=");
                        webSocketConnectOptions2.setHeaders(headers2);
                        httpClient2.webSocket(webSocketConnectOptions2).onSuccess(unused2 -> {
                            log.info("Target Service2 connected to Proxy service");
                            vertx.setTimer(10000, l -> httpClient2.close());
                        }).onFailure(throwable -> {
                            log.error("Target Service2 failed to connect to Proxy Service", throwable);
                        });
                    })
                    .onFailure(throwable -> log.error(throwable.getMessage(), throwable));

        });
    }
}
