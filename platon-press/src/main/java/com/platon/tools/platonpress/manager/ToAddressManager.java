package com.platon.tools.platonpress.manager;

import com.platon.tools.platonpress.event.task.TxEvent;

public interface ToAddressManager {
    String getAddress(TxEvent txEvent);
}
