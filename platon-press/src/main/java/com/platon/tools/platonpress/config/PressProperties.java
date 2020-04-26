package com.platon.tools.platonpress.config;

import com.platon.tools.platonpress.enums.TxTypeEnum;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix="press")
@Data
public class PressProperties {
    private List<TxTypeEnum> txType;
    private List<Integer> txRate;
    private int tps = 1;
    private long totalTx = Long.MAX_VALUE;
    private String nodeUrl;
    private String nodePublicKey;
    private long  chainId;
    private File  keystoreDir;
    private String  keystorePasswd;
    private BigInteger tranferGasPrice;
    private BigInteger tranferGasLimit;
    private boolean tranferNeedReceipt;
    private BigInteger tranferValue;
    private List<String>  tranferToAddrs = new ArrayList<>();
    private File tranferToAddrsFile;
    private BigInteger evmGasPrice;
    private BigInteger evmGasLimit;
    private boolean evmNeedReceipt;
    private String  evmAddr;
    private BigInteger wasmGasPrice;
    private BigInteger wasmGasLimit;
    private boolean wasmNeedReceipt;
    private String wasmAddr;
    private int disruptorBfferSize  = 1024;
    private int disruptorConsumerNumber  = 16;
}