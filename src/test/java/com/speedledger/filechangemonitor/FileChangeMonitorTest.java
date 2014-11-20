package com.speedledger.filechangemonitor;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;

public class FileChangeMonitorTest {
    private final static String TMP_DIR = System.getProperty("java.io.tmpdir");

    private static File testDir;
    private static File testFile;

    Thread monitor;
    Map<String, CountDownLatch> latches;

    @Before
    public void before() {
        final String DIR = "testdir";
        final String FILE = "testfile";
        testDir = new File(TMP_DIR + File.separator + DIR);
        testDir.mkdir();
        testFile = new File(TMP_DIR + File.separator + DIR + File.separator + FILE);
        latches = new HashMap<>();
    }

    @Test
    public void testFileChange() throws IOException, InterruptedException {
        addLatchAfterNumberOfChanges("fileChange", 2); // fileChanged is called when monitor is initialized

        initMonitor(this::fileChanged);

        FileUtils.writeStringToFile(testFile, "B");
        assertFileChange("fileChange");
    }

    @Test
    public void testFileDelete() throws Exception {
        addLatchAfterNumberOfChanges("one", 2);
        addLatchAfterNumberOfChanges("two", 3);
        addLatchAfterNumberOfChanges("three", 4);
        initMonitor(this::fileChanged);

        testFile.delete();
        assertFileChange("one");

        FileUtils.writeStringToFile(testFile, "I'm back!");
        assertFileChange("two");

        FileUtils.writeStringToFile(testFile, "I'm still watched");
        assertFileChange("three");
    }

    @Test
    public void testActionThrowsException() throws Exception {
        addLatchAfterNumberOfChanges("B", 2);
        initMonitor(this::fileChangedWithException);

        FileUtils.writeStringToFile(testFile, "B");
        assertFileChange("B");
    }

    private void addLatchAfterNumberOfChanges(String name, int numberOfChanges) {
        latches.put(name, new CountDownLatch(numberOfChanges));
    }

    private void assertFileChange(String name) throws InterruptedException {
        assertTrue("Timeout waiting for file change event", latches.get(name).await(60, TimeUnit.SECONDS));
    }

    private void initMonitor(FileChangeMonitor.OnFileChange action) throws IOException, InterruptedException {
        FileUtils.writeStringToFile(testFile, "Original file");
        monitor = new Thread(new FileChangeMonitor(testDir.getAbsolutePath(), testFile.getName(), action, 1));
        monitor.start();
        Thread.sleep(1000);
    }

    private void fileChanged(String dir, String file) {
        for (CountDownLatch latch : latches.values()) {
            latch.countDown();
        }
    }

    private void fileChangedWithException(String dir, String file) {
        for (CountDownLatch latch : latches.values()) {
            latch.countDown();
        }

        throw new NullPointerException("An exception in action!");
    }

    @After
    public void after() {
        monitor.interrupt();
        testFile.delete();
        testDir.delete();
    }
}
