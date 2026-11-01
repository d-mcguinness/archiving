package com.dmc.archiving.pkg.generator;

import com.dmc.archiving.archive.model.ArchiveStandard;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AbstractPackageGeneratorFactoryTest {

    /** Minimal generator stub: only the lookup keys matter here. */
    private record Stub(ArchiveStandard supportedStandard, String packageType) implements PackageGenerator<Object> {
        @Override public String generate(Object pkg) { return "url"; }
        @Override public String generate(PackageSnapshot snapshot) { return "url"; }
        @Override public String getStandardName() { return supportedStandard == null ? "DEFAULT" : supportedStandard.name(); }
    }

    private static final class Factory extends AbstractPackageGeneratorFactory<Stub> {
        Factory(List<Stub> gens) { super(gens); }
    }

    private static final Stub DEFAULT = new Stub(null, "sip");
    private static final Stub NOARK5 = new Stub(ArchiveStandard.NOARK5, "sip");
    private static final Stub EARK = new Stub(ArchiveStandard.EARK, "sip");

    @Test
    void resolvesDedicatedGeneratorByStandard() {
        Factory f = new Factory(List.of(DEFAULT, NOARK5, EARK));
        assertThat(f.getGenerator(ArchiveStandard.NOARK5)).isSameAs(NOARK5);
        assertThat(f.getGenerator(ArchiveStandard.EARK)).isSameAs(EARK);
        assertThat(f.hasDedicatedGenerator(ArchiveStandard.EARK)).isTrue();
    }

    @Test
    void fallsBackToDefaultForUnknownOrNullStandard() {
        Factory f = new Factory(List.of(NOARK5, DEFAULT));
        assertThat(f.getGenerator(ArchiveStandard.METS)).isSameAs(DEFAULT);
        assertThat(f.getGenerator(null)).isSameAs(DEFAULT);
        assertThat(f.hasDedicatedGenerator(ArchiveStandard.METS)).isFalse();
        assertThat(f.hasDedicatedGenerator(null)).isFalse();
    }

    @Test
    void refusesToStartWithoutAFallback() {
        assertThatThrownBy(() -> new Factory(List.of(NOARK5)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("fallback");
    }

    @Test
    void refusesTwoFallbacks() {
        assertThatThrownBy(() -> new Factory(List.of(DEFAULT, new Stub(null, "sip"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Two fallback");
    }

    @Test
    void refusesTwoGeneratorsForOneStandard() {
        assertThatThrownBy(() -> new Factory(List.of(DEFAULT, NOARK5, new Stub(ArchiveStandard.NOARK5, "sip"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("NOARK5");
    }
}
