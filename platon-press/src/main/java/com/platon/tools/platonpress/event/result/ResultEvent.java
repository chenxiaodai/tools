package com.platon.tools.platonpress.event.result;

import com.platon.tools.platonpress.event.task.TxEvent;
import lombok.Data;

import java.util.Date;

@Data
public class ResultEvent {
    private String txHash;
    private boolean commitOk;
    private boolean receiptOk;
    private TxEvent event;
    private String msg;
    private Date begin;
    private Date end;

    @Override
    public String toString() {
        return "ResultEvent{" +
                "txHash='" + txHash + '\'' +
                ", commitOk=" + commitOk +
                ", receiptOk=" + receiptOk +
                ", event=" + event +
                ", msg='" + msg + '\'' +
                ", begin=" + begin +
                ", end=" + end +
                ", duration=" + ( end.getTime() - begin.getTime()) + " ms"+
                '}';
    }

    public static void main(String[] args) {
        System.out.println(new Date().getTime());

    }
}
