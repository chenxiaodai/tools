package com.platon.tools.platonpress.manager.impl;

import com.platon.tools.platonpress.config.PressProperties;
import com.platon.tools.platonpress.enums.TxTypeEnum;
import com.platon.tools.platonpress.event.task.EvmContractTxEvent;
import com.platon.tools.platonpress.event.task.TranferTxEvent;
import com.platon.tools.platonpress.event.task.TxEvent;
import com.platon.tools.platonpress.event.task.WasmContractTxEvent;
import com.platon.tools.platonpress.manager.ToAddressManager;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.crypto.WalletUtils;
import org.web3j.utils.Numeric;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ToAddressManagerImpl implements ToAddressManager {

    @Autowired
    private PressProperties pressProperties;
    private List<String> addressList = new ArrayList<>();
    private long index = 0;

    @PostConstruct
    public void init() throws IOException {
        List<String> tempAddressList;
        //从tranfer-to-addrs中加载地址
        tempAddressList = pressProperties.getTranferToAddrs();
        //从tranfer-to-addrs-file中加载地址
        File addressFile = pressProperties.getTranferToAddrsFile();
        if(addressFile.exists() && addressFile.isFile()){
            List<String> tempFileAddressList = FileUtils.readLines(addressFile, StandardCharsets.UTF_8);
            tempAddressList.addAll(tempFileAddressList);
        }
        //过滤无效地址
        addressList = tempAddressList
                .stream()
                .filter(address -> WalletUtils.isValidAddress(address))
                .map(address -> Numeric.prependHexPrefix(address))
                .collect(Collectors.toList());

        if(pressProperties.getTxType().contains(TxTypeEnum.TRANFER) && addressList.size() == 0){
            throw  new RuntimeException("TRANFER must to address，check tranfer-to-addrs or tranfer-to-addrs-file config!");
        }
    }

    @Override
    public String getAddress(TxEvent txEvent) {
        String address;
        if(txEvent instanceof EvmContractTxEvent){
            address = pressProperties.getEvmAddr();
        }else if(txEvent instanceof WasmContractTxEvent){
            address = pressProperties.getWasmAddr();
        }else if(txEvent instanceof TranferTxEvent){
            //不需要考虑线程安全
            address = addressList.get((int)(index++ % addressList.size()));
        }else {
            throw new RuntimeException("no support txType");
        }
        return address;
    }
}
