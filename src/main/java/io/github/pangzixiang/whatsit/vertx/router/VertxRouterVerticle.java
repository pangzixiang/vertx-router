package io.github.pangzixiang.whatsit.vertx.router;

import io.github.pangzixiang.whatsit.vertx.router.options.VertxRouterVerticleOptions;
import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Slf4j
public class VertxRouterVerticle extends AbstractVerticle {

    private final VertxRouterVerticleOptions vertxRouterVerticleOptions;

    public static final String CONNECTION_MAP = "vertx-router-connection-map-" + UUID.randomUUID();

    public VertxRouterVerticle() {
        this(new VertxRouterVerticleOptions());
    }

    public VertxRouterVerticle(VertxRouterVerticleOptions vertxRouterVerticleOptions) {
        this.vertxRouterVerticleOptions = vertxRouterVerticleOptions;
    }

    @Override
    public void start(Promise<Void> startPromise) throws Exception {

        Future<String> deployProxyServerFuture = getVertx().deployVerticle(ProxyServerVerticle.class, new DeploymentOptions()
                .setConfig(JsonObject.mapFrom(vertxRouterVerticleOptions)).setInstances(vertxRouterVerticleOptions.getProxyServerInstanceNumber()));

        Future<String> deployListenerServerFuture = getVertx().deployVerticle(ListenerServerVerticle.class, new DeploymentOptions()
                .setConfig(JsonObject.mapFrom(vertxRouterVerticleOptions)).setInstances(vertxRouterVerticleOptions.getListenerServerInstanceNumber()));


        deployProxyServerFuture.compose(unused -> deployListenerServerFuture)
                .compose(unused -> Future.succeededFuture())
                .onSuccess(unused -> startPromise.complete())
                .onFailure(startPromise::fail);
    }
}
