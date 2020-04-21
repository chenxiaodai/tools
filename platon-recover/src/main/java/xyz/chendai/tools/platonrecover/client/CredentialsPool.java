package xyz.chendai.tools.platonrecover.client;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.web3j.crypto.Credentials;
import xyz.chendai.tools.platonrecover.config.RecoverProperties;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;

@Slf4j
@Component
public class CredentialsPool {

    @Autowired
    private RecoverProperties recoverProperties;
    private Queue<Credentials> freeCredentials =  new LinkedList<>();

    @PostConstruct
    public synchronized void init(){
        loadWallet();
    }

    /**
     * 获得一个空闲的Credentials
     * @return
     */
    public synchronized Credentials borrow(){
        return freeCredentials.poll();
    }

    /**
     * 归还
     */
    public synchronized void yet(Credentials credentials){
        freeCredentials.offer(credentials);
    }


    private void loadWallet() {
        log.info("加载钱包文件开始");
        File dir = FileUtils.getFile(new String[] { System.getProperty("user.dir"), "wallets" });

        Collection<File> fileList = FileUtils.listFiles(dir,new String[] {"json"},false);

        fileList.stream()
                .forEach(file -> {

                });

    }

    public static Credentials fileToCredentials(File file) {
        Credentials wallet = null;
        try {
            return wallet;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
