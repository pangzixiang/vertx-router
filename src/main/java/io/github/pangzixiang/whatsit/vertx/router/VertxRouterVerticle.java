package io.github.pangzixiang.whatsit.vertx.router;

import io.github.pangzixiang.whatsit.vertx.router.options.VertxRouterVerticleOptions;
import io.vertx.core.*;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Slf4j
public class VertxRouterVerticle extends AbstractVerticle {

    private final VertxRouterVerticleOptions vertxRouterVerticleOptions;

    public static final String CONNECTION_MAP = "vertx-router-connection-map-" + UUID.randomUUID();

    public static final String VERTX_ROUTER_SHARE_MAP_NAME = "vertx-router-share-map-" + UUID.randomUUID();

    public static final String VERTX_ROUTER_OPTIONS_KEY_NAME = "vertx-router-verticle-options";

    public VertxRouterVerticle() {
        this(new VertxRouterVerticleOptions());
    }

    public VertxRouterVerticle(VertxRouterVerticleOptions vertxRouterVerticleOptions) {
        this.vertxRouterVerticleOptions = vertxRouterVerticleOptions;
    }

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        getVertx().sharedData().getLocalMap(VERTX_ROUTER_SHARE_MAP_NAME).putIfAbsent(VERTX_ROUTER_OPTIONS_KEY_NAME, vertxRouterVerticleOptions);
        Future<String> deployProxyServerFuture = getVertx().deployVerticle(ProxyServerVerticle.class, new DeploymentOptions());

        deployProxyServerFuture.compose(unused -> getVertx().deployVerticle(ListenerServerVerticle.class, new DeploymentOptions()
                        .setInstances(vertxRouterVerticleOptions.getListenerServerInstanceNumber())))
                .onSuccess(unused -> {
                    log.info("Succeeded to deploy vertx router verticle");
                    startPromise.complete();
                })
                .onFailure(throwable -> {
                    log.error("Failed to deploy vertx router verticle", throwable);
                    startPromise.fail(throwable);
                });
    }
}
