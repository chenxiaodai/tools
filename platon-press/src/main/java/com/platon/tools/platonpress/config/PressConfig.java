package com.platon.tools.platonpress.config;

import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.util.DaemonThreadFactory;
import com.platon.tools.platonpress.event.ObjectEvent;
import com.platon.tools.platonpress.event.task.TxEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PressConfig {
    @Bean
    public Disruptor<ObjectEvent<TxEvent>> serverEndpointExporter(PressProperties pressProperties) {
        Disruptor<ObjectEvent<TxEvent>> disruptor = new Disruptor<>(ObjectEvent<TxEvent>::new, pressProperties.getDisruptorBfferSize(), DaemonThreadFactory.INSTANCE);
        return  disruptor;
    }
}
