package io.github.pangzixiang.whatsit.vertx.router.model;

import io.vertx.core.shareddata.Shareable;
import io.vertx.ext.web.proxy.handler.ProxyHandler;
import lombok.Getter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class TargetService implements Shareable {
    private final Map<String, String> hosts = new ConcurrentHashMap<>();
    private final Map<String, Integer> ports = new ConcurrentHashMap<>();
    private final Map<String, ProxyHandler> proxyHandlers = new ConcurrentHashMap<>();

    public TargetService add(String connectionId, String host, Integer port, ProxyHandler proxyHandler) {
        this.hosts.put(connectionId, host);
        this.ports.put(connectionId, port);
        this.proxyHandlers.put(connectionId, proxyHandler);
        return this;
    }

    public TargetService remove(String connectionId) {
        this.hosts.remove(connectionId);
        this.ports.remove(connectionId);
        this.proxyHandlers.remove(connectionId);
        return this;
    }
}
