package com.platon.tools.platonpress.manager.dto;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.web3j.crypto.Credentials;

import java.util.concurrent.atomic.AtomicLong;

@Data
@EqualsAndHashCode
@Builder
public class FromInfo {
    private Credentials credentials;
    private AtomicLong norce;
}
