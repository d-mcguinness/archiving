package com.dmc.archiving.scheme.api;

/** One typed field of an entity. {@code type} is "string", "text", "date" or "number". */
public record FieldDefinition(String name, String label, String type, boolean required) {}
