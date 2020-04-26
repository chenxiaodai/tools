package com.platon.tools.platonpress.service;

public interface RecoverService {

    /**
     * 账户余额初始检查
     * @throws java.lang.Exception
     */
    void initCheck() throws java.lang.Exception;

    /**
     * 余额恢复
     * @throws java.lang.Exception
     */
    void recover() throws java.lang.Exception;

    /**
     * 对账
     * @throws java.lang.Exception
     */
    void check() throws java.lang.Exception;

}
