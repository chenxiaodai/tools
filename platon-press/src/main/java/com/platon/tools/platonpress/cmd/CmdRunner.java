package com.platon.tools.platonpress.cmd;

import com.platon.tools.platonpress.consumer.TaskConsumer;
import com.platon.tools.platonpress.producer.TaskProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
public class CmdRunner implements CommandLineRunner {

    @Autowired
    TaskProducer taskProducer;
    @Autowired
    TaskConsumer taskConsumer;

    @Override
    public void run(String... line)  {
        try {
            //开启测试任务消费者
            taskConsumer.start();
            //开启测试任务生成者
            CompletableFuture<Void> producerFuture = taskProducer.start();
            //等待测试完成
            producerFuture.get();
        } catch (Exception e) {
            log.error("press error! ", e);
        }
    }
}
