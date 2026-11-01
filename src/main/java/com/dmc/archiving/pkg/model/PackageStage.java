package com.dmc.archiving.pkg.model;

/**
 * Lifecycle phase of an archival package. The OAIS marker is what generators
 * key their storage prefix and filenames on ({@code sips/…}, {@code aips/…},
 * {@code dips/…}); it is normative in the generated output and therefore not
 * renamed with the product vocabulary.
 */
public enum PackageStage {
    INTAKE("sip"),
    PRESERVATION("aip"),
    RELEASE("dip");

    private final String packageType;

    PackageStage(String packageType) {
        this.packageType = packageType;
    }

    /** The generator marker for this stage — matches {@code PackageGenerator.packageType()}. */
    public String packageType() {
        return packageType;
    }

    /** Inverse of {@link #packageType()}. */
    public static PackageStage fromPackageType(String packageType) {
        for (PackageStage s : values()) {
            if (s.packageType.equals(packageType)) return s;
        }
        throw new IllegalArgumentException("Unknown package type: " + packageType);
    }
}
