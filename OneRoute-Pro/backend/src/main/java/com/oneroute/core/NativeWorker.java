package com.oneroute.core;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/** Exclusive access is enforced by NativeWorkerPool. No shell is involved. */
public final class NativeWorker implements AutoCloseable {
    private final Path binary;
    private final Path graph;
    private Process process;
    private BufferedWriter input;
    private BlockingQueue<String> output;

    public NativeWorker(Path binary, Path graph) throws IOException {
        this.binary = binary;
        this.graph = graph;
        start();
    }

    private void start() throws IOException {
        process = new ProcessBuilder(binary.toString(), graph.toString())
                .redirectError(ProcessBuilder.Redirect.INHERIT).start();
        input = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
        output = new LinkedBlockingQueue<>();
        Process running = process;
        BlockingQueue<String> replies = output;
        Thread reader = new Thread(() -> readReplies(running, replies), "native-reply-reader");
        reader.setDaemon(true);
        reader.start();
    }

    private static void readReplies(Process running, BlockingQueue<String> replies) {
        try (var stream = new BufferedReader(new InputStreamReader(running.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = stream.readLine()) != null) {
                replies.offer(line);
            }
        } catch (IOException ignored) {
            // The EOF marker below makes the request fail without exposing system details.
        } finally {
            replies.offer("");
        }
    }

    public Map<String, Object> request(String query) throws IOException, InterruptedException {
        try {
            if (!process.isAlive()) {
                throw new IOException("Native worker exited");
            }
            input.write(query);
            input.newLine();
            input.flush();
            String line = output.poll(5, TimeUnit.SECONDS);
            if (line == null || line.isEmpty()) {
                throw new IOException("Native reply timed out or exited");
            }
            Map<String, Object> result = Json.object(Json.parse(line));
            if (result.containsKey("error")) {
                throw new IOException("Native worker rejected request");
            }
            return result;
        } catch (IOException | IllegalArgumentException exception) {
            close();
            start();
            throw new IOException("Worker restarted; retry request", exception);
        } catch (InterruptedException exception) {
            close();
            start();
            throw exception;
        }
    }

    @Override
    public void close() {
        if (process == null) {
            return;
        }
        process.destroyForcibly();
        try {
            process.waitFor(3, TimeUnit.SECONDS);
            input.close();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } catch (IOException ignored) {
            // Process termination has already closed the other end.
        }
    }
}
