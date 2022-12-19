package com.lckp.service.facade;

import com.lckp.model.TMDBKeyConfig;
import com.lckp.param.TMDBApiKeyQueryParam;
import com.lckp.param.TMDBApiKeyModifyParam;

/*
@author manduted
@date 2022-09-09
*/

public interface ITMDBConfigService {

    TMDBKeyConfig query(TMDBApiKeyQueryParam param);

    int modify(TMDBApiKeyModifyParam param);
}
