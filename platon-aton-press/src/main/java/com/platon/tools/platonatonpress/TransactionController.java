package com.platon.tools.platonatonpress;


import com.platon.crypto.*;
import com.platon.parameters.NetworkParameters;
import com.platon.utils.Numeric;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;


public class TransactionController {

    private final Credentials fromCredentials;
    private BigInteger gasPrice;
    private BigInteger gasLimit;

    public TransactionController(long chainId, String hrp, String fromKey, String _gasPrice, String _gasLimit) {
        NetworkParameters.init(chainId,hrp);
        fromCredentials = Credentials.create(fromKey);
        gasPrice = new BigInteger(_gasPrice);
        gasLimit = new BigInteger(_gasLimit);
    }

    public String createSignedDataTxCode0(String toAddress, String amount, long nonce) {
        RawTransaction rawTransaction = RawTransaction.createTransaction(BigInteger.valueOf(nonce), gasPrice, gasLimit, toAddress, new BigInteger(amount), "");
        byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, NetworkParameters.getChainId(), fromCredentials);
        return Numeric.toHexString(signedMessage);
    }

    public String createSigned(String signedData, String remark) {
        byte[] signedDataByte = Numeric.hexStringToByteArray(signedData);
        byte[] remarkByte = remark.getBytes(StandardCharsets.UTF_8);
        byte[] message = new byte[signedDataByte.length + remarkByte.length];
        System.arraycopy(signedDataByte, 0, message, 0, signedDataByte.length);
        System.arraycopy(remarkByte, 0, message, signedDataByte.length, remarkByte.length);
        byte[] messageHash = Hash.sha3(message);
        Sign.SignatureData signatureData = Sign.signMessage(messageHash, fromCredentials.getEcKeyPair(), false);
        byte[] signByte = SignCodeUtils.encode(signatureData);
        return Numeric.toHexString(signByte);
    }
}
