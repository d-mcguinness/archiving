package com.dmc.archiving.auth;

import com.dmc.archiving.auth.RefreshTokenService.Rotation;
import com.dmc.archiving.auth.model.RefreshToken;
import com.dmc.archiving.auth.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies opaque refresh-token rotation with a controllable clock (mirrors
 * TokenSignerTest): a token rotates once, the consumed token is rejected,
 * re-presenting a rotated token is treated as theft (whole session set revoked),
 * expiry is enforced at the clock>=expiresAt boundary, and an explicit logout
 * revoke rejects WITHOUT tripping the theft nuke. The repository is a small
 * in-memory fake so rotation/reuse are exercised without a database.
 */
class RefreshTokenServiceTest {

    private static final long TTL = 60_000L;

    private final Map<String, RefreshToken> store = new HashMap<>();
    private final AtomicLong idSeq = new AtomicLong(1);
    private final AtomicLong now = new AtomicLong(0L);
    private RefreshTokenRepository repo;
    private RefreshTokenService service;

    @BeforeEach
    void setUp() {
        repo = mock(RefreshTokenRepository.class);
        when(repo.save(any(RefreshToken.class))).thenAnswer(inv -> {
            RefreshToken t = inv.getArgument(0);
            if (t.getId() == null) {
                t.setId(idSeq.getAndIncrement());
            }
            store.put(t.getTokenHash(), t);
            return t;
        });
        when(repo.findByTokenHash(anyString())).thenAnswer(inv ->
                Optional.ofNullable(store.get((String) inv.getArgument(0))));
        // Atomic claim: flip revoked false→true + link successor, only if live.
        when(repo.consumeForRotation(anyLong(), anyLong())).thenAnswer(inv -> {
            long id = inv.getArgument(0);
            for (RefreshToken t : store.values()) {
                if (t.getId() == id && !t.isRevoked()) {
                    t.setRevoked(true);
                    t.setRotatedToId(inv.getArgument(1));
                    return 1;
                }
            }
            return 0;
        });
        when(repo.revokeAllByUserId(anyLong())).thenAnswer(inv -> {
            long uid = inv.getArgument(0);
            int n = 0;
            for (RefreshToken t : store.values()) {
                if (t.getUserId() == uid && !t.isRevoked()) {
                    t.setRevoked(true);
                    n++;
                }
            }
            return n;
        });
        service = new RefreshTokenService(repo, TTL, now::get);
    }

    @Test
    void rotatesOnceAndConsumesThePresentedToken() {
        String first = service.generate(42L);

        Optional<Rotation> rotated = service.rotate(first);

        assertThat(rotated).isPresent();
        assertThat(rotated.get().userId()).isEqualTo(42L);
        assertThat(rotated.get().refreshToken()).isNotEqualTo(first); // a fresh successor
        // The successor continues the chain...
        assertThat(service.rotate(rotated.get().refreshToken())).isPresent();
    }

    @Test
    void reusingARotatedTokenIsTheft_revokesTheWholeChain() {
        String first = service.generate(7L);
        String second = service.rotate(first).orElseThrow().refreshToken();

        // Re-presenting the already-rotated FIRST token is the stolen-token signal.
        assertThat(service.rotate(first)).isEmpty();
        // ...and it nukes every live token for the user, including the successor.
        assertThat(service.rotate(second)).isEmpty();
    }

    @Test
    void rejectsAtTheExpiryBoundary() {
        now.set(0L);
        String justValid = service.generate(1L);   // expiresAt = TTL
        now.set(TTL - 1);                           // one ms before expiry → still valid
        assertThat(service.rotate(justValid)).isPresent();

        now.set(0L);
        String expires = service.generate(1L);      // expiresAt = TTL
        now.set(TTL);                               // clock reaches expiry → rejected
        assertThat(service.rotate(expires)).isEmpty();
    }

    @Test
    void logoutRevokeRejectsButIsNotTreatedAsTheft() {
        String a = service.generate(3L);
        String b = service.generate(3L); // a second, independent session for the same user

        service.revoke(a);

        assertThat(service.rotate(a)).isEmpty();       // revoked → rejected
        assertThat(service.rotate(b)).isPresent();     // the other session is untouched (no nuke)
    }

    @Test
    void unknownOrBlankTokenIsRejected() {
        assertThat(service.rotate("never-issued")).isEmpty();
        assertThat(service.rotate(null)).isEmpty();
        assertThat(service.rotate("  ")).isEmpty();
    }

    @Test
    void losingTheConcurrentClaimRejectsWithoutMintingOrNuking() {
        // Simulate the race: the token still reads as live, but the atomic
        // consumeForRotation reports 0 rows because a concurrent refresh already
        // claimed it. The loser must reject — NOT return a usable rotation, and
        // NOT fire a false reuse-nuke against the (now legitimately rotated) user.
        String token = service.generate(8L);
        when(repo.consumeForRotation(anyLong(), anyLong())).thenReturn(0);

        assertThat(service.rotate(token)).isEmpty();
        verify(repo, never()).revokeAllByUserId(anyLong());
    }
}
