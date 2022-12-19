package com.lckp.controller;

/*
@author manduted
@date 2022-09-12
*/

import com.alibaba.fastjson.JSON;
import com.lckp.config.JProxyConfiguration;
import com.lckp.constant.Message;
import com.lckp.model.TMDBKeyConfig;
import com.lckp.param.TMDBApiKeyModifyParam;
import com.lckp.param.TMDBApiKeyQueryParam;
import com.lckp.resp.ResVo;
import com.lckp.service.facade.ITMDBConfigService;
import com.lckp.util.TmdbUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

import java.util.Locale;


@RestController
@RequestMapping("/tmdbConfig")
public class tmdbConfigController {

    @Autowired
    private ITMDBConfigService tmdbConfigService;
    @Autowired
    private MessageSource messageSource;

    private static final Logger LOGGER = LoggerFactory.getLogger(tmdbConfigController.class);

    @GetMapping("/query")
        public ResVo<TMDBKeyConfig> query(TMDBApiKeyQueryParam param) {
        LOGGER.info("TMDB - 查询：{}", JSON.toJSONString(param));
        return ResVo.success(tmdbConfigService.query(param));
    }

    @PostMapping("/modify")
    public ResVo<String> modify(TMDBApiKeyModifyParam  param, @ApiIgnore Locale locale) {
        LOGGER.info("TMDB配置 - 修改：{}", JSON.toJSONString(param));

        if (tmdbConfigService.modify(param) > 0) {
            JProxyConfiguration.keyconfig.setTmdbAPIKEY(param.getTmdbAPIKEY());
            JProxyConfiguration.keyconfig.setTmdbSTATUS(param.getTmdbSTATUS());
            JProxyConfiguration.keyconfig.setTmdbCHINESE(param.getTmdbCHINESE());
            JProxyConfiguration.keyconfig.setTmdbPROXYSTATUS(param.getTmdbPROXYSTATUS());
            JProxyConfiguration.keyconfig.setTmdbPROXYIP(param.getTmdbPROXYIP());
            JProxyConfiguration.keyconfig.setTmdbPROXYPORT(param.getTmdbPROXYPORT());
            return ResVo.success(Message.MODIFY_SUCCESS, messageSource, locale);
        }
        return ResVo.fail(Message.MODIFY_FAIL, messageSource, locale);
    }

    @GetMapping("/testproxy")
    public ResVo<String> testProxy (@ApiIgnore Locale locale){
        if (TmdbUtil.testProxy(TmdbUtil.tmdbPROXY())){
            return ResVo.success(Message.PROXY_SUCCESS, messageSource, locale);
        }
        return ResVo.fail(Message.PROXY_FAIL, messageSource, locale);
    }
}
