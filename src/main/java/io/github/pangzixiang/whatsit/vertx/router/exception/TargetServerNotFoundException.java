package io.github.pangzixiang.whatsit.vertx.router.exception;

import lombok.Getter;

@Getter
public class TargetServerNotFoundException extends RuntimeException {
    private final String targetServerName;
    public TargetServerNotFoundException(String targetServerName) {
        super("Target service [%s] not found".formatted(targetServerName));
        this.targetServerName = targetServerName;
    }
}
