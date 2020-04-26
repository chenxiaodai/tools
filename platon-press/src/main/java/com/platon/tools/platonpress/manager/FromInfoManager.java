package com.platon.tools.platonpress.manager;

import com.platon.tools.platonpress.manager.dto.FromInfo;

public interface FromInfoManager {

    FromInfo borrow();
    void yet(FromInfo credentials);
}
