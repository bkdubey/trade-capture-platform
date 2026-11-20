package com.capitalmarkets.trade.service;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum TradeFileFormat {
    CSV("csv"),
    TXT("txt");

    private final String extension;

    TradeFileFormat(String extension) {
        this.extension = extension;
    }

    public String extension() {
        return extension;
    }

    public static Optional<TradeFileFormat> fromFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return Optional.empty();
        }
        int extensionSeparator = filename.lastIndexOf('.');
        if (extensionSeparator < 0 || extensionSeparator == filename.length() - 1) {
            return Optional.empty();
        }
        String extension = filename.substring(extensionSeparator + 1).toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(format -> format.extension.equals(extension))
                .findFirst();
    }

    public static String supportedExtensions() {
        return Arrays.stream(values())
                .map(format -> "." + format.extension)
                .reduce((first, second) -> first + ", " + second)
                .orElse("(none)");
    }
}
