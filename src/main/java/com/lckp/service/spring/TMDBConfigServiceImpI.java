package com.lckp.service.spring;

import com.lckp.mapper.ITMDBConfigMapper;
import com.lckp.service.facade.ITMDBConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.lckp.model.TMDBKeyConfig;
import com.lckp.param.TMDBApiKeyQueryParam;
import com.lckp.param.TMDBApiKeyModifyParam;
/*
@author manduted
@date 2022-09-09
*/

@Service("tmdbConfigMapper")
public class TMDBConfigServiceImpI implements ITMDBConfigService {

    @Autowired
    private ITMDBConfigMapper tmdbConfigMapper;

    @Override
    public TMDBKeyConfig query(TMDBApiKeyQueryParam param){
        return tmdbConfigMapper.query(param);
    }

    @Override
    public int modify(TMDBApiKeyModifyParam param){
        return tmdbConfigMapper.modify(param);
    }
}
