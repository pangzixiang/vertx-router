package io.github.pangzixiang.whatsit.vertx.router.security;

import io.github.pangzixiang.whatsit.vertx.router.exception.AuthenticationException;
import io.github.pangzixiang.whatsit.vertx.router.options.VertxRouterVerticleOptions;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authentication.AuthenticationProvider;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@AllArgsConstructor
public class BasicAuthenticationProvider implements AuthenticationProvider {

    private final VertxRouterVerticleOptions vertxRouterVerticleOptions;

    @Override
    public void authenticate(JsonObject credentials, Handler<AsyncResult<User>> resultHandler) {
        String username = credentials.getString("username");
        String password = credentials.getString("password");
        resultHandler.handle(new AsyncResult<>() {
            @Override
            public User result() {
                if (succeeded()) {
                    return User.fromName(username);
                }
                return null;
            }

            @Override
            public Throwable cause() {
                if (failed()) {
                    return new AuthenticationException("Authentication Failed!");
                }
                return null;
            }

            @Override
            public boolean succeeded() {
                return validate(username, password);
            }

            @Override
            public boolean failed() {
                return !validate(username, password);
            }
        });
    }

    private boolean validate(String inputUsername, String inputPassword) {
        String username = vertxRouterVerticleOptions.getBasicAuthenticationUsername();
        String password = vertxRouterVerticleOptions.getBasicAuthenticationPassword();
        if (StringUtils.isAnyEmpty(username, password)) return true;
        if (StringUtils.isNoneEmpty(inputUsername, inputPassword)) {
            return username.equals(inputUsername) && password.equals(inputPassword);
        }
        return false;
    }
}
