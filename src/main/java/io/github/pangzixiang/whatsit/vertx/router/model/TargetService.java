package io.github.pangzixiang.whatsit.vertx.router.model;

import io.vertx.core.net.SocketAddress;
import io.vertx.core.shareddata.Shareable;
import lombok.Getter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class TargetService implements Shareable {
    private final Map<String, SocketAddress> socketAddressMap = new ConcurrentHashMap<>();

    public TargetService add(String connectionId, String host, Integer port) {
        this.socketAddressMap.put(connectionId, SocketAddress.inetSocketAddress(port, host));
        return this;
    }

    public TargetService remove(String connectionId) {
        this.socketAddressMap.remove(connectionId);
        return this;
    }
}
