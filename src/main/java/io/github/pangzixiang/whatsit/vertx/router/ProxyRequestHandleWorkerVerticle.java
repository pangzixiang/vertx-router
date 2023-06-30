package io.github.pangzixiang.whatsit.vertx.router;

import io.github.pangzixiang.whatsit.vertx.router.model.ProxyEvent;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.httpproxy.*;
import lombok.extern.slf4j.Slf4j;
import java.util.UUID;

import static io.github.pangzixiang.whatsit.vertx.router.ProxyCustomHeaders.*;

@Slf4j
public class ProxyRequestHandleWorkerVerticle extends AbstractVerticle {

    public static final String EVENT_BUS_ID = ProxyRequestHandleWorkerVerticle.class.getSimpleName() + UUID.randomUUID();
    private final String instanceId = UUID.randomUUID().toString();
    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        getVertx().eventBus().consumer(EVENT_BUS_ID).handler(message -> {
            ProxyEvent proxyEvent = (ProxyEvent) message.body();
            ProxyContext context = proxyEvent.getProxyContext();
            long startTime = System.currentTimeMillis();
            context.set("startTime", startTime);
            String handleId = UUID.randomUUID().toString();
            context.set(PROXY_HEADERS_X_HANDLE_ID, handleId);
            ProxyRequest proxyRequest = context.request();
            HttpServerRequest proxiedRequest = proxyRequest.proxiedRequest();
            String proxyServerHost = "%s:%s".formatted(proxiedRequest.localAddress().host(), proxiedRequest.localAddress().port());
            context.set(PROXY_HEADERS_X_PROXY_SERVER_HOST, proxyServerHost);
            proxiedRequest.headers().add(PROXY_HEADERS_X_HANDLE_ID, handleId);
            proxyRequest.putHeader(PROXY_HEADERS_X_HANDLE_ID, handleId);
            proxyRequest.putHeader(PROXY_HEADERS_X_PROXY_SERVER_HOST, proxyServerHost);
            log.info("Start to proxy request [{} {}] from [{}] (instance={})(handleId={})",
                    proxyRequest.getMethod(), proxyRequest.getURI(), proxyRequest.proxiedRequest().host(), instanceId, handleId);
            context.sendRequest().onSuccess(proxyResponse -> {
                String originServer = proxyResponse.request().proxiedRequest().getHeader(PROXY_HEADERS_X_ORIGIN_SERVER_HOST);
                context.set(PROXY_HEADERS_X_ORIGIN_SERVER_HOST, originServer);
                log.info("Succeeded to receive response [{} {}] from origin server [{}] for proxy request [{} {}] (instance={})(handleId={})",
                        proxyResponse.getStatusCode(), proxyResponse.getStatusMessage(), originServer, proxyRequest.getMethod(), proxyRequest.getURI(), instanceId, handleId);
                proxyEvent.setProxyResponse(proxyResponse);
                message.reply(proxyEvent);
            }).onFailure(throwable -> {
                log.error("Failed to receive response from origin server for proxy request [{} {}] (instance={})(handleId={})",
                        proxyRequest.getMethod(), proxyRequest.getURI(), instanceId, handleId, throwable);
            });
        }).completionHandler(res -> {
            if (res.succeeded()) {
                log.info("Proxy worker verticle started listener (instance={})", instanceId);
                startPromise.complete();
            } else {
                log.error("Failed to start listener for proxy worker verticle (instance={})", instanceId);
                startPromise.fail(res.cause());
            }
        });
    }
}
