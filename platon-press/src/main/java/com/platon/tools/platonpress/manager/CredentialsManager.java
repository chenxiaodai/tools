package com.platon.tools.platonpress.manager;

import com.platon.crypto.Credentials;

import java.util.stream.Stream;

public interface CredentialsManager {

    Credentials borrow();
    void yet(Credentials credentials);
    Stream<Credentials> stream();
}
