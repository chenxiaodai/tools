package com.platon.tools.platonpress.manager;

import org.web3j.crypto.Credentials;

public interface CredentialsManager {

    Credentials borrow();
    void yet(Credentials credentials);
}
