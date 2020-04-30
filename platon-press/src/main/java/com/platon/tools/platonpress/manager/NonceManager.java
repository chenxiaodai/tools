package com.platon.tools.platonpress.manager;

import java.math.BigInteger;

public interface NonceManager {

    BigInteger getNonce(String address);

    void resetNonce(String address);

    void incrementNonce(String address);
}
