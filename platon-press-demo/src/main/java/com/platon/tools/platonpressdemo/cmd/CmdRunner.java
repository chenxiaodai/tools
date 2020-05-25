package com.platon.tools.platonpressdemo.cmd;

import com.platon.tools.platonpressdemo.enums.TxTypeEnum;
import com.platon.tools.platonpressdemo.client.PlatOnClient;
import com.platon.tools.platonpressdemo.config.PressDemoProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.Range;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.WasmFunctionEncoder;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.WasmFunction;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.crypto.WalletUtils;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Component
public class CmdRunner implements CommandLineRunner {

    @Autowired
    private PressDemoProperties pressProperties;
    @Autowired
    private PlatOnClient platOnClient;

    @Override
    public void run(String... line)  {
        try {
            // 加载钱包
            List<Credentials> credentialsList = FileUtils.readLines(pressProperties.getKeysFile(), StandardCharsets.UTF_8)
                    .parallelStream()
                    .skip(pressProperties.getKeyIndex() + 1)
                    .limit(pressProperties.getKeySize())
                    .map(keyStr ->{
                        String key = keyStr.substring(keyStr.indexOf(",")+1);
                        return Credentials.create(Numeric.prependHexPrefix(key));
                    })
                    .collect(Collectors.toList());

            // to地址
            List<String> toAddress =  FileUtils.readLines(pressProperties.getTranferToAddrsFile(), StandardCharsets.UTF_8)
                    .stream()
                    .filter(address -> WalletUtils.isValidAddress(address))
                    .map(address -> Numeric.prependHexPrefix(address))
                    .collect(Collectors.toList());

            //范围控制
            final long totalRate = pressProperties.getTxRate().stream().mapToLong(x -> x).sum();
            final Range<Long> tranferRateRange = createRange(TxTypeEnum.TRANFER);
            final Range<Long> evmRateRange = createRange(TxTypeEnum.EVM);
            final Range<Long> wasmRateRange = createRange(TxTypeEnum.WASM);

            // 循环发送
            int fSize =  credentialsList.size();
            int tSize =  toAddress.size();
            Credentials from;
            String to;
            TxTypeEnum txTypeEnum;
            long addressIndex = 0;
            for (long i = 0; i < Long.MAX_VALUE ; i++) {
                txTypeEnum = getTxType(i % totalRate, tranferRateRange, evmRateRange, wasmRateRange);
                from = credentialsList.get((int)i%fSize);
                if(txTypeEnum == TxTypeEnum.WASM){
                    to = pressProperties.getWasmAddr();
                } else if(txTypeEnum == TxTypeEnum.EVM){
                    to = pressProperties.getEvmAddr();
                } else{
                    to = toAddress.get((int)addressIndex++%tSize);
                }

                try{
                    press(from,to,txTypeEnum);
                }catch (Exception e){
                    log.error(e.getMessage());
                }
            }

        } catch (Exception e) {
            throw  new RuntimeException(e);
        }
    }


    private void press(Credentials from, String to, TxTypeEnum txTypeEnum) throws Exception{
        Date begin = new Date();
        BigInteger nonce = platOnClient.platonGetTransactionCount(from.getAddress());
        Date nonceEnd = new Date();
        RawTransaction rawTransaction = createTransaction(to, nonce, txTypeEnum);
        String signedData = signTransaction(rawTransaction,from);
        String txHashRemote = platOnClient.platonSendRawTransaction(signedData);
        log.info("txType ={}  hash = {}  nonceTime = {} ms   time = {} ms", txTypeEnum ,txHashRemote, (nonceEnd.getTime() - begin.getTime()),  (new Date().getTime()- begin.getTime()));
        if(pressProperties.getConsumerThreadSleepDuration() > 0){
            TimeUnit.MILLISECONDS.sleep(pressProperties.getConsumerThreadSleepDuration());
        }
    }

    private RawTransaction createTransaction(String to, BigInteger nonce, TxTypeEnum txTypeEnum){
        BigInteger gasPrice;
        BigInteger gasLimit;
        BigInteger amount;
        String data;
        if(txTypeEnum == TxTypeEnum.WASM){
            gasPrice = pressProperties.getWasmGasPrice();
            gasLimit = pressProperties.getWasmGasLimit();
            amount = BigInteger.ZERO;
            WasmFunction wasmFunction = new WasmFunction("record", Arrays.asList(pressProperties.getNodePublicKey()), Void.class);
            data = WasmFunctionEncoder.encode(wasmFunction);
        }else if(txTypeEnum == TxTypeEnum.EVM){
            gasPrice = pressProperties.getEvmGasPrice();
            gasLimit = pressProperties.getEvmGasLimit();
            amount = BigInteger.ZERO;
            Function evmFunction = new Function("record", Arrays.<Type>asList(new Utf8String(pressProperties.getNodePublicKey())), Collections.<TypeReference<?>>emptyList());
            data = FunctionEncoder.encode(evmFunction);
        }else {
            gasPrice = pressProperties.getTranferGasPrice();
            gasLimit = pressProperties.getTranferGasLimit();
            amount = pressProperties.getTranferValue();
            data = "";
        }
        return RawTransaction.createTransaction(nonce, gasPrice, gasLimit, to, amount, data);
    }

    private String signTransaction(RawTransaction rawTransaction,  Credentials credentials){
        byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, pressProperties.getChainId(), credentials);
        return Numeric.toHexString(signedMessage);
    }

    private Range<Long> createRange(TxTypeEnum txTypeEnum) {
        if (pressProperties.getTxType().contains(txTypeEnum)) {
            int index = pressProperties.getTxType().indexOf(txTypeEnum);
            long start = pressProperties.getTxRate().stream().mapToLong(x -> x).limit(index).sum();
            long end = start + pressProperties.getTxRate().get(index) - 1;
            return Range.between(start, end);
        } else {
            return Range.between(-1L, -1L);
        }
    }

    private TxTypeEnum getTxType(long index, Range<Long> tranferRateRange, Range<Long> evmRateRange, Range<Long> wasmRateRange){
        if(tranferRateRange.contains(index)){
            return TxTypeEnum.TRANFER;
        }else if(evmRateRange.contains(index)){
            return TxTypeEnum.EVM;
        }else if(wasmRateRange.contains(index)){
            return TxTypeEnum.WASM;
        }else {
            throw new RuntimeException("tx-type or tx-rate config error!");
        }
    }
}
