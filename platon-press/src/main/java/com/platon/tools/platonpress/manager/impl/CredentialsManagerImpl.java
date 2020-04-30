package com.platon.tools.platonpress.manager.impl;

import com.platon.tools.platonpress.config.PressProperties;
import com.platon.tools.platonpress.manager.CredentialsManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;
import org.web3j.utils.Numeric;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class CredentialsManagerImpl implements CredentialsManager {
    @Autowired
    private PressProperties pressProperties;
    private Queue<Credentials> freeCredentials =  new ConcurrentLinkedQueue<>();

    @PostConstruct
    public void init() throws IOException {
        log.info("loading keystore file start!");
        long begin = System.currentTimeMillis();

        List<Credentials> credentialsListFromKeyStore;
        List<Credentials> credentialsListFromKeyFile;

        File keystoreDir = pressProperties.getKeystoreDir();
        String keystorePasswd = pressProperties.getKeystorePasswd();
        File keysFile = pressProperties.getKeysFile();

        if(null != keystoreDir && keystoreDir.exists()&&keystoreDir.isDirectory()){
            credentialsListFromKeyStore = FileUtils.listFiles(keystoreDir,new String[] {"json"},false).parallelStream()
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
                        return credentials;
                    })
                    .collect(Collectors.toList());
            freeCredentials.addAll(credentialsListFromKeyStore);
        }

        if(null != keysFile && keysFile.exists()&&keysFile.isFile()){
            credentialsListFromKeyFile = FileUtils.readLines(keysFile, StandardCharsets.UTF_8)
                    .parallelStream()
                    .skip(1)
                    .map(keyStr ->{
                        String key = keyStr.substring(keyStr.indexOf(",")+1);
                        return Credentials.create(Numeric.prependHexPrefix(key));
                    })
                    .collect(Collectors.toList());
            freeCredentials.addAll(credentialsListFromKeyFile);
        }

        if(freeCredentials.size() == 0){
            throw  new RuntimeException("no available keystore file，check keystore-dir config!");
        }
        log.info("loading keystore file end!  time={}s", (System.currentTimeMillis()-begin)/1000);
    }

    @Override
    public Credentials borrow() {
        Credentials credentials = freeCredentials.poll();
        while (credentials == null){
            log.warn("wait borrow credentials!");
            try {
                TimeUnit.MILLISECONDS.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            credentials = freeCredentials.poll();
        }
        return credentials;
    }

    @Override
    public void yet(Credentials credentials) {
        freeCredentials.offer(credentials);
    }

    @Override
    public Stream<Credentials> stream() {
        return  freeCredentials.stream();
    }

}
