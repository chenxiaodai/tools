package com.platon.tools.platonpress.service;


public interface CollectService {

    /**
     * 采集地址
     * @throws Exception
     */
    void collectAddress() throws Exception;

    /**
     * 采集余额
     * @throws Exception
     */
    void collectBalance() throws Exception;
}

