package com.dmc.archiving.pkg.generator;

/**
 * What a generator hands to the storage layer: the bytes, the filename to use
 * (under {type}s/{id}/) and the content-type to record. Lets different standards
 * emit different formats (JSON, XML, ZIP, …) through the same seam.
 */
public record PackagePayload(byte[] bytes, String filename, String contentType) {}
