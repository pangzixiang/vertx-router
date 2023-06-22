package io.github.pangzixiang.whatsit.vertx.router.algorithm;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.net.SocketAddress;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.UUID;

@Slf4j
public class RoundRobin implements LoadBalanceAlgorithm {
    private static final String ROUND_ROBIN_COUNTER_NAME = RoundRobin.class.getSimpleName() + UUID.randomUUID();
    @Override
    public Future<SocketAddress> handle(Vertx vertx, HttpServerRequest httpServerRequest, Map<String, SocketAddress> socketAddressMap) {
        return vertx.sharedData().getLocalCounter(ROUND_ROBIN_COUNTER_NAME).compose(counter -> {
            return counter.getAndIncrement().compose(now -> {
                int index = (int) ((now + 1) % socketAddressMap.size());
                return Future.succeededFuture(socketAddressMap.values().stream().toList().get(index));
            }, throwable -> {
                log.error("Failed to get counter for Round Robin, hence default return the first origin", throwable);
                return Future.succeededFuture(socketAddressMap.values().stream().toList().get(0));
            });
        }, throwable -> {
            log.error("Failed to get counter for Round Robin, hence default return the first origin", throwable);
            return Future.succeededFuture(socketAddressMap.values().stream().toList().get(0));
        });
    }
}
