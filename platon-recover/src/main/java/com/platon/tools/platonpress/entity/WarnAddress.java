package com.platon.tools.platonpress.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.math.BigInteger;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class WarnAddress {
    private static final long serialVersionUID = 1L;
    @Id
    private String address;
    @Column(precision=50, scale=0)
    private BigInteger balance;
}
