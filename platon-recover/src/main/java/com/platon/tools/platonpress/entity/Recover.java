package com.platon.tools.platonpress.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Recover implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    private String address;
    @Column(precision=50, scale=0)
    private BigInteger balance;
    private Type type;
    private String txHash;
    private Status status;
    @Column(length=1024)
    private String log;
    private Date createTime;
    private Date updateTime;

    public static enum Type {
        NORMAL, LARGE, IGNORE, CONTRACT
    }

    public static enum Status {
        INIT, PEDDING, SUCCESS, ERROR, HANG
    }
}
