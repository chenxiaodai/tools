package com.platon.tools.platonpress.data;

import org.springframework.data.repository.CrudRepository;
import com.platon.tools.platonpress.entity.Recover;

import java.math.BigInteger;
import java.util.List;

public interface RecoverRepository extends CrudRepository<Recover, String> {

    List<Recover> findAllByBalanceGreaterThanAndTypeEqualsAndStatusEquals(BigInteger minValue, Recover.Type type, Recover.Status status);

}
