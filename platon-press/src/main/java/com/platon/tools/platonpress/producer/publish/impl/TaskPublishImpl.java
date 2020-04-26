package com.platon.tools.platonpress.producer.publish.impl;

import com.lmax.disruptor.dsl.Disruptor;
import com.platon.tools.platonpress.event.ObjectEvent;
import com.platon.tools.platonpress.event.task.TxEvent;
import com.platon.tools.platonpress.producer.publish.TaskPublish;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TaskPublishImpl implements TaskPublish {

    @Autowired
    private Disruptor<ObjectEvent<TxEvent>> disruptor;

    public void publish(TxEvent txEvent) {
        long sequence = disruptor.getRingBuffer().next();
        try {
            ObjectEvent<TxEvent> event = disruptor.getRingBuffer().get(sequence);
            event.setEvent(txEvent);
        } finally {
            disruptor.getRingBuffer().publish(sequence);
        }
    }
}
