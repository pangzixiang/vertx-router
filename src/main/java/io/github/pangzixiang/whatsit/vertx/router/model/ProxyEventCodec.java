package io.github.pangzixiang.whatsit.vertx.router.model;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;
import io.vertx.core.json.Json;

public class ProxyEventCodec implements MessageCodec<ProxyEvent, ProxyEvent> {
    @Override
    public void encodeToWire(Buffer buffer, ProxyEvent proxyEvent) {
        buffer.appendBuffer(Json.encodeToBuffer(proxyEvent));
    }

    @Override
    public ProxyEvent decodeFromWire(int pos, Buffer buffer) {
        return (ProxyEvent) buffer.toJson();
    }

    @Override
    public ProxyEvent transform(ProxyEvent proxyEvent) {
        return proxyEvent;
    }

    @Override
    public String name() {
        return ProxyEventCodec.class.getSimpleName();
    }

    @Override
    public byte systemCodecID() {
        return -1;
    }
}
