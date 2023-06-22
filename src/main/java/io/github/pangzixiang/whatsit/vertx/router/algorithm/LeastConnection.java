package io.github.pangzixiang.whatsit.vertx.router.algorithm;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.net.SocketAddress;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class LeastConnection implements LoadBalanceAlgorithm {
    private final Map<String, AtomicLong> connectionCountMap = new ConcurrentHashMap<>();
    @Override
    public Future<SocketAddress> handle(Vertx vertx, HttpServerRequest httpServerRequest, Map<String, SocketAddress> socketAddressMap) {
        socketAddressMap.keySet().forEach(connectionId -> connectionCountMap.putIfAbsent(connectionId, new AtomicLong(0)));
        SocketAddress socketAddress = getLeastConnectionId(socketAddressMap);
        return Future.succeededFuture(socketAddress);
    }

    private SocketAddress getLeastConnectionId(Map<String, SocketAddress> socketAddressMap) {
        if (connectionCountMap.isEmpty()) {
            return socketAddressMap.values().stream().toList().get(0);
        }
        Optional<Map.Entry<String, AtomicLong>> minConnection = connectionCountMap.entrySet().stream().min(Comparator.comparingLong(o -> o.getValue().get()));
        Map.Entry<String, AtomicLong> min = minConnection.get();
        min.getValue().getAndIncrement();
        SocketAddress socketAddress = socketAddressMap.get(min.getKey());
        if (socketAddress == null) {
            connectionCountMap.remove(min.getKey());
            return getLeastConnectionId(socketAddressMap);
        }
        return socketAddress;
    }
}
