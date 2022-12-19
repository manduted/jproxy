package com.lckp.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.lckp.constant.Menu;
import com.lckp.constant.Page;
import com.lckp.interceptor.LoginInterceptor;

import io.swagger.annotations.Api;

/**
 * @ClassName: PageController
 * @Description: 页面Controller
 * @author LuckyPuppy514
 * @date 2022-07-16 13:39:45
 *
 */
@Controller
@RequestMapping("/page")
@Api(tags = "页面")
public class PageController {
	public static final Logger LOGGER = LoggerFactory.getLogger(PageController.class);
	
	@Autowired
	private MessageSource messageSource;
	@Autowired
	private ResourceLoader resourceLoader;
	
	@Value("${project.version}")
	private String version;
	@Value("${notice.location}")
	private String noticeLocation;
	
	@GetMapping("/login")
	public String login(HttpServletRequest request, HttpServletResponse response) throws IOException {
		if(LoginInterceptor.checkLogin(request)) {
			response.sendRedirect(Page.INDEX);
		}
		return Page.LOGIN;
	}
	
	@GetMapping("/home")
	public String home(Model model) {
		model.addAttribute("PROJECT_VERSION", "v" + version);
		return Page.HOME;
	}
	
	@GetMapping("/{proxyType}-config")
	public String proxyConfig(@PathVariable(value="proxyType") String proxyType, Model model, Locale locale) {
		if(!"tmdb".equals(proxyType)){
			model.addAttribute("PROXY_CONFIG_TITLE", proxyType);
			model.addAttribute("PROXY_TYPE", proxyType);
			model.addAttribute("TIPS_HREF",tips(locale));
			return "page/proxy-config";
		}
		else{
			model.addAttribute("PROXY_CONFIG_TITLE", "TMDB");
            model.addAttribute("PROXY_TYPE", "TMDB");
			model.addAttribute("TIPS_HREF",tips(locale));
            return "page/tmdb-config";
		}
	}

	private String  tips(Locale locale) {
		if ("en_US".equals(locale.toString())) {
			return "https://github.com/LuckyPuppy514/jproxy/blob/main/README.md#4-sonarr-setting";
		} else {
			return "https://github.com/LuckyPuppy514/jproxy/blob/main/README.zh_CN.md#4-sonarr-%E9%85%8D%E7%BD%AE";
		}
	}
	
	@GetMapping("/{pageName}")
	public String pageName(@PathVariable(value="pageName") String pageName) {
		return "page/" + pageName;
	}
	
	@GetMapping("/menu")
	@ResponseBody
	public Object menu(Model model, Locale locale) throws IOException {
		Resource resource = resourceLoader.getResource(Menu.PATH);
		BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()));
		String line = null;
		StringBuffer buffer = new StringBuffer();
		while((line = reader.readLine()) != null) {
			buffer.append(line);
		}
		reader.close();
		
		JSONObject menuJson = JSON.parseObject(buffer.toString());
		JSONObject homeInfo = menuJson.getJSONObject(Menu.HOME_INFO);
		String title = homeInfo.getString(Menu.TITLE);
		title = messageSource.getMessage(title, null, locale);
		homeInfo.put(Menu.TITLE, title);
		
		JSONArray menuInfos = menuJson.getJSONArray(Menu.MENU_INFO);
		for (Object menuInfoObject : menuInfos) {
			JSONObject menuInfoJson = (JSONObject) menuInfoObject;
			title = menuInfoJson.getString(Menu.TITLE);
			title = messageSource.getMessage(title, null, locale);
			menuInfoJson.put(Menu.TITLE, title);
			
			JSONArray childs = menuInfoJson.getJSONArray(Menu.CHILD);
			//添加判断，可增加单独父菜单@manduted
			if (childs != null && childs.size() > 0)
			{
				for (Object childObject : childs) {
					JSONObject childJson = (JSONObject) childObject;
					title = childJson.getString(Menu.TITLE);
					title = messageSource.getMessage(title, null, locale);
					childJson.put(Menu.TITLE, title);
				}
			}
		}

		return menuJson;
	}
	
	@GetMapping("/notice")
	@ResponseBody
	public Object notice(Model model, Locale locale) throws IOException {
		LOGGER.info("noticeLocation: {}", noticeLocation);
		if (!noticeLocation.startsWith("classpath")) {
			try {
				RestTemplate restTemplate = new RestTemplate();
				ResponseEntity<String> response = restTemplate.getForEntity(noticeLocation, String.class);
				if (response.getStatusCode() == HttpStatus.OK) {
					return response.getBody();
				}
				LOGGER.error("notice request fail: ", JSON.toJSONString(response));
			} catch (Exception e) {
				LOGGER.error("notice request fail: ", e);
			}
		}
		
		Resource resource = resourceLoader.getResource("classpath:json/notice.json");
		BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()));
		String line = null;
		StringBuffer buffer = new StringBuffer();
		while((line = reader.readLine()) != null) {
			buffer.append(line);
		}
		reader.close();
		return buffer.toString();
	}
}
