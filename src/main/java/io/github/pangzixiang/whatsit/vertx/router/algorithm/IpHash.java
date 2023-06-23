package io.github.pangzixiang.whatsit.vertx.router.algorithm;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.net.SocketAddress;

import java.util.Map;

public class IpHash implements LoadBalanceAlgorithm {
    @Override
    public Future<SocketAddress> handle(Vertx vertx, HttpServerRequest httpServerRequest, Map<String, SocketAddress> socketAddressMap) {
        int index = httpServerRequest.remoteAddress().hostAddress().hashCode() % socketAddressMap.size();
        if (index > socketAddressMap.size() || index < 0) {
            index = 0;
        }
        return Future.succeededFuture(socketAddressMap.values().stream().toList().get(index));
    }
}
