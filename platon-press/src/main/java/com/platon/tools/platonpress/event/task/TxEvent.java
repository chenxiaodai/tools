package com.platon.tools.platonpress.event.task;

import lombok.Data;

import java.math.BigInteger;

@Data
public class TxEvent {
    private BigInteger gasPrice;
    private BigInteger gasLimit;
    private boolean isNeedReceipt;
}
