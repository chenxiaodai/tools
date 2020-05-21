package com.platon.tools.platonpress.config;

import com.platon.tools.platonpress.enums.TxTypeEnum;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
@ConfigurationProperties(prefix="press")
@Data
public class PressProperties {
    private List<TxTypeEnum> txType = Arrays.asList(TxTypeEnum.TRANFER, TxTypeEnum.WASM, TxTypeEnum.EVM);
    private List<Integer> txRate = Arrays.asList(5,3,2);
    private int tps = 50;
    private long totalTx = Long.MAX_VALUE;
    private String nodeUrl;
    private String nodePublicKey;
    private long  chainId = 102;
    private File  keystoreDir;
    private String  keystorePasswd;
    private File keysFile = new File("./platon-press-config/keys.csv");
    private int keyIndex = 0;
    private int keySize = 10000;

    private boolean limitMaxPendingTxSize = true;
    private int maxPendingTxSize = 3072;
    private int onMaxPendingTxSizeSleep = 5000;

    private boolean tranferEstimateGas = false;
    private BigInteger tranferGasInsuranceValue = BigInteger.valueOf(0L);
    private BigInteger tranferGasPrice = BigInteger.valueOf(10000000000L);
    private BigInteger tranferGasLimit = BigInteger.valueOf(21000L);
    private boolean tranferNeedReceipt = false;
    private BigInteger tranferValue = BigInteger.ONE;
    private List<String>  tranferToAddrs = new ArrayList<>();
    private File tranferToAddrsFile = new File("./platon-press-config/to-address.txt");

    private boolean evmEstimateGas = true;
    private BigInteger evmGasInsuranceValue = BigInteger.valueOf(0L);
    private BigInteger evmGasPrice = tranferGasPrice;
    private BigInteger evmGasLimit = BigInteger.valueOf(42017L);
    private boolean evmNeedReceipt = tranferNeedReceipt;
    private String evmAddr = "0x75832e679fed57a33a43639fcf02adb7b717e9f5";

    private boolean wasmEstimateGas = true;
    private BigInteger wasmGasInsuranceValue = BigInteger.valueOf(50000L);
    private BigInteger wasmGasPrice = tranferGasPrice;
    private BigInteger wasmGasLimit = BigInteger.valueOf(42017L);
    private boolean wasmNeedReceipt = tranferNeedReceipt;
    private String wasmAddr = "0xdef88f99fe516c158f390da5c725a88180e93491";

    /**
     * 是否本地管理自增nonce
     */
    private boolean nonceAddInLocal = false;
    /**
     * 消费队列大小
     */
    private int disruptorBfferSize  = 1024;
    /**
     * 消费线程的数据
     */
    private int disruptorConsumerNumber  = 5;
    /**
     * 获取回执的次数
     */
    private int receiptAttempts  = 3;
    /**
     * 获取回执的等待时间，单位毫秒
     */
    private int receiptSleepDuration  = 1000;
    /**
     * 消费线程sleep的时间，单位毫秒
     */
    private int consumerThreadSleepDuration  = 0;
}