package com.platon.tools.platonpress.client;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.platon.tools.platonpress.config.PressProperties;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.protocol.http.HttpService;
import org.web3j.protocol.websocket.WebSocketService;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.math.BigInteger;
import java.net.ConnectException;
import java.util.HashMap;
import java.util.Optional;


@Slf4j
@Component
public class PlatOnClient {

    @Autowired
    private PressProperties pressProperties;
    private Web3j web3j;
    private OkHttpClient okHttpClient;
    private boolean isWs;

    @PostConstruct
    public void init(){
        if (pressProperties.getNodeUrl().startsWith("http")) {
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor(log::debug);
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
            builder.addInterceptor(logging);
            okHttpClient = builder.build();
            web3j = Web3j.build(new HttpService(pressProperties.getNodeUrl(), okHttpClient,false));
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


    public BigInteger platonEstimateGas(String from, String to, String data) {
        try {
            Transaction transaction = Transaction.createEthCallTransaction(from, to, data);
            PlatonEstimateGas platonEstimateGas = web3j.platonEstimateGas(transaction).send();
            if(platonEstimateGas.hasError()){
                throw new RuntimeException(platonEstimateGas.getError().getMessage());
            }
            return platonEstimateGas.getAmountUsed();
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    @Cacheable("platonPendingTransactionsLength")
    public int platonPendingTransactionsLength() {
        try {
            int size = queryPlatonPendingTransactionsLength();
            System.err.println("pennding1 = " + size);
            return size;
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    private int queryPlatonPendingTransactionsLength() throws IOException {
        JSONObject body = new JSONObject();
        body.put("jsonrpc","2.0");
        body.put("method","platon_pendingTransactionsLength");
        body.put("id",1);
        JSONArray paramsList = new JSONArray();
        body.put("params",paramsList);

        okhttp3.RequestBody requestBody = okhttp3.RequestBody.create(MediaType.parse("application/json; charset=utf-8"), body.toJSONString());
        okhttp3.Headers headers = okhttp3.Headers.of(new HashMap<>());
        okhttp3.Request httpRequest = new okhttp3.Request.Builder()
                .url(pressProperties.getNodeUrl())
                .headers(headers)
                .post(requestBody)
                .build();

        okhttp3.Response okResponse = okHttpClient.newCall(httpRequest).execute();

        String result = okResponse.body().string();
        JSONObject resultJson = JSONObject.parseObject(result);

        if(resultJson.containsKey("error")){
            throw new RuntimeException("platon_pendingTransactionsLength error！ msg = " + resultJson.getJSONObject("error").getString("message"));
        }

        return  resultJson.getInteger("result");
    }
}
