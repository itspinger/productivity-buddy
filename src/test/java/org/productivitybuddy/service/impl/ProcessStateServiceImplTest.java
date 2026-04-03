package org.productivitybuddy.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.productivitybuddy.model.Process;
import org.productivitybuddy.model.ProcessCategory;
import org.productivitybuddy.registry.impl.InMemoryProcessRegistry;

class ProcessStateServiceImplTest {
    private InMemoryProcessRegistry registry;
    private StubProcessScannerService scannerService;
    private ProcessStateServiceImpl stateService;

    @BeforeEach
    void setUp() {
        this.registry = new InMemoryProcessRegistry();
        this.scannerService = new StubProcessScannerService();
        this.stateService = new ProcessStateServiceImpl(this.registry, this.scannerService);
    }

    @Test
    void freezeShouldApplyToAllInstancesAndSaved() {
        this.scannerService.putSaved(TestHelper.createSaved("zsh", 1000));

        // 5 active zsh instances
        for (int i = 0; i < 5; i++) {
            TestHelper.addToRegistry(this.registry, TestHelper.createProcess("zsh", 10 + i, 30));
        }

        this.stateService.setTrackingFrozenByOriginalName("zsh", true);

        // All active instances should be frozen
        for (final Process p : this.registry.findByName("zsh")) {
            assertTrue(p.isTrackingFrozen());
        }

        // Saved should also be frozen (so new instances inherit it)
        assertTrue(this.scannerService.getSavedProcesses().get("zsh").isTrackingFrozen());
    }

    @Test
    void renameShouldApplyToAllInstancesAndSaved() {
        this.scannerService.putSaved(TestHelper.createSaved("chrome", 5000));

        TestHelper.addToRegistry(this.registry, TestHelper.createProcess("chrome", 1, 60));
        TestHelper.addToRegistry(this.registry, TestHelper.createProcess("chrome", 2, 60));

        this.stateService.renameByOriginalName("chrome", "My Browser");

        for (final Process p : this.registry.findByName("chrome")) {
            assertEquals("My Browser", p.getAliasName());
        }

        assertEquals("My Browser", this.scannerService.getSavedProcesses().get("chrome").getAliasName());
    }

    @Test
    void changeCategoryShouldApplyToAllInstancesAndSaved() {
        this.scannerService.putSaved(TestHelper.createSaved("idea", 3000));

        TestHelper.addToRegistry(this.registry, TestHelper.createProcess("idea", 1, 100));

        this.stateService.changeCategoryByOriginalName("idea", ProcessCategory.WORK);

        assertEquals(ProcessCategory.WORK, this.registry.findByName("idea").get(0).getProcessCategory());
        assertEquals(ProcessCategory.WORK, this.scannerService.getSavedProcesses().get("idea").getProcessCategory());
    }

    @Test
    void loadStateShouldUpdateMetadataButNotSessionTime() {
        // Active chrome with 60s session time and UNCATEGORIZED
        TestHelper.addToRegistry(this.registry, TestHelper.createProcess("chrome", 1, 60));

        // Load from file: chrome is WORK with alias
        final Process loaded = TestHelper.createSaved("chrome", 5000, ProcessCategory.WORK);
        loaded.setAliasName("Browser");

        this.stateService.loadState(List.of(loaded));

        final Process active = this.registry.findByName("chrome").get(0);
        assertEquals(ProcessCategory.WORK, active.getProcessCategory());
        assertEquals("Browser", active.getAliasName());
        // Session time must NOT be reset
        assertEquals(60, active.getTotalTimeSeconds());
    }

    @Test
    void commitStateShouldResetSessionTime() {
        TestHelper.addToRegistry(this.registry, TestHelper.createProcess("chrome", 1, 300));
        TestHelper.addToRegistry(this.registry, TestHelper.createProcess("idea", 2, 500));

        final Process snapshot = TestHelper.createSaved("chrome", 5300);
        this.stateService.commitState(List.of(snapshot));

        // Session time should be reset to 0
        assertEquals(0, this.registry.findByName("chrome").get(0).getTotalTimeSeconds());
        assertEquals(0, this.registry.findByName("idea").get(0).getTotalTimeSeconds());

        // Saved should have the snapshot values
        assertEquals(5300, this.scannerService.getSavedProcesses().get("chrome").getTotalTimeSeconds());
    }

    @Test
    void unfreezeAfterFreezeShouldWork() {
        this.scannerService.putSaved(TestHelper.createSaved("zsh", 100));
        TestHelper.addToRegistry(this.registry, TestHelper.createProcess("zsh", 1, 10));

        this.stateService.setTrackingFrozenByOriginalName("zsh", true);
        assertTrue(this.registry.findByName("zsh").get(0).isTrackingFrozen());
        assertTrue(this.scannerService.getSavedProcesses().get("zsh").isTrackingFrozen());

        this.stateService.setTrackingFrozenByOriginalName("zsh", false);
        assertFalse(this.registry.findByName("zsh").get(0).isTrackingFrozen());
        assertFalse(this.scannerService.getSavedProcesses().get("zsh").isTrackingFrozen());
    }

    @Test
    void metadataChangeOnProcessNotInSavedShouldNotCrash() {
        TestHelper.addToRegistry(this.registry, TestHelper.createProcess("newapp", 1, 10));

        // Should not throw even though "newapp" is not in saved
        this.stateService.setTrackingFrozenByOriginalName("newapp", true);
        this.stateService.renameByOriginalName("newapp", "My App");
        this.stateService.changeCategoryByOriginalName("newapp", ProcessCategory.WORK);

        assertEquals("My App", this.registry.findByName("newapp").get(0).getAliasName());
    }
}
