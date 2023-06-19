package io.github.pangzixiang.whatsit.vertx.router;

import io.github.pangzixiang.whatsit.vertx.router.exception.TargetServerNotFoundException;
import io.github.pangzixiang.whatsit.vertx.router.handler.ProxyServerHandler;
import io.github.pangzixiang.whatsit.vertx.router.options.VertxRouterVerticleOptions;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Slf4j
public class ProxyServerVerticle extends AbstractVerticle {
    private final String instanceId = UUID.randomUUID().toString();

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        VertxRouterVerticleOptions vertxRouterVerticleOptions = config().mapTo(VertxRouterVerticleOptions.class);

        HttpServer proxyServer = getVertx().createHttpServer(vertxRouterVerticleOptions.getProxyServerOptions());

        Router proxyRouter = Router.router(getVertx());
        proxyRouter.route().handler(routingContext -> {
            log.debug("Proxy Server received request from {} [instance={}]", routingContext.normalizedPath(), instanceId);
            routingContext.next();
        });

        proxyRouter.route("/:serviceName/*").handler(new ProxyServerHandler(getVertx(), instanceId)).failureHandler(routingContext -> {
            if (routingContext.failure() instanceof TargetServerNotFoundException e) {
                routingContext.response().setStatusCode(HttpResponseStatus.NOT_FOUND.code()).end("No Proxy for target service %s".formatted(e.getTargetServerName()));
            } else {
                routingContext.response().setStatusCode(routingContext.statusCode()).end();
            }
        });
        proxyServer.requestHandler(proxyRouter);
        Future<HttpServer> proxyServerFuture = proxyServer.listen(vertxRouterVerticleOptions.getProxyServerPort());
        proxyServerFuture.onSuccess(httpServer -> {
            log.info("Vertx Router Proxy Server started at port {} (instance={})", httpServer.actualPort(), instanceId);
            startPromise.complete();
        }).onFailure(startPromise::fail);
    }
}
