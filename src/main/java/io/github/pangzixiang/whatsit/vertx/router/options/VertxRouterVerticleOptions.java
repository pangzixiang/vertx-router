package io.github.pangzixiang.whatsit.vertx.router.options;

import io.github.pangzixiang.whatsit.vertx.router.algorithm.LoadBalanceAlgorithm;
import io.github.pangzixiang.whatsit.vertx.router.algorithm.RoundRobin;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.shareddata.Shareable;
import io.vertx.ext.web.RoutingContext;
import lombok.Getter;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Getter
public class VertxRouterVerticleOptions implements Shareable {
    private String registerPath;
    private int listenerServerPort;
    private int proxyServerPort;
    private boolean enableBasicAuthentication;
    private String basicAuthenticationUsername;
    private String basicAuthenticationPassword;
    private boolean enableCustomAuthentication;
    private List<Handler<RoutingContext>> customAuthenticationHandlers;
    private HttpServerOptions listenerServerOptions;
    private HttpServerOptions proxyServerOptions;
    private int proxyServerInstanceNumber;
    private int listenerServerInstanceNumber;
    private HttpClientOptions proxyHttpClientOptions;
    private LoadBalanceAlgorithm loadBalanceAlgorithm;
    private boolean allowCORS;
    private Set<String> allowCORSHeaders;
    private Set<HttpMethod> allowCORSMethods;

    public VertxRouterVerticleOptions() {
        registerPath = "/register";
        listenerServerPort = 0;
        proxyServerPort = 0;
        enableBasicAuthentication = false;
        enableCustomAuthentication = false;
        listenerServerOptions = new HttpServerOptions();
        proxyServerOptions = new HttpServerOptions();
        proxyServerInstanceNumber = 2;
        listenerServerInstanceNumber = 2;
        loadBalanceAlgorithm = new RoundRobin();
        allowCORS = true;
        proxyHttpClientOptions = new HttpClientOptions()
                .setReadIdleTimeout(10)
                .setWriteIdleTimeout(10)
                .setConnectTimeout((int) TimeUnit.SECONDS.toMillis(10));
    }

    public VertxRouterVerticleOptions setRegisterPath(String registerPath) {
        this.registerPath = registerPath;
        return this;
    }

    public VertxRouterVerticleOptions setListenerServerPort(int listenerServerPort) {
        this.listenerServerPort = listenerServerPort;
        return this;
    }

    public VertxRouterVerticleOptions setProxyServerPort(int proxyServerPort) {
        this.proxyServerPort = proxyServerPort;
        return this;
    }

    public VertxRouterVerticleOptions setEnableBasicAuthentication(boolean enableBasicAuthentication) {
        this.enableBasicAuthentication = enableBasicAuthentication;
        return this;
    }

    public VertxRouterVerticleOptions setBasicAuthenticationUsername(String basicAuthenticationUsername) {
        this.basicAuthenticationUsername = basicAuthenticationUsername;
        return this;
    }

    public VertxRouterVerticleOptions setBasicAuthenticationPassword(String basicAuthenticationPassword) {
        this.basicAuthenticationPassword = basicAuthenticationPassword;
        return this;
    }

    public VertxRouterVerticleOptions setEnableCustomAuthentication(boolean enableCustomAuthentication) {
        this.enableCustomAuthentication = enableCustomAuthentication;
        return this;
    }

    public VertxRouterVerticleOptions setCustomAuthenticationHandlers(List<Handler<RoutingContext>> customAuthenticationHandlers) {
        this.customAuthenticationHandlers = customAuthenticationHandlers;
        return this;
    }

    public VertxRouterVerticleOptions setListenerServerOptions(HttpServerOptions listenerServerOptions) {
        this.listenerServerOptions = listenerServerOptions;
        return this;
    }

    public VertxRouterVerticleOptions setProxyServerOptions(HttpServerOptions proxyServerOptions) {
        this.proxyServerOptions = proxyServerOptions;
        return this;
    }

    public VertxRouterVerticleOptions setProxyServerInstanceNumber(int proxyServerInstanceNumber) {
        this.proxyServerInstanceNumber = proxyServerInstanceNumber;
        return this;
    }

    public VertxRouterVerticleOptions setListenerServerInstanceNumber(int listenerServerInstanceNumber) {
        this.listenerServerInstanceNumber = listenerServerInstanceNumber;
        return this;
    }

    public VertxRouterVerticleOptions setProxyHttpClientOptions(HttpClientOptions proxyHttpClientOptions) {
        this.proxyHttpClientOptions = proxyHttpClientOptions;
        return this;
    }

    public VertxRouterVerticleOptions setLoadBalanceAlgorithm(LoadBalanceAlgorithm loadBalanceAlgorithm) {
        this.loadBalanceAlgorithm = loadBalanceAlgorithm;
        return this;
    }

    public VertxRouterVerticleOptions setAllowCORS(boolean allowCORS) {
        this.allowCORS = allowCORS;
        return this;
    }

    public VertxRouterVerticleOptions setAllowCORSHeaders(Set<String> allowCORSHeaders) {
        this.allowCORSHeaders = allowCORSHeaders;
        return this;
    }

    public VertxRouterVerticleOptions setAllowCORSMethods(Set<HttpMethod> allowCORSMethods) {
        this.allowCORSMethods = allowCORSMethods;
        return this;
    }
}
