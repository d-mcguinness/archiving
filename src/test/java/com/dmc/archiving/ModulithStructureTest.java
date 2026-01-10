package com.dmc.archiving;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Test to verify Spring Modulith module structure and boundaries
 */
@SpringBootTest
class ModulithStructureTest {

    ApplicationModules modules = ApplicationModules.of(ArchivingApplication.class);

    @Test
    void verifiesModularStructure() {
        // This will verify that the module structure is valid
        modules.verify();
    }

    @Test
    void createModuleDocumentation() {
        // This will create documentation for the modules
        new Documenter(modules)
                .writeDocumentation()
                .writeIndividualModulesAsPlantUml();
    }

    @Test
    void printModules() {
        // Print out the discovered modules
        modules.forEach(System.out::println);
    }
}
