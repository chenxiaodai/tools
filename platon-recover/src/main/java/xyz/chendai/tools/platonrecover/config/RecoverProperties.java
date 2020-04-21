package xyz.chendai.tools.platonrecover.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix="biz.recover")
@Data
public class RecoverProperties {
    private List<String> url;
    private Long chainId;
    private String returnWalletAddress;
}
