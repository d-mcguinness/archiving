package com.dmc.archiving.auth;

import com.dmc.archiving.auth.api.AuthContext;
import com.dmc.archiving.auth.api.TokenSigner;
import graphql.ExecutionInput;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.http.HttpHeaders;
import reactor.core.publisher.Mono;

import java.util.function.BiFunction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies the GraphQL auth interceptor verifies the bearer-token signature
 * (Review L5 — mirrors RestAuthInterceptorTest on the GraphQL surface, the
 * primary API for tenant/admin mutations): a valid signed token stashes its
 * identity in the GraphQLContext; a forged/unsigned/missing token stashes
 * ANONYMOUS, so a mutation cannot be reached with a bogus token.
 */
class GraphQlAuthInterceptorTest {

    private final TokenSigner signer = new TokenSigner("test-secret");
    private final GraphQlAuthInterceptor interceptor = new GraphQlAuthInterceptor(signer);

    /** Drive intercept() and return the AuthContext it stashes into the execution input's context. */
    @SuppressWarnings("unchecked")
    private AuthContext stashedContextFor(String authHeader) {
        WebGraphQlRequest request = mock(WebGraphQlRequest.class);
        HttpHeaders headers = new HttpHeaders();
        if (authHeader != null) {
            headers.add("Authorization", authHeader);
        }
        when(request.getHeaders()).thenReturn(headers);
        WebGraphQlInterceptor.Chain chain = r -> Mono.empty();

        interceptor.intercept(request, chain);

        ArgumentCaptor<BiFunction<ExecutionInput, ExecutionInput.Builder, ExecutionInput>> configurer =
                ArgumentCaptor.forClass(BiFunction.class);
        verify(request).configureExecutionInput(configurer.capture());

        ExecutionInput original = ExecutionInput.newExecutionInput("{__typename}").build();
        ExecutionInput configured =
                configurer.getValue().apply(original, ExecutionInput.newExecutionInput("{__typename}"));
        return configured.getGraphQLContext().get("authContext");
    }

    @Test
    void validSignedTokenStashesItsIdentity() {
        AuthContext ctx = stashedContextFor(signer.issue(1L, "admin", "ADMIN"));
        assertThat(ctx.isAuthenticated()).isTrue();
        assertThat(ctx.isAdmin()).isTrue();
    }

    @Test
    void forgedUnsignedTokenStashesAnonymous() {
        AuthContext ctx = stashedContextFor("Bearer_attacker_ADMIN_0");
        assertThat(ctx).isEqualTo(AuthContext.ANONYMOUS);
        assertThat(ctx.isAuthenticated()).isFalse();
    }

    @Test
    void missingTokenStashesAnonymous() {
        assertThat(stashedContextFor(null)).isEqualTo(AuthContext.ANONYMOUS);
    }
}
