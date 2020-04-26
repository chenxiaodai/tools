package com.platon.tools.platonpress.manager.impl;

import com.platon.tools.platonpress.config.PressProperties;
import com.platon.tools.platonpress.manager.CredentialsManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CredentialsManagerImpl implements CredentialsManager {
    @Autowired
    private PressProperties pressProperties;
    private Queue<Credentials> freeCredentials =  new LinkedList<>();

    @PostConstruct
    public void init() throws IOException {
        List<Credentials> credentialsList;

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
                    .map(optional -> (Credentials)optional.get())
                    .collect(Collectors.toList());
            freeCredentials.addAll(credentialsList);
        }
        if(freeCredentials.size() == 0){
            throw  new RuntimeException("no available keystore file，check keystore-dir config!");
        }
    }

    @Override
    public Credentials borrow() {
        return null;
    }

    @Override
    public void yet(Credentials credentials) {

    }
}
