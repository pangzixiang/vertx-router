package io.github.pangzixiang.whatsit.vertx.router.model;

import io.vertx.httpproxy.ProxyContext;
import io.vertx.httpproxy.ProxyResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class ProxyEvent implements Serializable {
    private ProxyContext proxyContext;
    private ProxyResponse proxyResponse;
}
