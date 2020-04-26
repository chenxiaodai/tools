package com.platon.tools.platonpress.consumer.handler;

import com.lmax.disruptor.WorkHandler;
import com.platon.tools.platonpress.event.ObjectEvent;
import com.platon.tools.platonpress.event.task.TxEvent;
import com.platon.tools.platonpress.manager.ToAddressManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TaskHandler implements WorkHandler<ObjectEvent<TxEvent>> {

    @Autowired
    private ToAddressManager toAddressManager;

    @Override
    public void onEvent(ObjectEvent<TxEvent> event) throws Exception {
        try {
            TxEvent txEvent = event.getEvent();
            //1. 获得Credentials

            //2. 获得to地址
            String to = toAddressManager.getAddress(txEvent);


            //2. 构造交易对象

            //2. 签名

            //3. 提交

            //4. 查询回执

            log.info("to:{}, event:{}",to,txEvent);
        } catch (Exception e){
            log.error("handler execute error!", e);
        }
    }

//    private RawTransaction createTransaction() throws Exception{
//        Credentials credentials = Credentials.create(ecKeyPair);
//
//        BigInteger gasPrice = web3j.platonGasPrice().send().getGasPrice();
//        BigInteger gasLimit = Transfer.GAS_LIMIT;
//        BigInteger nonce = getNonce(credentials);
//        BigInteger amount = new BigInteger("1000000000000000000");
//        String toAddress = "0x45886ffccf2c6726f44deec15446f9a53c007848";
//
//        return RawTransaction.createTransaction(nonce, gasPrice, gasLimit, toAddress, amount, "");
//    }





}
