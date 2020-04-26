package com.platon.tools.platonpress.consumer.handler;

import com.lmax.disruptor.WorkHandler;
import com.platon.tools.platonpress.client.PlatOnClient;
import com.platon.tools.platonpress.config.PressProperties;
import com.platon.tools.platonpress.event.ObjectEvent;
import com.platon.tools.platonpress.event.result.ResultEvent;
import com.platon.tools.platonpress.event.task.EvmContractTxEvent;
import com.platon.tools.platonpress.event.task.TranferTxEvent;
import com.platon.tools.platonpress.event.task.TxEvent;
import com.platon.tools.platonpress.event.task.WasmContractTxEvent;
import com.platon.tools.platonpress.manager.FromInfoManager;
import com.platon.tools.platonpress.manager.ToAddressManager;
import com.platon.tools.platonpress.manager.dto.FromInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Hash;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.exceptions.TransactionException;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.Date;
import java.util.Optional;

@Slf4j
@Component
public class TaskHandler implements WorkHandler<ObjectEvent<TxEvent>> {

    @Autowired
    private ToAddressManager toAddressManager;
    @Autowired
    private FromInfoManager credentialsManager;
    @Autowired
    private PlatOnClient platOnClient;
    @Autowired
    private PressProperties pressProperties;

    @Override
    public void onEvent(ObjectEvent<TxEvent> event) throws Exception {
        FromInfo fromInfo = null;
        String txHashLocal;
        ResultEvent resultEvent = new ResultEvent();
        resultEvent.setBegin(new Date());
        try {
            TxEvent txEvent = event.getEvent();
            //1. 获得Credentials
            fromInfo = credentialsManager.borrow();
            //2. 获得to地址
            String to = toAddressManager.getAddress(txEvent);
            //3. 构造交易对象
            RawTransaction rawTransaction = createTransaction(txEvent,fromInfo,to);
            //4. 签名
            String signedData = signTransaction(rawTransaction,fromInfo);
            //5. 提交
            txHashLocal = Hash.sha3(signedData);
            resultEvent.setTxHash(txHashLocal);
            String txHashRemote = platOnClient.platonSendRawTransaction(signedData);
            if (!txHashRemote.equals(txHashLocal)) {
                throw new RuntimeException("交易hash不一致！");
            }
            fromInfo.getNorce().incrementAndGet();
            resultEvent.setCommitOk(true);
            //6. 查询回执
            if(txEvent.isNeedReceipt()){
                TransactionReceipt transactionReceipt = checkTransaction(txHashRemote);
                if(transactionReceipt.isStatusOK()){
                    resultEvent.setReceiptOk(true);
                }else{
                    System.out.println("交易失败！ txHash = " + txHashRemote);
                    resultEvent.setReceiptOk(false);
                }
            }
            credentialsManager.yet(fromInfo);
        } catch (Exception e){
            log.error("handler execute error!", e);
            resultEvent.setMsg(e.getMessage());
            if(fromInfo != null ){
                credentialsManager.yet(fromInfo);
            }
        } finally {
            resultEvent.setEnd(new Date());
            log.info(resultEvent.toString());
        }
    }

    private TransactionReceipt checkTransaction(String txHash) throws Exception{
        int attempts = 180;
        long sleepDuration = 1000;

        Optional<TransactionReceipt> receiptOptional = platOnClient.platonGetTransactionReceipt(txHash);
        for (int i = 0; i < attempts; i++) {
            if (!receiptOptional.isPresent()) {
                try {
                    Thread.sleep(sleepDuration);
                } catch (InterruptedException e) {
                    throw new TransactionException(e);
                }
                receiptOptional = platOnClient.platonGetTransactionReceipt(txHash);
            } else {
                return receiptOptional.get();
            }
        }

        throw new RuntimeException("Transaction receipt was not generated after "
                + ((sleepDuration * attempts) / 1000
                + " seconds for transaction: " + txHash));
    }


    private String signTransaction(RawTransaction rawTransaction,  FromInfo fromInfo){
        Credentials credentials = fromInfo.getCredentials();
        byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, pressProperties.getChainId(), credentials);
        return Numeric.toHexString(signedMessage);
    }

    private RawTransaction createTransaction(TxEvent txEvent, FromInfo fromInfo, String to){
        BigInteger nonce = BigInteger.valueOf(fromInfo.getNorce().get());
        BigInteger gasPrice = txEvent.getGasPrice();
        BigInteger gasLimit = txEvent.getGasLimit();
        BigInteger amount = BigInteger.ZERO;
        if(txEvent instanceof TranferTxEvent){
            amount = pressProperties.getTranferValue();
        }
        String data = "";
        if(txEvent instanceof WasmContractTxEvent){
            //TODO
        }
        if(txEvent instanceof EvmContractTxEvent){
            //TODO
        }
        return RawTransaction.createTransaction(nonce, gasPrice, gasLimit, to, amount, "");
    }





}
