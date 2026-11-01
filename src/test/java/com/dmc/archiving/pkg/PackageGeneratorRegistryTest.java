package com.dmc.archiving.pkg;

import com.dmc.archiving.archive.model.ArchiveStandard;
import com.dmc.archiving.pkg.generator.PackageGenerator;
import com.dmc.archiving.pkg.generator.PackageSnapshot;
import com.dmc.archiving.pkg.model.PackageStage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PackageGeneratorRegistryTest {

    private record Stub(ArchiveStandard supportedStandard, String packageType) implements PackageGenerator<Object> {
        @Override public String generate(Object pkg) { return "url"; }
        @Override public String generate(PackageSnapshot snapshot) { return "url"; }
        @Override public String getStandardName() { return supportedStandard == null ? "DEFAULT" : supportedStandard.name(); }
    }

    private static final Stub SIP_DEFAULT = new Stub(null, "sip");
    private static final Stub SIP_NOARK5 = new Stub(ArchiveStandard.NOARK5, "sip");
    private static final Stub AIP_DEFAULT = new Stub(null, "aip");
    private static final Stub AIP_EARK = new Stub(ArchiveStandard.EARK, "aip");
    private static final Stub DIP_DEFAULT = new Stub(null, "dip");

    private final PackageGeneratorRegistry registry = new PackageGeneratorRegistry(
            List.of(SIP_DEFAULT, SIP_NOARK5, AIP_DEFAULT, AIP_EARK, DIP_DEFAULT));

    @Test
    void groupsGeneratorsByStageAndResolvesByStandard() {
        assertThat(registry.getGenerator(PackageStage.INTAKE, ArchiveStandard.NOARK5)).isSameAs(SIP_NOARK5);
        assertThat(registry.getGenerator(PackageStage.PRESERVATION, ArchiveStandard.EARK)).isSameAs(AIP_EARK);
    }

    @Test
    void fallsBackPerStage() {
        // NOARK5 is dedicated for INTAKE but not for PRESERVATION: the stage decides.
        assertThat(registry.getGenerator(PackageStage.PRESERVATION, ArchiveStandard.NOARK5)).isSameAs(AIP_DEFAULT);
        assertThat(registry.getGenerator(PackageStage.RELEASE, ArchiveStandard.NOARK5)).isSameAs(DIP_DEFAULT);
        assertThat(registry.getGenerator(PackageStage.RELEASE, null)).isSameAs(DIP_DEFAULT);
        assertThat(registry.hasDedicatedGenerator(PackageStage.INTAKE, ArchiveStandard.NOARK5)).isTrue();
        assertThat(registry.hasDedicatedGenerator(PackageStage.RELEASE, ArchiveStandard.NOARK5)).isFalse();
    }

    @Test
    void failsClearlyForAStageWithNoGenerators() {
        PackageGeneratorRegistry sipOnly = new PackageGeneratorRegistry(List.of(SIP_DEFAULT));
        assertThatThrownBy(() -> sipOnly.getGenerator(PackageStage.RELEASE, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("RELEASE");
        assertThat(sipOnly.hasDedicatedGenerator(PackageStage.RELEASE, ArchiveStandard.EARK)).isFalse();
    }

    @Test
    void rejectsAGeneratorWithAnUnknownPackageType() {
        assertThatThrownBy(() -> new PackageGeneratorRegistry(List.of(new Stub(null, "zip"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("zip");
    }

    @Test
    void stageAndPackageTypeRoundTrip() {
        for (PackageStage stage : PackageStage.values()) {
            assertThat(PackageStage.fromPackageType(stage.packageType())).isSameAs(stage);
        }
    }
}
