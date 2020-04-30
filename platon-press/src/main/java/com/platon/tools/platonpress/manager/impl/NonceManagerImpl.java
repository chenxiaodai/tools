package com.platon.tools.platonpress.manager.impl;

import com.platon.tools.platonpress.client.PlatOnClient;
import com.platon.tools.platonpress.config.PressProperties;
import com.platon.tools.platonpress.manager.CredentialsManager;
import com.platon.tools.platonpress.manager.NonceManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class NonceManagerImpl implements NonceManager {

    @Autowired
    private PressProperties pressProperties;
    @Autowired
    private PlatOnClient platOnClient;
    @Autowired
    private CredentialsManager credentialsManager;

    private Map<String, AtomicLong> localNonceMap = Collections.synchronizedMap(new HashMap<>());

    @PostConstruct
    public void init(){
        if(pressProperties.isNonceAddInLocal()){
            credentialsManager.stream().forEach( credentials -> {
                String address = credentials.getAddress();
                BigInteger nonce = platOnClient.platonGetTransactionCount(address);
                localNonceMap.put(address, new AtomicLong(nonce.longValue()));
            });
        }
    }

    @Override
    public BigInteger getNonce(String address) {
        BigInteger result;
        if(pressProperties.isNonceAddInLocal()){
            result = BigInteger.valueOf(localNonceMap.get(address).get());
        }else {
            result = platOnClient.platonGetTransactionCount(address);
        }
        return result;
    }

    @Override
    public void resetNonce(String address) {
        if(pressProperties.isNonceAddInLocal()){
            BigInteger nonce = platOnClient.platonGetTransactionCount(address);
            localNonceMap.put(address, new AtomicLong(nonce.longValue()));
        }
    }

    @Override
    public void incrementNonce(String address) {
        if(pressProperties.isNonceAddInLocal()){
            localNonceMap.get(address).incrementAndGet();
        }
    }
}
