package com.platon.tools.demo.exchange;

import org.junit.jupiter.api.Test;


import org.web3j.crypto.*;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.DefaultBlockParameterNumber;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.protocol.exceptions.TransactionException;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.Transfer;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

public class ExchangeDemoTest {

    private ECKeyPair ecKeyPair = ECKeyPair.create(Numeric.toBigInt("0xcc7308df14ff3c45353b4c8aeb7e2046d597b1e70d28d2a8139107ccfd2caa82"));
    private long chainId = 101;
    private Web3j web3j = Web3j.build(new HttpService("http://192.168.120.150:6789"));

    /**
     * 充值流程
     * @throws Exception
     */
    @Test
    public void recharge() throws Exception{
        //开始监听的块高
        BigInteger beginBn = new BigInteger("427125");
        //监听的充币地址
        String toAddress = "0x45886ffccf2c6726f44deec15446f9a53c007848";
        long sleepDuration = 1000;

        //循环监听块高中交易
        while (true){
            PlatonBlock platonBlock  = web3j.platonGetBlockByNumber(new DefaultBlockParameterNumber(beginBn) ,true).send();
            if(platonBlock.hasError()){
                throw new RuntimeException("交易提交失败！error="+platonBlock.getError().getMessage());
            }

            PlatonBlock.Block block = platonBlock.getBlock();
            if(block == null ){
                try {
                    Thread.sleep(sleepDuration);
                    continue;
                } catch (InterruptedException e) {
                    throw new TransactionException(e);
                }
            }

            List<PlatonBlock.TransactionResult> transactionResultList = block.getTransactions();
            for (PlatonBlock.TransactionResult transactionResult: transactionResultList) {
                Transaction transaction = (Transaction) transactionResult.get();
                if(transaction.getTo().equals(toAddress)){
                    //该账户存在交易
                    String txHash = transaction.getHash();
                    String from = transaction.getFrom();
                    BigInteger value = transaction.getValue();  //单位VON.  1LAT=1000000000000000000VON
                    TransactionReceipt transactionReceipt = checkTransaction(txHash);
                    System.out.println("收到转账记录： from = " + from + " hash = " +txHash + " value = "+ value.toString() + " isOk = " + transactionReceipt.isStatusOK());
                }
            }
            beginBn = beginBn.add(BigInteger.ONE);
        }
    }



    /**
     * 提现流程
     * @throws Exception
     */
    @Test
    public void withdraw() throws Exception{
        //交易构造
        RawTransaction rawTransaction = createTransaction();
        //交易签名
        String signedData = signTransaction1(rawTransaction);
        //解析签名
        RawTransaction decodeRawTransaction = decodeTransaction1(signedData);
        //交易发送
        String txHashRemote = sendTransaction(signedData);
        String txHashLocal = Hash.sha3(signedData);
        if (!txHashRemote.equals(txHashLocal)) {
            throw new RuntimeException("交易hash不一致！");
        }
        //提现成功的判断
        TransactionReceipt transactionReceipt = checkTransaction(txHashRemote);
        if(transactionReceipt.isStatusOK()){
            System.out.println("交易成功！ txHash = " + txHashRemote);
        }else{
            System.out.println("交易失败！ txHash = " + txHashRemote);
        }

    }

    private RawTransaction createTransaction() throws Exception{
        Credentials credentials = Credentials.create(ecKeyPair);

        BigInteger gasPrice = web3j.platonGasPrice().send().getGasPrice();
        BigInteger gasLimit = Transfer.GAS_LIMIT;
        BigInteger nonce = getNonce(credentials);
        BigInteger amount = new BigInteger("1000000000000000000");
        String toAddress = "0x45886ffccf2c6726f44deec15446f9a53c007848";

        return RawTransaction.createTransaction(nonce, gasPrice, gasLimit, toAddress, amount, "");
    }

    private String signTransaction1(RawTransaction rawTransaction){
        Credentials credentials = Credentials.create(ecKeyPair);

        byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, chainId, credentials);
        return Numeric.toHexString(signedMessage);
    }

    private RawTransaction decodeTransaction1(String signedData) throws Exception{
        SignedRawTransaction transaction = (SignedRawTransaction) TransactionDecoder.decode(signedData);
        return transaction;
    }

    private String sendTransaction(String signedData) throws Exception{
        PlatonSendTransaction platonSendTransaction =  web3j.platonSendRawTransaction(signedData).send();
        if(platonSendTransaction.hasError()){
            throw new RuntimeException("交易提交失败！error="+platonSendTransaction.getError().getMessage());
        }
        return platonSendTransaction.getTransactionHash();
    }

    private TransactionReceipt checkTransaction(String txHash) throws Exception{
        int attempts = 180;
        long sleepDuration = 1000;

        Optional<TransactionReceipt> receiptOptional = sendTransactionReceiptRequest(txHash);

        for (int i = 0; i < attempts; i++) {
            if (!receiptOptional.isPresent()) {
                try {
                    Thread.sleep(sleepDuration);
                } catch (InterruptedException e) {
                    throw new TransactionException(e);
                }
                receiptOptional = sendTransactionReceiptRequest(txHash);
            } else {
                return receiptOptional.get();
            }
        }

        throw new RuntimeException("Transaction receipt was not generated after "
                + ((sleepDuration * attempts) / 1000
                + " seconds for transaction: " + txHash));
    }

    private Optional<TransactionReceipt> sendTransactionReceiptRequest(String txHash) throws Exception{
        PlatonGetTransactionReceipt receipt  = web3j.platonGetTransactionReceipt(txHash).send();
        if(receipt.hasError()){
            throw new RuntimeException("查询交易回执失败！error="+receipt.getError().getMessage());
        }

        Optional<TransactionReceipt> receiptOptional = receipt.getTransactionReceipt();
        return receiptOptional;
    }

    private BigInteger getNonce(Credentials credentials) throws IOException {
        PlatonGetTransactionCount ethGetTransactionCount = web3j.platonGetTransactionCount(
                credentials.getAddress(), DefaultBlockParameterName.PENDING).send();

        if (ethGetTransactionCount.getTransactionCount().intValue() == 0) {
            ethGetTransactionCount = web3j.platonGetTransactionCount(
                    credentials.getAddress(), DefaultBlockParameterName.LATEST).send();
        }

        return ethGetTransactionCount.getTransactionCount();
    }
}
