package com.dmc.archiving.scheme.api;

/** One allowed value of an enumerated field, with the vocabulary's description as its label when it has one. */
public record FieldOption(String value, String label) {}
