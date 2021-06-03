package pro.gravit.utils;

import pro.gravit.launcher.AsyncDownloader;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Downloader {
    public interface DownloadCallback {
        void apply(long fullDiff);
    }

    public CompletableFuture<Void> downloadList(List<AsyncDownloader.SizedFile> files, String baseURL, Path targetDir, DownloadCallback callback, ExecutorService executor, int threads) throws Exception {
        final boolean closeExecutor;
        if (executor == null) {
            executor = Executors.newWorkStealingPool(4);
            closeExecutor = true;
        } else {
            closeExecutor = false;
        }
        AsyncDownloader asyncDownloader = new AsyncDownloader((diff) -> {
            if (callback != null) {
                callback.apply(diff);
            }
        });
        List<List<AsyncDownloader.SizedFile>> list = asyncDownloader.sortFiles(files, threads);
        CompletableFuture<Void> future = CompletableFuture.allOf(asyncDownloader.runDownloadList(list, baseURL, targetDir, executor));

        ExecutorService finalExecutor = executor;
        return future.thenAccept(e -> {
            if (closeExecutor) {
                finalExecutor.shutdownNow();
            }
        });
    }
}