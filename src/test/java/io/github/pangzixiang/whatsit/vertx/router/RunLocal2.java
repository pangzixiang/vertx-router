package io.github.pangzixiang.whatsit.vertx.router;

import io.github.pangzixiang.whatsit.vertx.router.options.VertxRouterVerticleOptions;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientOptions;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RunLocal2 {
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        VertxRouterVerticleOptions options = new VertxRouterVerticleOptions();
        options.setProxyServerPort(8080);
        options.setListenerServerPort(8085);
        options.setProxyServerInstanceNumber(4);
        options.setListenerServerInstanceNumber(2);
        options.setProxyHttpClientOptions(new HttpClientOptions());

        vertx.deployVerticle(new VertxRouterVerticle(options)).onSuccess(unused -> {
            log.info("Application initialization completed!");
        }).onFailure(throwable -> {
            log.error("Application initialization failed!", throwable);
        });
    }
}
