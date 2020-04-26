package com.platon.tools.platonpress.service.impl;

import com.platon.sdk.contracts.ppos.dto.common.ContractAddress;
import com.platon.tools.platonpress.data.RecoverRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.methods.response.Transaction;
import com.platon.tools.platonpress.client.CollectPlatOnClient;
import com.platon.tools.platonpress.config.CollectProperties;
import com.platon.tools.platonpress.entity.Recover;
import com.platon.tools.platonpress.service.CollectService;

import java.io.File;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.StreamSupport;

@Slf4j
@Service
public class CollectServiceImpl implements CollectService {

    @Autowired
    private RecoverRepository recoverRepository;
    @Autowired
    private CollectPlatOnClient client;
    @Autowired
    private CollectProperties collectProperties;

    @Override
    public void collectAddress() throws Exception {
        //确定范围
        BigInteger begin = BigInteger.ONE;
        BigInteger end = client.platonBlockNumber();
        Date now = new Date();

        //查询
        Set<Recover> recoverSet = LongStream.range(begin.longValue(),end.longValue())
                .parallel()
                .mapToObj(index -> {
                    try {
                        return client.platonGetBlockByNumber(BigInteger.valueOf(index));
                    } catch (Exception e) {
                       throw new RuntimeException(e);
                    }
                })
                .filter(block -> {
                    return block.getTransactions().size() > 0;
                })
                .flatMap(block ->{
                    return block.getTransactions().stream();
                })
                .flatMap(transactionResult ->{
                    Transaction  t = (Transaction)transactionResult.get();
                    return Arrays.asList(t.getFrom(),t.getTo()).stream();
                })
                .filter(address -> {
                    return address != null && address.length() > 5;
                })
                .collect(Collectors.toSet())
                .parallelStream()
                .map(address -> {
                    return Recover.builder().address(address).status(Recover.Status.INIT).createTime(now).updateTime(now).build();
                })
                .collect(Collectors.toSet());

        //保存入库
        recoverRepository.saveAll(recoverSet);
    }

    @Override
    public void collectBalance() throws Exception {

        Date now = new Date();

        // 加载内部地址
        final Set<String> innerAddress = new HashSet<>();
        File ignoreFile = FileUtils.getFile(System.getProperty("user.dir"), "address.ignore");
        List<String> addressList = FileUtils.readLines(ignoreFile,"UTF-8");
        innerAddress.addAll(addressList);

        // 加载内置合约地址
        final Set<String> innerContractAddress = new HashSet<>();
        innerContractAddress.add(ContractAddress.RESTRICTING_PLAN_CONTRACT_ADDRESS);
        innerContractAddress.add(ContractAddress.STAKING_CONTRACT_ADDRESS);
        innerContractAddress.add(ContractAddress.INCENTIVE_POOL_CONTRACT_ADDRESS);
        innerContractAddress.add(ContractAddress.SLASH_CONTRACT_ADDRESS);
        innerContractAddress.add(ContractAddress.PROPOSAL_CONTRACT_ADDRESS);
        innerContractAddress.add(ContractAddress.REWARD_CONTRACT_ADDRESS);

        //查询数据库中地址
        Set<Recover> recoverSet = StreamSupport.stream(recoverRepository.findAll().spliterator(), true)
                .map(item ->{
                    try {
                        String address = item.getAddress();
                        String code = client.platonGetCode(address);
                        BigInteger balance = client.platonGetBalance(address);
                        if(!"0x".equals(code)){
                            // 如果是普通合约地址
                            item.setType(Recover.Type.CONTRACT);
                        } else if(innerContractAddress.contains(address)){
                            // 如果是内置合约地址
                            item.setType(Recover.Type.CONTRACT);
                        } else if(innerAddress.contains(address)){
                            // 如果是指定需要忽略的地址
                            item.setType(Recover.Type.IGNORE);
                        } else{
                            // 正常账户
                            item.setType(Recover.Type.NORMAL);
                        }

                        item.setUpdateTime(now);
                        item.setStatus(Recover.Status.PEDDING);
                        item.setBalance(balance);

                        return item;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toSet());

        //保存入库
        recoverRepository.saveAll(recoverSet);
    }
}
