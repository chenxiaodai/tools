package com.platon.tools.platonpressdemo.client;

import com.platon.tools.platonpressdemo.config.PressDemoProperties;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.PlatonGetTransactionCount;
import org.web3j.protocol.core.methods.response.PlatonSendTransaction;
import org.web3j.protocol.http.HttpService;
import org.web3j.protocol.websocket.WebSocketClient;
import org.web3j.protocol.websocket.WebSocketService;

import javax.annotation.PostConstruct;
import java.math.BigInteger;
import java.net.ConnectException;
import java.net.URI;
import java.net.URISyntaxException;


@Slf4j
@Component
public class PlatOnClient {

    @Autowired
    private PressDemoProperties pressProperties;
    private Web3j web3j;
    private OkHttpClient okHttpClient;

    @PostConstruct
    public void init() throws URISyntaxException {
        if (pressProperties.getNodeUrl().startsWith("http")) {
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor(log::debug);
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
            builder.addInterceptor(logging);
            okHttpClient = builder.build();
            web3j = Web3j.build(new HttpService(pressProperties.getNodeUrl(), okHttpClient,false));
        }
        if (pressProperties.getNodeUrl().startsWith("ws")) {
            WebSocketClient webSocketClient = new WebSocketClient(new URI(pressProperties.getNodeUrl()));
            WebSocketService webSocketService = new WebSocketService(webSocketClient,false);
            try {
                webSocketService.connect();
                web3j = Web3j.build(webSocketService);
            } catch (ConnectException e) {
                throw new RuntimeException(e);
            }
        }
    }

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
}
