package com.dmc.archiving.archive.strategy;

import com.dmc.archiving.archive.model.ArchiveStandard;
import com.dmc.archiving.archive.strategy.impl.*;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Factory for creating ArchiveStrategy instances based on archiving standard
 */
@Component
public class ArchiveStrategyFactory {

    private final Map<ArchiveStandard, ArchiveStrategy> strategies;
    private final DefaultArchiveStrategy defaultStrategy;

    public ArchiveStrategyFactory(
            Noark5Strategy noark5Strategy,
            OaisStrategy oaisStrategy,
            PremisStrategy premisStrategy,
            DublinCoreStrategy dublinCoreStrategy,
            MetsStrategy metsStrategy,
            EadStrategy eadStrategy,
            BagitStrategy bagitStrategy,
            IsadgStrategy isadgStrategy,
            ModsStrategy modsStrategy,
            EarkStrategy earkStrategy,
            DefaultArchiveStrategy defaultStrategy) {

        this.strategies = new HashMap<>();
        this.defaultStrategy = defaultStrategy;

        // Register all strategies with enum keys
        strategies.put(ArchiveStandard.NOARK5, noark5Strategy);
        strategies.put(ArchiveStandard.OAIS, oaisStrategy);
        strategies.put(ArchiveStandard.PREMIS, premisStrategy);
        strategies.put(ArchiveStandard.DUBLIN_CORE, dublinCoreStrategy);
        strategies.put(ArchiveStandard.METS, metsStrategy);
        strategies.put(ArchiveStandard.EAD, eadStrategy);
        strategies.put(ArchiveStandard.BAGIT, bagitStrategy);
        strategies.put(ArchiveStandard.ISADG, isadgStrategy);
        strategies.put(ArchiveStandard.MODS, modsStrategy);
        strategies.put(ArchiveStandard.EARK, earkStrategy);
    }

    /**
     * Get strategy for a specific archiving standard (enum)
     *
     * @param standard The ArchiveStandard enum value
     * @return The appropriate strategy
     */
    public ArchiveStrategy getStrategy(ArchiveStandard standard) {
        if (standard == null) {
            return getDefaultStrategy("UNKNOWN");
        }
        return strategies.getOrDefault(standard, getDefaultStrategy(standard.name()));
    }

    /**
     * Get strategy for a specific archiving standard (String) - for backward compatibility
     *
     * @param standardName The name of the standard (e.g., "NOARK5", "OAIS")
     * @return The appropriate strategy
     */
    public ArchiveStrategy getStrategy(String standardName) {
        if (standardName == null) {
            return getDefaultStrategy("UNKNOWN");
        }

        try {
            // Try to parse as enum
            String normalizedName = standardName.toUpperCase().replace(" ", "_");
            ArchiveStandard standard = ArchiveStandard.valueOf(normalizedName);
            return getStrategy(standard);
        } catch (IllegalArgumentException e) {
            // If not a valid enum, return default strategy
            return getDefaultStrategy(standardName);
        }
    }

    /**
     * Get default strategy with custom standard name
     */
    private ArchiveStrategy getDefaultStrategy(String standardName) {
        DefaultArchiveStrategy strategy = new DefaultArchiveStrategy(standardName);
        return strategy;
    }

    /**
     * Check if a strategy exists for a standard
     */
    public boolean hasStrategy(ArchiveStandard standard) {
        return standard != null && strategies.containsKey(standard);
    }

    /**
     * Check if a strategy exists for a standard (String version)
     */
    public boolean hasStrategy(String standardName) {
        if (standardName == null) return false;
        try {
            String normalizedName = standardName.toUpperCase().replace(" ", "_");
            ArchiveStandard standard = ArchiveStandard.valueOf(normalizedName);
            return strategies.containsKey(standard);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
