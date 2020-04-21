package xyz.chendai.tools.platonrecover.data;

import org.springframework.data.repository.CrudRepository;
import xyz.chendai.tools.platonrecover.entity.Recover;

import java.math.BigInteger;
import java.util.List;

public interface RecoverRepository extends CrudRepository<Recover, String> {

    List<Recover> findAllByBalanceGreaterThanAndTypeEqualsAndStatusEquals(BigInteger minValue, Recover.Type type, Recover.Status status);

}
