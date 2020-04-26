package com.platon.tools.platonpress.manager.impl;

import com.platon.tools.platonpress.client.PlatOnClient;
import com.platon.tools.platonpress.config.PressProperties;
import com.platon.tools.platonpress.manager.FromInfoManager;
import com.platon.tools.platonpress.manager.dto.FromInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;

import javax.annotation.PostConstruct;
import java.io.File;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FromInfoManagerImpl implements FromInfoManager {
    @Autowired
    private PressProperties pressProperties;
    @Autowired
    private PlatOnClient platOnClient;
    private Queue<FromInfo> freeCredentials =  new ConcurrentLinkedQueue<>();

    @PostConstruct
    public void init()  {
        List<FromInfo> credentialsList;

        File keystoreDir = pressProperties.getKeystoreDir();
        String keystorePasswd = pressProperties.getKeystorePasswd();

        if(keystoreDir.isDirectory()){
            credentialsList = FileUtils.listFiles(keystoreDir,new String[] {"json"},false).stream()
                    .map(file -> {
                        try {
                            return Optional.of(WalletUtils.loadCredentials(keystorePasswd, file));
                        } catch (Exception e) {
                            log.warn("load wallet file error ! file = {}",file.getAbsolutePath(),e);
                            return Optional.empty();
                        }
                    })
                    .filter(optional -> optional.isPresent())
                    .map(optional -> {
                        Credentials credentials = (Credentials) optional.get();
                        BigInteger nonce = platOnClient.platonGetTransactionCount(credentials.getAddress());
                        return FromInfo.builder().credentials(credentials).norce(new AtomicLong(nonce.longValue())).build();
                    })
                    .collect(Collectors.toList());
            freeCredentials.addAll(credentialsList);
        }
        if(freeCredentials.size() == 0){
            throw  new RuntimeException("no available keystore file，check keystore-dir config!");
        }
    }

    @Override
    public FromInfo borrow() {
        FromInfo fromInfo = freeCredentials.poll();
        while (fromInfo == null){
            log.warn("wait borrow credentials!");
            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            fromInfo = freeCredentials.poll();
        }
        return fromInfo;
    }

    @Override
    public void yet(FromInfo fromInfo) {
        freeCredentials.offer(fromInfo);
    }
}
