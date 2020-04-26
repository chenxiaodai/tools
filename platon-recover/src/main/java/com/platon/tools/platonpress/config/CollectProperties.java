package com.platon.tools.platonpress.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix="biz.collect")
@Data
public class CollectProperties {
    private List<String> url;
}
