package io.github.pangzixiang.whatsit.vertx.router;

import io.github.pangzixiang.whatsit.vertx.router.options.VertxRouterVerticleOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.CorsHandler;

public class BaseVerticle extends AbstractVerticle {

    public Router createRouter(VertxRouterVerticleOptions vertxRouterVerticleOptions) {
        Router router = Router.router(getVertx());

        if (vertxRouterVerticleOptions.isAllowCORS()) {
            CorsHandler corsHandler = CorsHandler.create();
            if (vertxRouterVerticleOptions.getAllowCORSHeaders() != null) {
                corsHandler.allowedHeaders(vertxRouterVerticleOptions.getAllowCORSHeaders());
            }
            if (vertxRouterVerticleOptions.getAllowCORSMethods() != null) {
                corsHandler.allowedMethods(vertxRouterVerticleOptions.getAllowCORSMethods());
            }
            router.route().handler(corsHandler);
        }

        return router;
    }
}
