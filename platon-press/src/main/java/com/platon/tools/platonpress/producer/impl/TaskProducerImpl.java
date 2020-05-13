package com.platon.tools.platonpress.producer.impl;

import com.platon.tools.platonpress.config.PressProperties;
import com.platon.tools.platonpress.enums.TxTypeEnum;
import com.platon.tools.platonpress.event.task.EvmContractTxEvent;
import com.platon.tools.platonpress.event.task.TranferTxEvent;
import com.platon.tools.platonpress.event.task.TxEvent;
import com.platon.tools.platonpress.event.task.WasmContractTxEvent;
import com.platon.tools.platonpress.producer.TaskProducer;
import com.platon.tools.platonpress.producer.publish.TaskPublish;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Range;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.LongStream;

@Slf4j
@Component
public class TaskProducerImpl implements TaskProducer {

    @Autowired
    private PressProperties pressProperties;
    @Autowired
    private TaskPublish taskPublish;

    @Override
    public CompletableFuture<Void> start() throws Exception {
        //微秒
        final long sleepMicrosTime = 1000000L/pressProperties.getTps();
        //总的比例数
        final long totalRate = pressProperties.getTxRate().stream().mapToLong(x -> x).sum();
        //转账交易的范围
        final Range<Long> tranferRateRange = createRange(TxTypeEnum.TRANFER);
        //evm交易的范围
        final Range<Long> evmRateRange = createRange(TxTypeEnum.EVM);
        //wasm交易的范围
        final Range<Long> wasmRateRange = createRange(TxTypeEnum.WASM);

        return  CompletableFuture.runAsync(()->{
            LongStream.range(0,pressProperties.getTotalTx())
                    .mapToObj(i -> {
                        long to = i % totalRate;
                        TxEvent txEvent;
                        if(tranferRateRange.contains(to)){
                            txEvent = new TranferTxEvent();
                            txEvent.setGasLimit(pressProperties.getTranferGasLimit());
                            txEvent.setGasPrice(pressProperties.getTranferGasPrice());
                            txEvent.setNeedReceipt(pressProperties.isTranferNeedReceipt());
                            txEvent.setEstimateGas(pressProperties.isTranferEstimateGas());
                            txEvent.setGasInsuranceValue(pressProperties.getTranferGasInsuranceValue());
                        }else if(evmRateRange.contains(to)){
                            txEvent = new EvmContractTxEvent();
                            txEvent.setGasLimit(pressProperties.getEvmGasLimit());
                            txEvent.setGasPrice(pressProperties.getEvmGasPrice());
                            txEvent.setNeedReceipt(pressProperties.isEvmNeedReceipt());
                            txEvent.setEstimateGas(pressProperties.isEvmEstimateGas());
                            txEvent.setGasInsuranceValue(pressProperties.getEvmGasInsuranceValue());
                        }else if(wasmRateRange.contains(to)){
                            txEvent = new WasmContractTxEvent();
                            txEvent.setGasLimit(pressProperties.getWasmGasLimit());
                            txEvent.setGasPrice(pressProperties.getWasmGasPrice());
                            txEvent.setNeedReceipt(pressProperties.isWasmNeedReceipt());
                                txEvent.setEstimateGas(pressProperties.isWasmEstimateGas());
                            txEvent.setGasInsuranceValue(pressProperties.getWasmGasInsuranceValue());
                        }else {
                            throw new RuntimeException("tx-type or tx-rate config error!");
                        }
                        return txEvent;
                    } )
                    .forEach(event -> {
                        try {
                            TimeUnit.MICROSECONDS.sleep(sleepMicrosTime);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        taskPublish.publish(event);
                    });
        });
    }


    private Range<Long> createRange(TxTypeEnum txTypeEnum) {
        if (pressProperties.getTxType().contains(txTypeEnum)) {
            int index = pressProperties.getTxType().indexOf(txTypeEnum);
            long start = pressProperties.getTxRate().stream().mapToLong(x -> x).limit(index).sum();
            long end = start + pressProperties.getTxRate().get(index) - 1;
            return Range.between(start, end);
        } else {
            return Range.between(-1L, -1L);
        }
    }
}
