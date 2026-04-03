package org.productivitybuddy.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.productivitybuddy.model.Process;
import org.productivitybuddy.model.ProcessCategory;
import org.productivitybuddy.registry.impl.InMemoryProcessRegistry;

class ProcessAggregationServiceImplTest {
    private InMemoryProcessRegistry registry;
    private StubProcessScannerService scannerService;
    private ProcessAggregationServiceImpl aggregationService;

    @BeforeEach
    void setUp() {
        this.registry = new InMemoryProcessRegistry();
        this.scannerService = new StubProcessScannerService();
        this.aggregationService = new ProcessAggregationServiceImpl(this.registry, this.scannerService);
    }

    @Test
    void multipleInstancesSameNameShouldNotMultiplySavedTime() {
        // Saved: chrome has 5000s from previous sessions
        this.scannerService.putSaved(TestHelper.createSaved("chrome", 5000));

        // 20 active chrome instances, each with 60s session time
        for (int i = 0; i < 20; i++) {
            TestHelper.addToRegistry(this.registry, TestHelper.createProcess("chrome", 100 + i, 60));
        }

        // Expected: 5000 (saved) + 20*60 (session) = 6200
        assertEquals(6200, this.aggregationService.getDisplayTotalTimeSeconds("chrome"));
    }

    @Test
    void aggregatedProcessesShouldHaveOneEntryPerName() {
        this.scannerService.putSaved(TestHelper.createSaved("chrome", 5000));

        for (int i = 0; i < 20; i++) {
            TestHelper.addToRegistry(this.registry, TestHelper.createProcess("chrome", 100 + i, 60));
        }

        final List<Process> aggregated = this.aggregationService.getAggregatedProcesses();
        final long chromeCount = aggregated.stream()
            .filter(p -> "chrome".equals(p.getOriginalName()))
            .count();

        assertEquals(1, chromeCount);
        assertEquals(6200, aggregated.stream()
            .filter(p -> "chrome".equals(p.getOriginalName()))
            .findFirst().orElseThrow()
            .getTotalTimeSeconds());
    }

    @Test
    void savedProcessNotActiveInSessionShouldStillAppear() {
        this.scannerService.putSaved(TestHelper.createSaved("notepad", 3000, ProcessCategory.WORK));

        final List<Process> aggregated = this.aggregationService.getAggregatedProcesses();
        final Process notepad = aggregated.stream()
            .filter(p -> "notepad".equals(p.getOriginalName()))
            .findFirst().orElseThrow();

        assertEquals(3000, notepad.getTotalTimeSeconds());
        assertEquals(ProcessCategory.WORK, notepad.getProcessCategory());
    }

    @Test
    void newProcessNotInSavedShouldAppearWithSessionTimeOnly() {
        TestHelper.addToRegistry(this.registry, TestHelper.createProcess("newapp", 1, 120));

        assertEquals(120, this.aggregationService.getDisplayTotalTimeSeconds("newapp"));

        final List<Process> aggregated = this.aggregationService.getAggregatedProcesses();
        assertEquals(1, aggregated.size());
        assertEquals(120, aggregated.get(0).getTotalTimeSeconds());
    }

    @Test
    void activeMetadataShouldTakePriorityOverSaved() {
        this.scannerService.putSaved(TestHelper.createSaved("chrome", 5000, ProcessCategory.FUN));

        final Process active = TestHelper.createProcess("chrome", 1, 60, ProcessCategory.WORK);
        active.setAliasName("My Browser");
        TestHelper.addToRegistry(this.registry, active);

        final Process aggregated = this.aggregationService.getAggregatedProcesses().stream()
            .filter(p -> "chrome".equals(p.getOriginalName()))
            .findFirst().orElseThrow();

        assertEquals(ProcessCategory.WORK, aggregated.getProcessCategory());
        assertEquals("My Browser", aggregated.getAliasName());
        assertEquals(5060, aggregated.getTotalTimeSeconds());
    }

    @Test
    void timePerCategoryShouldAggregateCorrectly() {
        this.scannerService.putSaved(TestHelper.createSaved("idea", 1000, ProcessCategory.WORK));
        this.scannerService.putSaved(TestHelper.createSaved("spotify", 500, ProcessCategory.FUN));

        TestHelper.addToRegistry(this.registry, TestHelper.createProcess("idea", 1, 200, ProcessCategory.WORK));
        TestHelper.addToRegistry(this.registry, TestHelper.createProcess("spotify", 2, 100, ProcessCategory.FUN));
        TestHelper.addToRegistry(this.registry, TestHelper.createProcess("unknown", 3, 50));

        final Map<ProcessCategory, Long> timePerCategory = this.aggregationService.getTimePerCategory();

        assertEquals(1200, timePerCategory.get(ProcessCategory.WORK));
        assertEquals(600, timePerCategory.get(ProcessCategory.FUN));
        assertEquals(50, timePerCategory.get(ProcessCategory.UNCATEGORIZED));
    }

    @Test
    void aggregatedProcessesByCategoryShouldFilter() {
        this.scannerService.putSaved(TestHelper.createSaved("idea", 1000, ProcessCategory.WORK));
        this.scannerService.putSaved(TestHelper.createSaved("spotify", 500, ProcessCategory.FUN));

        TestHelper.addToRegistry(this.registry, TestHelper.createProcess("idea", 1, 200, ProcessCategory.WORK));
        TestHelper.addToRegistry(this.registry, TestHelper.createProcess("spotify", 2, 100, ProcessCategory.FUN));

        final List<Process> workProcesses = this.aggregationService.getAggregatedProcessesByCategory(ProcessCategory.WORK);
        assertEquals(1, workProcesses.size());
        assertEquals("idea", workProcesses.get(0).getOriginalName());
    }

    @Test
    void buildProcessSnapshotShouldReturnCopies() {
        this.scannerService.putSaved(TestHelper.createSaved("chrome", 5000));
        TestHelper.addToRegistry(this.registry, TestHelper.createProcess("chrome", 1, 60));

        final List<Process> snapshot = this.aggregationService.buildProcessSnapshot();
        assertEquals(1, snapshot.size());
        assertEquals(5060, snapshot.get(0).getTotalTimeSeconds());

        // Modifying snapshot should not affect aggregation
        snapshot.get(0).setTotalTimeSeconds(0);
        assertEquals(5060, this.aggregationService.getDisplayTotalTimeSeconds("chrome"));
    }

    @Test
    void displayTotalForUnknownProcessShouldBeZero() {
        assertEquals(0, this.aggregationService.getDisplayTotalTimeSeconds("nonexistent"));
    }

    @Test
    void emptyRegistryAndSavedShouldProduceEmptyResults() {
        assertTrue(this.aggregationService.getAggregatedProcesses().isEmpty());
        assertEquals(0, this.aggregationService.getTimePerCategory().get(ProcessCategory.WORK));
    }
}
