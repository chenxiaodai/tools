package com.platon.tools.platonpress.manager;

import org.web3j.crypto.Credentials;

import java.util.stream.Stream;

public interface CredentialsManager {

    Credentials borrow();
    void yet(Credentials credentials);
    Stream<Credentials> stream();
}
