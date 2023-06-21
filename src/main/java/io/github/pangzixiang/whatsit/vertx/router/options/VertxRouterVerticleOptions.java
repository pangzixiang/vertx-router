package io.github.pangzixiang.whatsit.vertx.router.options;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.shareddata.Shareable;
import io.vertx.ext.web.RoutingContext;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class VertxRouterVerticleOptions implements Shareable {
    @Builder.Default
    private String registerPath = "/register";
    @Builder.Default
    private int listenerServerPort = 0;
    @Builder.Default
    private int proxyServerPort = 0;
    @Builder.Default
    private boolean enableBasicAuthentication = false;
    private String basicAuthenticationUsername;
    private String basicAuthenticationPassword;
    @Builder.Default
    private boolean enableCustomAuthentication = false;
    private List<Handler<RoutingContext>> customAuthenticationHandlers;
    @Builder.Default
    private HttpServerOptions listenerServerOptions = new HttpServerOptions();
    @Builder.Default
    private HttpServerOptions proxyServerOptions = new HttpServerOptions();
    @Builder.Default
    private int proxyServerInstanceNumber = 2;
    @Builder.Default
    private int listenerServerInstanceNumber = 2;
    private HttpClientOptions proxyHttpClientOptions;
    private BiFunction<String, Map<String, SocketAddress>, Future<SocketAddress>> customOriginServerSelector;
}
