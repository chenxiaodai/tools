package com.platon.tools.platonpress.manager.impl;

import com.platon.tools.platonpress.client.PlatOnClient;
import com.platon.tools.platonpress.config.PressProperties;
import com.platon.tools.platonpress.exception.LimitException;
import com.platon.tools.platonpress.manager.LimitManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LimitManagerImpl implements LimitManager {

    @Autowired
    private PressProperties pressProperties;

    @Autowired
    private PlatOnClient platOnClient;

    @Override
    public void isAllowed() {
        if(pressProperties.isLimitMaxPendingTxSize()){
            int curSize = platOnClient.platonPendingTransactionsLength();
            System.err.println("pennding2 = " + curSize);
            if(curSize > pressProperties.getMaxPendingTxSize()){
                throw new LimitException(String.format("pending tx too much! limit = %d  cur = %d", pressProperties.getMaxPendingTxSize(), curSize));
            }
        }
    }
}
