package com.platon.tools.platonpress.producer;


import java.util.concurrent.CompletableFuture;

public interface TaskProducer {
    CompletableFuture<Void> start() throws Exception;
}