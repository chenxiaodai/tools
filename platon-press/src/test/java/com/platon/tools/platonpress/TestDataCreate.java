package com.platon.tools.platonpress;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.platon.crypto.Credentials;
import com.platon.crypto.ECKeyPair;
import com.platon.crypto.Keys;
import com.platon.crypto.WalletUtils;
import com.platon.parameters.NetworkParameters;
import com.platon.protocol.Web3j;
import com.platon.protocol.core.DefaultBlockParameterNumber;
import com.platon.protocol.core.methods.response.PlatonBlock;
import com.platon.protocol.http.HttpService;
import com.platon.tx.Transfer;
import com.platon.utils.Convert;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

@Slf4j
public class TestDataCreate {

    private static Web3j web3j =  Web3j.build(new HttpService("http://192.168.9.221:6789"));
    private static long chainId = 100;
    private static String hrp = "lat";
    private static Credentials superCredentials;
    static {
        NetworkParameters.init(chainId,hrp);
        superCredentials = Credentials.create("0x6905e13456332c750ee490d780b94a5b4038fcf61f6de44c0f8424aa89cbc300");
    }

    private static File keystoreDir = new File("../platon-press-config/keyStores");
    private static File keysDir = new File("../platon-press-config/node");
    private static File jsonkeysDir = new File("../platon-press-config/node-json");
    private static File allocFile = new File("../platon-press-config/node.alloc");
    private static File toAddressFile = new File("../platon-press-config/to-address.txt");
    private static String passwd = "88888888";
    private static String template = "\"{address}\": {\"balance\": \"0x200000000000000000000000000000000000000000000000000000000000\"},";



    private static String gasLimit = "4712388";
    private static String gasPrice = "500000000000";

    /**
     * 创建测试钱包
     */
    @Test
    public void createKeys() throws Exception{
        log.info("createKeys start!");
        long begin = System.currentTimeMillis();
        int size = 10000;
        int nodeSize = 1;
        IntStream.range(1, nodeSize+1)
                .parallel()
                .forEach(i -> {
                    try {
                        createKeys(i,size);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

        log.info("createKeys finish! nodeSize={}  size={}  time={}s", nodeSize, size, (System.currentTimeMillis()-begin)/1000);
    }

    private void createKeys(int id, int size) throws IOException {
        File saveFile = FileUtils.getFile(keysDir,"node-"+id+".csv");

        List<String> keysList = IntStream.range(0,size).parallel()
                .mapToObj(i -> {
                    ECKeyPair ecKeyPair = null;
                    try {
                        ecKeyPair = Keys.createEcKeyPair();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    Credentials credentials = Credentials.create(ecKeyPair);
                    return new StringBuilder().append(credentials.getAddress()).append(",").append(ecKeyPair.getPrivateKey().toString(16)).toString();
                }).collect(Collectors.toList());
        List<String> head = new ArrayList<>();
        head.add(new StringBuilder().append("address").append(",").append("private").toString());
        FileUtils.deleteQuietly(saveFile);
        FileUtils.writeLines(saveFile, head, true);
        FileUtils.writeLines(saveFile, keysList, true);
    }

    @Test
    public void createAllocItem(){
        log.info("createAllocItem start!");
        long begin = System.currentTimeMillis();
        FileUtils.deleteQuietly(allocFile);
        FileUtils.listFiles(keysDir,new String[] {"csv"},false)
                .stream()
                .forEach(file -> {
                    try {
                        List<String> allocItemList = FileUtils.readLines(file, StandardCharsets.UTF_8)
                                .parallelStream()
                                .skip(1)
                                .map(keyStr ->{
                                    return template.replace("{address}",keyStr.substring(0, keyStr.indexOf(",")));
                                })
                                .collect(Collectors.toList());
                        FileUtils.writeLines(allocFile,allocItemList,true);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

        log.info("createAllocItem finish! time={}s", (System.currentTimeMillis()-begin)/1000);
    }

    @Test
    public void createToAddress() throws IOException{
        log.info("createToAddress start!");
        long begin = System.currentTimeMillis();
        FileUtils.deleteQuietly(toAddressFile);

        File keyFiles = FileUtils.getFile(keysDir,"node-1000.csv");

        List<String> addressList = FileUtils.readLines(keyFiles, StandardCharsets.UTF_8)
                .parallelStream()
                .skip(1)
                .map(keyStr ->{
                    return keyStr.substring(0, keyStr.indexOf(","));
                })
                .collect(Collectors.toList());
        FileUtils.writeLines(toAddressFile,addressList,false);

        log.info("createToAddress finish! time={}s", (System.currentTimeMillis()-begin)/1000);
    }


    @Test
    public void createJsonKey(){
        log.info("createJsonKey start!");
        long begin = System.currentTimeMillis();
        FileUtils.deleteQuietly(jsonkeysDir);
        FileUtils.listFiles(keysDir,new String[] {"csv"},false)
                .stream()
                .forEach(file -> {
                    try {
                        createKeysJson(file,  FileUtils.getFile(jsonkeysDir,file.getName().replace(".csv",".json")));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

        log.info("createJsonKey finish! time={}s", (System.currentTimeMillis()-begin)/1000);
    }


    private void createKeysJson(File src, File desc) throws IOException {
        JSONArray array = new JSONArray();
        FileUtils.readLines(src, StandardCharsets.UTF_8)
                .stream()
                .skip(1)
                .forEach(keyStr ->{
                    String[] items = keyStr.split(",");
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("private_key",items[1]);
                    jsonObject.put("address",items[0]);
                    array.add(jsonObject);
                });

        FileUtils.write(desc, array.toString(), StandardCharsets.UTF_8);
    }



    @Test
    public void checkBlockGasLimit(){
        LongStream.range(90000L, 90200L).forEach(index ->{
            BigInteger parentBn = BigInteger.valueOf(index);
            BigInteger curBn = BigInteger.valueOf(index+1);
            try {
                PlatonBlock.Block parent = web3j.platonGetBlockByNumber(new DefaultBlockParameterNumber(parentBn),false).send().getBlock();
                PlatonBlock.Block cur = web3j.platonGetBlockByNumber(new DefaultBlockParameterNumber(curBn),false).send().getBlock();
                long curLocal = calculationBlockGasLimit(parent.getGasLimit().longValue(), parent.getGasUsed().longValue());
                log.info("cur = {}  curLocal = {}",cur.getGasLimit(), curLocal);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * 预估gaslimit
     */
    @Test
    public void predictionBlockGasLimit(){
        //gasUse占gasLimit的比例, 必须大于 2/3
        double rate = 0.7;
        long curBlock = 470990L;
        long curGaslimit = 4712388L;
        long curGasUsed = (long)Math.floor(curGaslimit * rate);

        //目标信息
        long targetGaslimit = 89670560L;
        long targetBlock = curBlock;

        while (curGaslimit < targetGaslimit){
            curGaslimit = calculationBlockGasLimit( curGaslimit, curGasUsed);
            curGasUsed = (long)Math.floor(curGaslimit * rate);
            targetBlock++;
        }

       log.info("curBlock = {}, targetBlock = {}, diff = {} ",curBlock, targetBlock, targetBlock - curBlock);
    }

    private long calculationBlockGasLimit(long parentGaslimit, long parentGasUsed){
        long limit = 0;
        long gasLimitBoundDivisor = 256L;
        long gasFloor  = 4712388L;  //默认值
        long gasCeil = 100800000L;  //默认值：治理参数可调整‬
        long contrib = (parentGasUsed + parentGasUsed/2) / gasLimitBoundDivisor;
        long decay = parentGaslimit/gasLimitBoundDivisor - 1;

        limit = parentGaslimit- decay + contrib;

        if (limit < gasFloor) {
            limit = parentGaslimit + decay;
            if (limit > gasFloor) {
                limit = gasFloor;
            }
        } else if (limit > gasCeil) {
            limit = parentGaslimit - decay;
            if (limit < gasCeil) {
                limit = gasCeil;
            }
        }
        return limit;
    }


    /**
     * 给测试钱包转账
     */
    @Test
    public void transfer(){
        long begin = System.currentTimeMillis();
        FileUtils.listFiles(keystoreDir,new String[] {"json"},false).stream()
                .map(file -> {
                    try {
                        return Optional.of(WalletUtils.loadCredentials(passwd, file));
                    } catch (Exception e) {
                        log.warn("load wallet file error ! file = {}",file.getAbsolutePath(),e);
                        return Optional.empty();
                    }
                })
                .filter(optional -> optional.isPresent())
                .forEach(optional -> {
                    Credentials credentials = (Credentials) optional.get();
                    String address = credentials.getAddress();
                    try {
                        Transfer.sendFunds(web3j, superCredentials, address, new BigDecimal("1000000"), Convert.Unit.KPVON).send();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
        log.info("transfer finish!  time={}s",(System.currentTimeMillis()-begin)/1000);
    }

}
