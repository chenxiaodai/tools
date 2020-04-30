package com.platon.tools.platonpress.client;

import com.platon.tools.platonpress.config.PressProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.PlatonGetTransactionCount;
import org.web3j.protocol.core.methods.response.PlatonGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.PlatonSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.protocol.websocket.WebSocketService;

import javax.annotation.PostConstruct;
import java.math.BigInteger;
import java.net.ConnectException;
import java.util.Optional;


@Slf4j
@Component
public class PlatOnClient {

    @Autowired
    private PressProperties pressProperties;
    private Web3j web3j;
    private boolean isWs;

    @PostConstruct
    public void init(){
        if (pressProperties.getNodeUrl().startsWith("http")) {
            web3j = Web3j.build(new HttpService(pressProperties.getNodeUrl()));
        }
        if (pressProperties.getNodeUrl().startsWith("ws")) {
            isWs = true;
            WebSocketService webSocketService = new WebSocketService(pressProperties.getNodeUrl(), false);
            try {
                webSocketService.connect();
                web3j = Web3j.build(webSocketService);
            } catch (ConnectException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Retryable(value = RuntimeException.class, maxAttempts = 10)
    public BigInteger platonGetTransactionCount(String address) {
        try {
            PlatonGetTransactionCount ethGetTransactionCount = web3j.platonGetTransactionCount(address, DefaultBlockParameterName.PENDING).send();
            if (ethGetTransactionCount.getTransactionCount().intValue() == 0) {
                ethGetTransactionCount = web3j.platonGetTransactionCount(address, DefaultBlockParameterName.LATEST).send();
            }
            return ethGetTransactionCount.getTransactionCount();
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    public String platonSendRawTransaction(String hexValue) {
        try {
            PlatonSendTransaction platonSendTransaction = web3j.platonSendRawTransaction(hexValue).send();
            if(platonSendTransaction.hasError()){
                throw new RuntimeException(platonSendTransaction.getError().getMessage());
            }
            return platonSendTransaction.getTransactionHash();
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    public Optional<TransactionReceipt> platonGetTransactionReceipt(String txHash) {
        try {
            PlatonGetTransactionReceipt platonSendTransaction = web3j.platonGetTransactionReceipt(txHash).send();
            if(platonSendTransaction.hasError()){
                throw new RuntimeException(platonSendTransaction.getError().getMessage());
            }
            return platonSendTransaction.getTransactionReceipt();
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }
}
