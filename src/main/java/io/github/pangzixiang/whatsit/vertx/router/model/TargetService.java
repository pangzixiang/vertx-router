package io.github.pangzixiang.whatsit.vertx.router.model;

import io.vertx.core.shareddata.Shareable;
import io.vertx.ext.web.proxy.handler.ProxyHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class TargetService implements Shareable {
    private Map<String, String> hosts = new ConcurrentHashMap<>();
    private Map<String, Integer> ports = new ConcurrentHashMap<>();
    private Map<String, ProxyHandler> proxyHandlers = new ConcurrentHashMap<>();

    public TargetService add(String connectionId, String host, Integer port, ProxyHandler proxyHandler) {
        this.hosts.put(connectionId, host);
        this.ports.put(connectionId, port);
        this.proxyHandlers.put(connectionId, proxyHandler);
        return this;
    }
}
