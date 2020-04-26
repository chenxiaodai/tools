package com.platon.tools.platonpress.event;

import lombok.Data;

@Data
public class ObjectEvent<T> {
    T event;
}
