package com.platon.tools.platonpress.event.result;

import lombok.Data;
import lombok.ToString;

import java.util.Date;

@Data
@ToString
public class ResultEvent {
    private String txHash;
    private boolean commitOk;
    private boolean receiptOk;
    private String msg;
    private Date begin;
    private Date end;
}
