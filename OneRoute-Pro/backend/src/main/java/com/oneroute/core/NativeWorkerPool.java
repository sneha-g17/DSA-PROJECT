package com.oneroute.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public final class NativeWorkerPool implements AutoCloseable {
    private final List<NativeWorker> workers = new ArrayList<>();
    private final BlockingQueue<NativeWorker> available = new ArrayBlockingQueue<>(2);
    private final Path graph;

    @FunctionalInterface
    public interface Task<T> {
        T execute(NativeWorker worker) throws IOException, InterruptedException;
    }

    public NativeWorkerPool(Path root, Network network) throws IOException {
        Path build = root.resolve("build");
        Files.createDirectories(build);
        String executable = System.getProperty("os.name").startsWith("Windows")
                ? "transit_engine.exe" : "transit_engine";
        Path binary = build.resolve(executable);
        if (!Files.isRegularFile(binary)) {
            throw new IOException("C++ engine missing: run the project build script first");
        }
        graph = Files.createTempFile(build, "network-", ".txt");
        try {
            network.writeGraph(graph);
            for (int i = 0; i < 2; i++) {
                NativeWorker worker = new NativeWorker(binary, graph);
                workers.add(worker);
                available.add(worker);
            }
        } catch (IOException exception) {
            close();
            throw exception;
        }
    }

    public <T> T execute(Task<T> task) throws IOException, InterruptedException {
        NativeWorker worker = available.poll(2, TimeUnit.SECONDS);
        if (worker == null) {
            throw new IOException("Routing queue is busy; retry shortly");
        }
        try {
            return task.execute(worker);
        } finally {
            available.offer(worker);
        }
    }

    @Override
    public void close() {
        workers.forEach(NativeWorker::close);
        try {
            Files.deleteIfExists(graph);
        } catch (IOException ignored) {
            // A temporary graph is harmless if an OS handle delays deletion.
        }
    }
}
