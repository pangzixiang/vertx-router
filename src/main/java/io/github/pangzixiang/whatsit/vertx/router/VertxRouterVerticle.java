package io.github.pangzixiang.whatsit.vertx.router;

import io.github.pangzixiang.whatsit.vertx.router.exception.TargetServerNotFoundException;
import io.github.pangzixiang.whatsit.vertx.router.handler.ListenerWebsocketHandler;
import io.github.pangzixiang.whatsit.vertx.router.handler.ProxyServerHandler;
import io.github.pangzixiang.whatsit.vertx.router.options.VertxRouterVerticleOptions;
import io.github.pangzixiang.whatsit.vertx.router.security.BasicAuthenticationProvider;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.*;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.AuthenticationHandler;
import io.vertx.ext.web.handler.BasicAuthHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Slf4j
public class VertxRouterVerticle extends AbstractVerticle {

    private final VertxRouterVerticleOptions vertxRouterVerticleOptions;

    public static final String CONNECTION_MAP = "vertx-router-connection-map-" + UUID.randomUUID();

    public VertxRouterVerticle() {
        this.vertxRouterVerticleOptions = new VertxRouterVerticleOptions();
    }

    public VertxRouterVerticle(VertxRouterVerticleOptions vertxRouterVerticleOptions) {
        this.vertxRouterVerticleOptions = vertxRouterVerticleOptions;
    }

    @Override
    public void start(Promise<Void> startPromise) throws Exception {

        Router listenerRouter = Router.router(getVertx());
        Route registerRoute = listenerRouter.route(vertxRouterVerticleOptions.getRegisterPath());

        if (!vertxRouterVerticleOptions.isEnableCustomAuthentication() && vertxRouterVerticleOptions.isEnableBasicAuthentication()) {
            listenerRouter.route().handler(SessionHandler.create(LocalSessionStore.create(getVertx())));
            AuthenticationHandler authenticationHandler = BasicAuthHandler.create(new BasicAuthenticationProvider(vertxRouterVerticleOptions));
            registerRoute.handler(authenticationHandler);
        }

        if (vertxRouterVerticleOptions.isEnableCustomAuthentication() && vertxRouterVerticleOptions.getCustomAuthenticationHandlers() != null
                && !vertxRouterVerticleOptions.getCustomAuthenticationHandlers().isEmpty()) {
            vertxRouterVerticleOptions.getCustomAuthenticationHandlers().forEach(registerRoute::handler);
        }

        registerRoute.handler(new ListenerWebsocketHandler(getVertx()));

        listenerRouter.errorHandler(HttpResponseStatus.UNAUTHORIZED.code(), routingContext -> {
            routingContext.response().setStatusCode(HttpResponseStatus.UNAUTHORIZED.code()).end();
        });

        listenerRouter.errorHandler(HttpResponseStatus.BAD_REQUEST.code(), routingContext -> {
            routingContext.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).end();
        });

        listenerRouter.errorHandler(HttpResponseStatus.NOT_FOUND.code(), routingContext -> {
            routingContext.response().setStatusCode(HttpResponseStatus.NOT_FOUND.code()).end();
        });

        HttpServer listenerServer = getVertx().createHttpServer();
        listenerServer.requestHandler(listenerRouter);
        Future<HttpServer> listenerServerFuture = listenerServer.listen(vertxRouterVerticleOptions.getListenerServerPort());

        HttpServer proxyServer = getVertx().createHttpServer();

        Router proxyRouter = Router.router(getVertx());
        proxyRouter.route().handler(routingContext -> {
            log.debug("Proxy Server received request from {}", routingContext.normalizedPath());
            routingContext.next();
        });

        proxyRouter.route("/:serviceName/*").handler(new ProxyServerHandler(getVertx())).failureHandler(routingContext -> {
            if (routingContext.failure() instanceof TargetServerNotFoundException e) {
                routingContext.response().setStatusCode(HttpResponseStatus.NOT_FOUND.code()).end("No Proxy for target service %s".formatted(e.getTargetServerName()));
            } else {
                routingContext.response().setStatusCode(routingContext.statusCode()).end();
            }
        });
        proxyServer.requestHandler(proxyRouter);
        Future<HttpServer> proxyServerFuture = proxyServer.listen(vertxRouterVerticleOptions.getProxyServerPort());

        proxyServerFuture.compose(proxyServerResult -> {
            log.info("Vertx Router Proxy Server started at port {}", proxyServerResult.actualPort());
            return listenerServerFuture;
        }).compose(listenerServerResult -> {
            log.info("Vertx Router Listener Server started at port {}", listenerServerResult.actualPort());
            return Future.succeededFuture();
        }).onSuccess(unused -> startPromise.complete()).onFailure(startPromise::fail);
    }
}
