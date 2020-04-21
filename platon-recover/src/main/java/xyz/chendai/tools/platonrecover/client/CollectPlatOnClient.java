package xyz.chendai.tools.platonrecover.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.PlatonBlock;
import org.web3j.protocol.http.HttpService;
import xyz.chendai.tools.platonrecover.config.CollectProperties;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;


@Component
public class CollectPlatOnClient {

    private List<Web3j> web3jList= Collections.synchronizedList(new ArrayList<>());
    private AtomicLong atomicLong = new AtomicLong();

    @Autowired
    private CollectProperties collectProperties;


    @PostConstruct
    public void init(){
        collectProperties.getUrl().forEach(address->{
            Web3j web3j = Web3j.build(new HttpService(address));
            web3jList.add(web3j);
        });
    }

    private Optional<Web3j> getWeb3j() {
        int size = web3jList.size();

        if(size == 0){
            return Optional.ofNullable(null);
        }

        long value = atomicLong.incrementAndGet();

        int index = (int)(value % size);

        return  Optional.of(web3jList.get(index));
    }

    @Retryable(value = RuntimeException.class, maxAttempts = 10)
    public BigInteger platonBlockNumber() throws Exception{
        Optional<Web3j> web3j = getWeb3j();
        if(web3j.isPresent()){
            try {
                return web3j.get().platonBlockNumber().send().getBlockNumber();
            } catch (IOException e) {
                web3jList.remove(web3j.get());
                throw new RuntimeException("");
            }
        }else{
            throw new Exception("no nodes");
        }
    }


    @Retryable(value = RuntimeException.class, maxAttempts = 10)
    public PlatonBlock.Block platonGetBlockByNumber(BigInteger bn) throws Exception{
        Optional<Web3j> web3j = getWeb3j();
        if(web3j.isPresent()){
            try {
                return web3j.get().platonGetBlockByNumber(DefaultBlockParameter.valueOf(bn),true).send().getBlock();
            } catch (IOException e) {
                web3jList.remove(web3j.get());
                throw new RuntimeException("");
            }
        }else{
            throw new Exception("no nodes");
        }
    }

    @Retryable(value = RuntimeException.class, maxAttempts = 10)
    public String platonGetCode(String address) throws  Exception{
        Optional<Web3j> web3j = getWeb3j();
        if(web3j.isPresent()){
            try {
                return web3j.get().platonGetCode(address, DefaultBlockParameterName.LATEST).send().getCode();
            } catch (IOException e) {
                web3jList.remove(web3j.get());
                throw new RuntimeException("");
            }
        }else{
            throw new Exception("no nodes");
        }
    }

    @Retryable(value = RuntimeException.class, maxAttempts = 10)
    public BigInteger platonGetBalance(String address) throws Exception {
        Optional<Web3j> web3j = getWeb3j();
        if(web3j.isPresent()){
            try {
                return web3j.get().platonGetBalance(address, DefaultBlockParameterName.LATEST).send().getBalance();
            } catch (IOException e) {
                web3jList.remove(web3j.get());
                throw new RuntimeException("");
            }
        }else{
            throw new Exception("no nodes");
        }
    }
}
