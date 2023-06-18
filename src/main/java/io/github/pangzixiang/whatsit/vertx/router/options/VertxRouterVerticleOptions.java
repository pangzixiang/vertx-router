package io.github.pangzixiang.whatsit.vertx.router.options;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class VertxRouterVerticleOptions {
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
}
