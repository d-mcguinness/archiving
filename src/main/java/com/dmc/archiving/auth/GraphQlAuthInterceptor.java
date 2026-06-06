package com.dmc.archiving.auth;

import com.dmc.archiving.auth.api.AuthContext;
import com.dmc.archiving.auth.api.AuthTokens;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class GraphQlAuthInterceptor implements WebGraphQlInterceptor {

    @Override
    public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest request, Chain chain) {
        AuthContext authContext = AuthTokens.parse(request.getHeaders().getFirst("Authorization"));

        request.configureExecutionInput((executionInput, builder) ->
            builder.graphQLContext(ctx -> ctx.of("authContext", authContext)).build()
        );

        return chain.next(request);
    }
}
