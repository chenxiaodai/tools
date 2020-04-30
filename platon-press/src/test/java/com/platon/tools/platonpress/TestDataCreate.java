package com.platon.tools.platonpress;

import com.platon.tools.platonpress.contract.evm.PressureContract;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterNumber;
import org.web3j.protocol.core.methods.response.PlatonBlock;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.Transfer;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.utils.Convert;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

@Slf4j
public class TestDataCreate {

    private static File keystoreDir = new File("../platon-press-config/keyStores");
    private static File keysDir = new File("../platon-press-config/keys");
    private static File allocFile = new File("../platon-press-config/keys.alloc");
    private static File toAddressFile = new File("../platon-press-config/to-address.txt");
    private static String passwd = "88888888";
    private static String template = "\"{address}\": {\"balance\": \"0x200000000000000000000000000000000000000000000000000000000000\"},";
    private static List<String> nodeList = Arrays.asList(
            "0x9999999999994f3d07b6cbcf3c10b6b4ccf605202868e2043b6f5db12b745df0604ef01ef4cb523adc6d9e14b83a76dd09f862e3fe77205d8ac83df707969b47",
            "0xe0b6af6cc2e10b2b74540b87098083d48343805a3ff09c655eab0b20dba2b2851aea79ee75b6e150bde58ead0be03ee4a8619ea1dfaf529cbb8ff55ca23531ed",
            "0x15245d4dceeb7552b52d70e56c53fc86aa030eab6b7b325e430179902884fca3d684b0e896ea421864a160e9c18418e4561e9a72f911e2511c29204a857de71a",
            "0xfb886b3da4cf875f7d85e820a9b39df2170fd1966ffa0ddbcd738027f6f8e0256204e4873a2569ef299b324da3d0ed1afebb160d8ff401c2f09e20fb699e4005",
            "0x77fffc999d9f9403b65009f1eb27bae65774e2d8ea36f7b20a89f82642a5067557430e6edfe5320bb81c3666a19cf4a5172d6533117d7ebcd0f2c82055499050"
            );
    private static Web3j web3j =  Web3j.build(new HttpService("http://192.168.120.145:6789"));
    private static String chainId = "103";
    private static Credentials superCredentials = Credentials.create("0xa689f0879f53710e9e0c1025af410a530d6381eebb5916773195326e123b822b");
    private static String gasLimit = "4712388";
    private static String gasPrice = "500000000000";
    private static ContractGasProvider provider = new ContractGasProvider(new BigInteger(gasPrice), new BigInteger(gasLimit));;
    private static RawTransactionManager adminTransactionManager = new RawTransactionManager(web3j, superCredentials, Long.valueOf(chainId));

    private static BigInteger beginBlock = BigInteger.valueOf(1000L);
    private static BigInteger endBlock = BigInteger.valueOf(10000000000L);
    private static String evmAddress = "0x905e9b518fbc71952145bcaa974c09168a7d7c41";
    private static String wasmAddress = "0x515d82f18f6a7d4c54702c973cb6f6deb88878d8";

    /**
     * 创建测试钱包
     */
    @Test
    public void createKeys() throws Exception{
        log.info("createKeys start!");
        long begin = System.currentTimeMillis();
        int size = 10000;
        int nodeSize = 110;
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
    public void  deployEvm() throws Exception {
        PressureContract pressureContract = PressureContract.deploy(web3j,adminTransactionManager,provider,beginBlock,endBlock).send();
        log.info("evm contract address = {}",pressureContract.getContractAddress());
    }

    @Test
    public void  deployWasm() throws Exception {
        com.platon.tools.platonpress.contract.wasm.PressureContract pressureContract = com.platon.tools.platonpress.contract.wasm.PressureContract.deploy(web3j,adminTransactionManager,provider,beginBlock,endBlock).send();
        log.info("wasm contract address = {}",pressureContract.getContractAddress());
    }

    @Test
    public void  setEvm() throws Exception {
        PressureContract pressureContract = PressureContract.load(evmAddress,web3j,adminTransactionManager,provider);
        List<String> result = nodeList
                .stream()
                .map( nodeId ->{
                    try {
                        return pressureContract.record(nodeId).send().getTransactionHash();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());

        log.info("evm set txhash = {}",result);
    }

    @Test
    public void  setWasm() throws Exception {
        com.platon.tools.platonpress.contract.wasm.PressureContract pressureContract = com.platon.tools.platonpress.contract.wasm.PressureContract.load(wasmAddress,web3j,adminTransactionManager,provider);

        List<String> result = nodeList
                .stream()
                .map( nodeId ->{
                    try {
                        return pressureContract.record(nodeId).send().getTransactionHash();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());

        log.info("wasm set txhash = {}", result);
    }

    @Test
    public void  getEvm() throws Exception {
        PressureContract pressureContract = PressureContract.load(evmAddress,web3j,adminTransactionManager,provider);

        List<String> result = nodeList
                .stream()
                .map( nodeId ->{
                    try {
                        return "nodeId = " + nodeId + " count = " + pressureContract.getValue(nodeId).send();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());

        log.info("evm get result =  {}",result);
    }

    @Test
    public void  getWasm() throws Exception {
        com.platon.tools.platonpress.contract.wasm.PressureContract pressureContract = com.platon.tools.platonpress.contract.wasm.PressureContract.load(wasmAddress,web3j,adminTransactionManager,provider);

        List<String> result = nodeList
                .stream()
                .map( nodeId ->{
                    try {
                        return "nodeId = " + nodeId + " count = " + pressureContract.getValue(nodeId).send();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());

        log.info("wasm get result =  {}",result);
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
                        Transfer.sendFunds(web3j, superCredentials, chainId, address, new BigDecimal("1000000"), Convert.Unit.LAT).send();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
        log.info("transfer finish!  time={}s",(System.currentTimeMillis()-begin)/1000);
    }

}
