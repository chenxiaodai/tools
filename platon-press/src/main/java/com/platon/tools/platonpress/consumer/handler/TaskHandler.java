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
import com.platon.tools.platonpress.exception.LimitException;
import com.platon.tools.platonpress.manager.CredentialsManager;
import com.platon.tools.platonpress.manager.LimitManager;
import com.platon.tools.platonpress.manager.NonceManager;
import com.platon.tools.platonpress.manager.ToAddressManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.WasmFunctionEncoder;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.WasmFunction;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Hash;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class TaskHandler implements WorkHandler<ObjectEvent<TxEvent>> {

    @Autowired
    private ToAddressManager toAddressManager;
    @Autowired
    private CredentialsManager credentialsManager;
    @Autowired
    private NonceManager nonceManager;
    @Autowired
    private PlatOnClient platOnClient;
    @Autowired
    private PressProperties pressProperties;
    @Autowired
    private LimitManager limitManager;

    @Override
    public void onEvent(ObjectEvent<TxEvent> event) throws Exception {
        Credentials credentials = null;
        BigInteger nonce;
        String to;
        String txHashLocal;
        ResultEvent resultEvent = new ResultEvent();
        resultEvent.setBegin(new Date());
        try {

            TxEvent txEvent = event.getEvent();
            resultEvent.setEvent(txEvent);

            //5. 限流
            limitManager.isAllowed();

            //1. 获得Credentials
            credentials = credentialsManager.borrow();
            //2. 获得Credentials对应的nonce
            nonce = nonceManager.getNonce(credentials.getAddress());
            //3. 获得to地址
            to = toAddressManager.getAddress(txEvent);
            //4. 构造交易对象
            RawTransaction rawTransaction = createTransaction(txEvent, credentials, to, nonce);

            //6. 签名
            String signedData = signTransaction(rawTransaction,credentials);
            //7. 提交
            txHashLocal = Hash.sha3(signedData);
            resultEvent.setTxHash(txHashLocal);
            String txHashRemote = platOnClient.platonSendRawTransaction(signedData);
            if (!txHashRemote.equals(txHashLocal)) {
                throw new RuntimeException("交易hash不一致！");
            }
            nonceManager.incrementNonce(credentials.getAddress());
            resultEvent.setCommitOk(true);
            //8. 查询回执
            if(txEvent.isNeedReceipt()){
                TransactionReceipt transactionReceipt = checkTransaction(txHashRemote);
                if(transactionReceipt.isStatusOK()){
                    resultEvent.setReceiptOk(true);
                }else{
                    resultEvent.setReceiptOk(false);
                }
            }
        } catch (LimitException e){
            resultEvent.setMsg(e.getMessage());
            TimeUnit.MILLISECONDS.sleep(pressProperties.getOnMaxPendingTxSizeSleep());
        } catch (Exception e){
            resultEvent.setMsg(e.getMessage());

        } finally {
            if(pressProperties.getConsumerThreadSleepDuration() > 0){
                TimeUnit.MILLISECONDS.sleep(pressProperties.getConsumerThreadSleepDuration());
            }
            resultEvent.setEnd(new Date());
            if(credentials != null ){
                credentialsManager.yet(credentials);
            }
            log.info(resultEvent.toString());
        }
    }

    private TransactionReceipt checkTransaction(String txHash) {

        int attempts = pressProperties.getReceiptAttempts();
        long sleepDuration = pressProperties.getReceiptSleepDuration();

        Optional<TransactionReceipt> receiptOptional = platOnClient.platonGetTransactionReceipt(txHash);
        for (int i = 0; i < attempts; i++) {
            if (!receiptOptional.isPresent()) {
                try {
                    TimeUnit.MILLISECONDS.sleep(sleepDuration);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
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


    private String signTransaction(RawTransaction rawTransaction,  Credentials credentials){
        byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, pressProperties.getChainId(), credentials);
        return Numeric.toHexString(signedMessage);
    }

    private RawTransaction createTransaction(TxEvent txEvent, Credentials credentials, String to, BigInteger nonce){

        BigInteger amount = BigInteger.ZERO;
        if(txEvent instanceof TranferTxEvent){
            amount = pressProperties.getTranferValue();
        }
        String data = "";
        if(txEvent instanceof WasmContractTxEvent){
            WasmFunction wasmFunction = new WasmFunction("record", Arrays.asList(pressProperties.getNodePublicKey()), Void.class);
            data = WasmFunctionEncoder.encode(wasmFunction);
        }
        if(txEvent instanceof EvmContractTxEvent){
            Function evmFunction = new Function("record", Arrays.<Type>asList(new Utf8String(pressProperties.getNodePublicKey())), Collections.<TypeReference<?>>emptyList());
            data = FunctionEncoder.encode(evmFunction);
        }

        BigInteger gasPrice = txEvent.getGasPrice();
        BigInteger gasLimit = txEvent.getGasLimit();
        if(txEvent.isEstimateGas()){
            gasLimit = platOnClient.platonEstimateGas(credentials.getAddress(), to, data).add(txEvent.getGasInsuranceValue());
        }

        return RawTransaction.createTransaction(nonce, gasPrice, gasLimit, to, amount, data);
    }
}
