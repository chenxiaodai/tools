package com.platon.tools.platonpressdemo.config;

import lombok.Data;
import com.platon.tools.platonpressdemo.enums.TxTypeEnum;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.File;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

@Component
@ConfigurationProperties(prefix="press")
@Data
public class PressDemoProperties {

    private List<TxTypeEnum> txType = Arrays.asList(TxTypeEnum.TRANFER, TxTypeEnum.WASM, TxTypeEnum.EVM);
    private List<Integer> txRate = Arrays.asList(5,3,2);
    private String nodeUrl;
    private String nodePublicKey;
    private long  chainId = 102;

    private File keysFile = new File("./platon-press-config/keys.csv");
    private int keyIndex = 0;
    private int keySize = 10000;

    private BigInteger tranferGasPrice = BigInteger.valueOf(10000000000L);
    private BigInteger tranferGasLimit = BigInteger.valueOf(21000L);
    private BigInteger tranferValue = BigInteger.ONE;
    private File tranferToAddrsFile = new File("./platon-press-config/to-address.txt");

    private BigInteger evmGasPrice = tranferGasPrice;
    private BigInteger evmGasLimit = BigInteger.valueOf(300000L);
    private String evmAddr = "0x75832e679fed57a33a43639fcf02adb7b717e9f5";

    private BigInteger wasmGasPrice = tranferGasPrice;
    private BigInteger wasmGasLimit = BigInteger.valueOf(3000000L);
    private String wasmAddr = "0xdef88f99fe516c158f390da5c725a88180e93491";

    private int consumerThreadSleepDuration  = 0;
}