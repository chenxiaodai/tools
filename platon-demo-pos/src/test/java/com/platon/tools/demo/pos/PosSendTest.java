package com.platon.tools.demo.pos;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.web3j.crypto.Hash;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class PosSendTest {

    private OkHttpClient okHttpClient;
    private String url = "https://?.?.?.?:?";
    private AtomicLong atomicLong = new AtomicLong(1);
    private Map<String,String> header = new HashMap<>();


    @Before
    public void setUp() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        okHttpClient = builder.build();
    }

//    @Before
//    public void setUp() {
//
//        X509TrustManager xtm = new X509TrustManager() {
//            @Override
//            public void checkClientTrusted(X509Certificate[] chain, String authType) {
//            }
//
//            @Override
//            public void checkServerTrusted(X509Certificate[] chain, String authType) {
//            }
//
//            @Override
//            public X509Certificate[] getAcceptedIssuers() {
//                X509Certificate[] x509Certificates = new X509Certificate[0];
//                return x509Certificates;
//            }
//        };
//
//        SSLContext sslContext = null;
//        try {
//            sslContext = SSLContext.getInstance("SSL");
//            sslContext.init(null, new TrustManager[]{xtm}, new SecureRandom());
//        } catch (NoSuchAlgorithmException e) {
//            e.printStackTrace();
//        } catch (KeyManagementException e) {
//            e.printStackTrace();
//        }
//        HostnameVerifier DO_NOT_VERIFY = new HostnameVerifier() {
//            @Override
//            public boolean verify(String hostname, SSLSession session) {
//                return true;
//            }
//        };
//
//
//        OkHttpClient.Builder builder = new OkHttpClient.Builder();
//        builder.sslSocketFactory(sslContext.getSocketFactory(),xtm);
//        builder.hostnameVerifier((hostName,session) ->{
//            return  true;
//        });
//
//        header.put("Authorization", okhttp3.Credentials.basic("??????","??????"));
//        okHttpClient = builder.build();
//    }

    @Test
    public void send() throws IOException, InterruptedException {
        //加载签名文件
        String raw = FileUtils.readFileToString(FileUtils.getFile(System.getProperty("user.dir"),"signTx.json"),StandardCharsets.UTF_8);

        //解析待发送的数组
        List<SignedItem> signedItemList = JSON.parseArray(raw,SignedItem.class);

        //排序nonce小的要先发送
        signedItemList.sort((x1, x2)->{
            return  (int)(Long.valueOf(x1.getNonce()) - Long.valueOf(x2.getNonce()));
        });

        //发送交易,一笔成功后才可以继续发下一笔。否则后面的都会失败
        for (SignedItem signedItem : signedItemList) {
            sendToNode(signedItem);
        }
    }

    private void sendToNode(SignedItem signedItem) throws IOException, InterruptedException {

        //计算本地hash
        String  localHash = Hash.sha3(signedItem.getSign());

        //发送交易
        String hash = sendTx(signedItem.getSign());
        System.out.println("交易已经提交 nonce = " + signedItem.getNonce()+"  hash = " + hash);

        //比对hash
        if(!localHash.equals(hash)){
            throw new RuntimeException("交易已经提交返回hash本地不一致 ！ localHash="+ localHash + " hash = " + hash);
        }

        //查询交易结果
        int sleepTime = 2;
        int retriesNumber = 300;
        while (true){
            TimeUnit.SECONDS.sleep(sleepTime);
            if(queryTx(hash)){
                System.out.println("交易已经上链 nonce = " + signedItem.getNonce()+"  hash = " + hash);
                break;
            }
            if(--retriesNumber <= 0){
                throw new RuntimeException("查询交易超时 nonce = "+ signedItem.getNonce() +"hash = " + hash);
            }
        }


    }

    /**
     * 发送交易
     * @param sign
     * @return
     */
    private String sendTx(String sign) throws IOException {
        //发送交易    curl -X POST --data '{"jsonrpc":"2.0","method":"platon_sendRawTransaction","params":["0xf86e81c88502540be40082520894912eea1aa4ad08ddf8e5d794d93e5294abcc2256880de0b6b3a76400008081eea0f82c775b2106110de5e60e50f1b17294f2e287dc0d152c79c2ebc9547a5ebdd9a03a591bab26402d4e6b65be1f85bb4532fd5d6edb2a7e70e09bec78762b547745"],"id":1}'
        JSONObject body = new JSONObject();
        body.put("jsonrpc","2.0");
        body.put("method","platon_sendRawTransaction");
        body.put("id",atomicLong.getAndIncrement());
        JSONArray paramsList = new JSONArray();
        paramsList.add(sign);
        body.put("params",paramsList);

        okhttp3.RequestBody requestBody = okhttp3.RequestBody.create(MediaType.parse("application/json; charset=utf-8"), body.toJSONString());
        okhttp3.Headers headers = okhttp3.Headers.of(header);
        okhttp3.Request httpRequest = new okhttp3.Request.Builder()
                .url(url)
                .headers(headers)
                .post(requestBody)
                .build();

        okhttp3.Response okResponse = okHttpClient.newCall(httpRequest).execute();

        //返回结果 result为交易hash { "id":1, "jsonrpc": "2.0", "result": "0xe670ec64341771606e55d6b4ca35a1a6b75ee3d5145a99d05921026d1527331"}
        //返回结果 失败 {"jsonrpc":"2.0","id":1,"error":{"code":-32000,"message":"nonce too low"}}
        String result = okResponse.body().string();
        JSONObject resultJson = JSONObject.parseObject(result);
        if(resultJson.containsKey("error")){
            throw new RuntimeException("发送失败！msg = " + resultJson.getJSONObject("error").getString("message"));
        }

        return  resultJson.getString("result");
    }

    /**
     * 查询交易回执
     * @param hash
     * @return
     */
    private boolean queryTx(String hash) throws IOException {

        //查询回执    curl -X POST --data '{"jsonrpc":"2.0","method":"platon_getTransactionReceipt","params":["0xb903239f8543d04b5dc1ba6579132b143087c68db1b2168786408fcbce568238"],"id":1}'
        JSONObject body = new JSONObject();
        body.put("jsonrpc","2.0");
        body.put("method","platon_getTransactionReceipt");
        body.put("id",atomicLong.getAndIncrement());
        JSONArray paramsList = new JSONArray();
        paramsList.add(hash);
        body.put("params",paramsList);

        okhttp3.RequestBody requestBody = okhttp3.RequestBody.create(MediaType.parse("application/json; charset=utf-8"), body.toJSONString());
        okhttp3.Headers headers = okhttp3.Headers.of(header);
        okhttp3.Request httpRequest = new okhttp3.Request.Builder()
                .url(url)
                .headers(headers)
                .post(requestBody)
                .build();

        okhttp3.Response okResponse = okHttpClient.newCall(httpRequest).execute();

        /**
         * 返回结果
         * {
         * "id":1,
         * "jsonrpc":"2.0",
         * "result": {          //如果为null， 代表交易没有上链
         *      transactionHash: '0xb903239f8543d04b5dc1ba6579132b143087c68db1b2168786408fcbce568238',
         *      transactionIndex:  '0x1', // 1
         *      blockNumber: '0xb', // 11
         *      blockHash: '0xc6ef2fc5426d6ad6fd9e2a26abeab0aa2411b7ab17f30a99d3cb96aed1d1055b',
         *      cumulativeGasUsed: '0x33bc', // 13244
         *      gasUsed: '0x4dc', // 1244
         *      contractAddress: '0xb60e8dd61c5d32be8058bb8eb970870f07233155', // or null, if none was created
         *      logs: [{
         *          // logs as returned by getFilterLogs, etc.
         *      }, ...],
         *      logsBloom: "0x00...0", // 256 byte bloom filter
         *      status: '0x1'   代表成功或者 status 为空
         *   }
         * }
         */
        String result = okResponse.body().string();
        JSONObject resultJson = JSONObject.parseObject(result);
        if(null == resultJson.get("result")){
            return  false;
        }

        if(resultJson.containsKey("error")){
            throw new RuntimeException("查询回执失败！msg = " + resultJson.getJSONObject("error").getString("message"));
        }

        if("0x1".equals(resultJson.getJSONObject("result").getString("status"))  || resultJson.getJSONObject("result").getString("status") == null){
            return  true;
        }else{
            throw new RuntimeException("查询回执失败！");
        }
    }

}
