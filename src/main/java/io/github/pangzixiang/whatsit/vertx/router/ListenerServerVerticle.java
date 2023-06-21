package io.github.pangzixiang.whatsit.vertx.router;

import io.github.pangzixiang.whatsit.vertx.router.handler.ListenerWebsocketHandler;
import io.github.pangzixiang.whatsit.vertx.router.options.VertxRouterVerticleOptions;
import io.github.pangzixiang.whatsit.vertx.router.security.BasicAuthenticationProvider;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.AuthenticationHandler;
import io.vertx.ext.web.handler.BasicAuthHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

import static io.github.pangzixiang.whatsit.vertx.router.VertxRouterVerticle.VERTX_ROUTER_OPTIONS_KEY_NAME;
import static io.github.pangzixiang.whatsit.vertx.router.VertxRouterVerticle.VERTX_ROUTER_SHARE_MAP_NAME;

@Slf4j
public class ListenerServerVerticle extends AbstractVerticle {
    private final String instanceId = UUID.randomUUID().toString();
    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        VertxRouterVerticleOptions vertxRouterVerticleOptions = (VertxRouterVerticleOptions)
                getVertx().sharedData().getLocalMap(VERTX_ROUTER_SHARE_MAP_NAME).get(VERTX_ROUTER_OPTIONS_KEY_NAME);

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

        registerRoute.handler(new ListenerWebsocketHandler(getVertx(), instanceId));

        listenerRouter.errorHandler(HttpResponseStatus.UNAUTHORIZED.code(), routingContext -> {
            routingContext.response().setStatusCode(HttpResponseStatus.UNAUTHORIZED.code()).end();
        });

        listenerRouter.errorHandler(HttpResponseStatus.BAD_REQUEST.code(), routingContext -> {
            routingContext.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).end();
        });

        listenerRouter.errorHandler(HttpResponseStatus.NOT_FOUND.code(), routingContext -> {
            routingContext.response().setStatusCode(HttpResponseStatus.NOT_FOUND.code()).end();
        });

        HttpServer listenerServer = getVertx().createHttpServer(vertxRouterVerticleOptions.getListenerServerOptions());
        listenerServer.requestHandler(listenerRouter);
        Future<HttpServer> listenerServerFuture = listenerServer.listen(vertxRouterVerticleOptions.getListenerServerPort());
        listenerServerFuture.onSuccess(httpServer -> {
            log.info("Vertx Router Listener Server started at port {} (instance={})", httpServer.actualPort(), instanceId);
            startPromise.complete();
        }).onFailure(startPromise::fail);
    }
}
