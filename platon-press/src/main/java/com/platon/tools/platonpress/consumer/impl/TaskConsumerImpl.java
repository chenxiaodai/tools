package com.platon.tools.platonpress.consumer.impl;

import com.lmax.disruptor.dsl.Disruptor;
import com.platon.tools.platonpress.config.PressProperties;
import com.platon.tools.platonpress.consumer.TaskConsumer;
import com.platon.tools.platonpress.consumer.handler.TaskHandler;
import com.platon.tools.platonpress.event.ObjectEvent;
import com.platon.tools.platonpress.event.task.TxEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TaskConsumerImpl implements TaskConsumer {

    @Autowired
    private Disruptor<ObjectEvent<TxEvent>> disruptor;
    @Autowired
    private PressProperties pressProperties;
    @Autowired
    private TaskHandler handler;

    @Override
    public void start() {
        TaskHandler[] handlers = new TaskHandler[pressProperties.getDisruptorConsumerNumber()];
        for(int i = 0; i < pressProperties.getDisruptorConsumerNumber(); i++) {
            handlers[i] = handler;
        }
        disruptor.handleEventsWithWorkerPool(handlers);
        disruptor.start();
    }
}
