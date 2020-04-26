package com.platon.tools.platonpress.producer.publish;

import com.platon.tools.platonpress.event.task.TxEvent;

public interface TaskPublish {
    void publish(TxEvent txEvent);
}
