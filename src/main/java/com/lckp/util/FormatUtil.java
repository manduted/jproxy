/**
 * @Title: FormatUtil.java
 * @version V1.0
 */
package com.lckp.util;

import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.alibaba.fastjson.JSONObject;
import com.lckp.proxy.IndexerProxy;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lckp.constant.ExecuteRule;
import com.lckp.constant.Field;
import com.lckp.model.RuleConfig;

/**
 * @className: FormatUtil
 * @description: 格式化工具类
 * @date 2022年7月21日
 * @author LuckyPuppy514
 */
public class FormatUtil {
	private static final Logger LOGGER = LoggerFactory.getLogger(FormatUtil.class);
	private static String SearchkeyEpisode = "(?i)E\\d{2}";
	
	/**
	 * 
	 * @param searchKey
	 * @param ruleConfigList
	 * @return
	 * @description: 搜索关键字格式化
	 */
	public static String search(String searchKey, List<RuleConfig> ruleList) {
		LOGGER.debug("查询关键字 - 格式化前：{}", searchKey);
		if (StringUtils.isBlank(searchKey)) {
			return searchKey;
		}
		if (TmdbUtil.tmdbSTATUS()){
			try {
				IndexerProxy.tmdbJSONDATA = new JSONObject(TmdbUtil.tmdbName(searchKey));
				IndexerProxy.hstmdbData = true;
				searchKey = tmdbFormatsk(searchKey,IndexerProxy.tmdbJSONDATA);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		searchKey = format(searchKey, ruleList);
		LOGGER.debug("查询关键字 - 格式化后：{}", searchKey);
		return searchKey;
	}
	
	/**
	 * 
	 * @param resultXml
	 * @param ruleConfigList
	 * @return
	 * @throws DocumentException 
	 * @description: 结果 xml 格式化
	 */
	public static String result(String resultXml, List<RuleConfig> ruleList,JSONObject tmdbJSON) throws DocumentException {
		Boolean Service = TmdbUtil.tmdbSTATUS();
		JSONObject tmdb = null;
		String Episode = null;
		Document document = DocumentHelper.parseText(resultXml);
		Element root = document.getRootElement();
		Element channel = root.element(Field.RESP_CHANNEL);
		if (null == channel) {
			return resultXml;
		}
		if (tmdbJSON != null && !tmdbJSON.isEmpty() && Service) {
			tmdb = new JSONObject(tmdbRegex(tmdbJSON));
			if (tmdb.getString("type").equals("Tv")){
				Pattern r1 = Pattern.compile(SearchkeyEpisode);
				Matcher m1 = r1.matcher(tmdb.getString("year"));
				if (m1.find()){
					Episode = m1.group(0).trim();
				}
			}
		}

		for (Iterator<Element> items = channel.elementIterator(Field.RESP_ITEM); items.hasNext();) {
			Element item = items.next();
			Element titleElement = item.element(Field.RESP_TITLE);
			String title = titleElement.getText();
			if (Service){
				LOGGER.debug("TMDB - 格式化前：{}", title);
				title = tmdbFormat(titleElement.getText(), tmdb);
				LOGGER.debug("TMDB - 格式化后：{}", title);
				//Tv结果筛选，不加入非必要的结果
				if (tmdb.getString("type").equals("Tv") && Episode != null && IndexerProxy.searchName != null){
					if (!title.contains(Episode)){
						channel.remove(item);
					}
					else{
						title = format(title, ruleList);
						LOGGER.debug("标题市场规则 - 格式化后：{}", title);
						titleElement.setText(title);
					}
				}
				else{
					title = format(title, ruleList);
					LOGGER.debug("标题市场规则 - 格式化后：{}", title);
					titleElement.setText(title);
				}
			}
			else{
				title = format(title, ruleList);
				LOGGER.debug("标题市场规则 - 格式化后：{}", title);
				titleElement.setText(title);
			}
		}
		return document.asXML();
	}
	
	/**
	 *
	 * @param content
	 * @param ruleList
	 * @return
	 * @description: 通过规则格式化
	 */
	public static String format(String content, List<RuleConfig> ruleList) {
		boolean onceMatchFlag = false;
		for (RuleConfig rule : ruleList) {

			boolean isOnce = rule.getExecuteRule().equals(ExecuteRule.Once.toString());
			if (onceMatchFlag && isOnce) {
				continue;
			}
			String old = content;
			content = content.replaceAll(rule.getRegularMatch(), rule.getRegularReplace());
			if (isOnce && !onceMatchFlag && !old.equals(content)) {
				onceMatchFlag = true;
			}
		}
		return content;
	}

	/**
	 *
	 *
	 * @return
	 * @description: TMDB规则生成
	 */
	public static JSONObject tmdbRegex(JSONObject tmdbArray) {
		String titleRegex = null;
		String RegularRule = null;
		String RegularResult = null;
		JSONObject Result = new JSONObject();
		String title_CN = tmdbArray.getString("title");
		String title_EN = tmdbArray.getString("original_title");
		String title_Search = tmdbArray.getString("search_key");
		String title_RCN = tmdbArray.getString("title");
		String title_REN = tmdbArray.getString("original_title");
		String type = tmdbArray.getString("type");
		String year = tmdbArray.getString("year");
		String language = tmdbArray.getString("language");

		if (!language.equals("en")) {
			title_EN = tmdbArray.getString("search_key");
			title_REN = tmdbArray.getString("search_key");
		}

		if (title_CN.indexOf("：") > -1 || title_CN.indexOf(":") > -1) {
			title_CN = title_CN.replaceAll("[：:]","(.?)");
		}
		if (title_EN.indexOf(": ") > -1){
			title_EN = title_EN.replaceAll(": ", " ");
		}
		if (title_EN.indexOf(" ") > -1){
			title_EN = title_EN.replaceAll(" ","(.+?)");
		}
		if(year.indexOf(" ") > -1) {
			year = year.replaceAll(" ",".");
		}

		if (title_EN.equals(title_Search.replaceAll(" ","(.?)"))){
			titleRegex = "(?<name>"+title_CN+"|"+title_EN+")";
		}
		else {
			titleRegex = "(?<name>"+title_CN+"|"+title_EN+"|"+title_Search.replaceAll(" ","(.?)")+")";
		}
		if (type.equals("Movie") && year != null){
			RegularRule = "(?i)(?<group>【.*?】|\\[.*?\\]|.*?)(?<other>.*)"+titleRegex+"(?<other2>\\]*)(?<other3>.*)(?<year>.*((19|20)\\d{2}))(?<txt>.*)";
			RegularResult = "${group}【"+title_RCN+"】"+title_REN+"."+year+"${txt}";
			Result.put("rule", RegularRule);
			Result.put("result", RegularResult);
			Result.put("title", title_CN);
			Result.put("title_en", title_REN);
			Result.put("type", type);
			Result.put("year",tmdbArray.getString("year"));
		}
		else {
			RegularRule = "(?i)(?<group>【.*?】|\\[.*?\\]|.*?)(?<other>.*)"+titleRegex+"(?<other2>\\]*)(?<txt>.*)";
			RegularResult = "${group}【"+title_RCN+"】"+title_REN;
			Result.put("rule", RegularRule);
			Result.put("result", RegularResult);
			Result.put("title", title_CN);
			Result.put("title_en", title_REN);
			Result.put("type", type);
			Result.put("year",tmdbArray.getString("year"));
		}

		LOGGER.debug("TMDB规则一:{}", titleRegex);
		LOGGER.debug("TMDB替换规则:{}", RegularRule);
		return Result;
	}


	/**
	 *
	 *
	 * @return
	 * @description: TMDB标题格式化，TV集数格式化
	 */

	public static String tmdbFormat(String content, JSONObject rule) {
		AnimeUtil animeUtil = new AnimeUtil();
		String Match = rule.getString("rule");
		String Replace = rule.getString("result");
		String Groupname = null;
		String GroupRule = null;
		String RegularNumber = "${txt}";
		String RegularText = "(?i)S\\d{2}\\s*-\\s*S\\d{2}|\\s+S\\d{1,2}\\s*-\\s*\\d{1,2}\\s+|\\[?S\\d{1,2}\\]?|\\s+S\\d{1,2}\\s+|EP?\\d{2,4}\\s*-\\s*EP?\\d{2,4}|\\[?EP?\\d{2,4}\\]?|\\s+EP?\\d{1,4}|\\[\\d{2}\\]|\\[?第\\d{1,2}[集话]\\]?";
		String ResultName = null;
		String ResultNumber = null;
		String ResultText = null;
		JSONObject Episode = new JSONObject();
		content = content.replaceAll("【","[");
		content = content.replaceAll("】","]");
		Pattern r = Pattern.compile(Match);
		Matcher m = r.matcher(content);
		if (m.find()) {
			if (m.group("group") != null && !m.group("group").equals("") && !m.group("group").equals(rule.getString("title")) && !m.group("group").equals(rule.getString("title_en"))) {
				Groupname = m.group("group");
				GroupRule = "【(.*)】|\\[(.*)\\]";
				Pattern r2 = Pattern.compile(GroupRule);
				Matcher m2 = r2.matcher(Groupname);
				if (m2.find()) {
					if (m2.group(1) != null && !m2.group(1).equals("")) {
						Replace = Replace.replaceAll("\\$\\{group\\}", "【"+m2.group(1)+"】");
					}
					else{
						Replace = Replace.replaceAll("\\$\\{group\\}", "【"+m2.group(2)+"】");
					}
				}
				else{
					Replace = Replace.replaceAll("\\$\\{group\\}","【\\${group}】");
				}
			}
			ResultName = content.replaceAll(Match, Replace);
			ResultNumber = content.replaceAll(Match,RegularNumber);
			LOGGER.debug("提取集数日志:{}", ResultNumber);

			if (rule.getString("type").equals("Tv")){
				Episode = animeUtil.AnimeInfo(content.replaceAll(Match, Replace+"${txt}"),content.replaceAll(Match, "${other}"),content.replaceAll(Match,"${txt}"));
				ResultText = ResultNumber.replaceAll(RegularText,"");
				ResultText = animeUtil.EpisodeText(ResultText);
				LOGGER.debug("去除集数后文本:{}", ResultText);

				if (Episode.containsKey("kElementAnimeSeason") && Episode.containsKey("kElementEpisodeNumber")){
					ResultName = ResultName + "[" + Episode.getString("kElementAnimeSeason") + Episode.getString("kElementEpisodeNumber")+"]";
				}
				else if(Episode.containsKey("kElementEpisodeTitle") && Episode.containsKey("kElementEpisodeNumber")){
					ResultName = ResultName + "[" + Episode.getString("kElementEpisodeTitle") + Episode.getString("kElementEpisodeNumber")+"]";
				}
				else if(Episode.containsKey("OtherEpisode")){
					if(Episode.getString("OtherEpisode").equals("null")){
						ResultName = ResultName;
					}
					else{
						ResultName = ResultName + "[" + Episode.getString("OtherEpisode") +"]";
					}
				}
				LOGGER.debug("TMDB - 最终命名规则:{}", ResultName);
				if (!ResultName.equals(ResultText)){
					content = ResultName + ResultText.trim();
				}
			}
			else{
				content = ResultName;
			}
		}
		return content;
	}

	public static String tmdbFormatsk(String searchkey,JSONObject data){
		if (TmdbUtil.tmdbSTATUS() && TmdbUtil.tmdbChineseSTATUS()){{
			if (!data.getString("title").equals("null")){
				searchkey = data.getString("title");
				LOGGER.debug("TMDB关键字 - 中文化后：{}", searchkey);
			}
			if (!data.getString("year").equals("null") && !data.getString("type").equals("Tv")) {
				searchkey = searchkey + " " + data.getString("year");
			}
		}}
		else{
			if (data.getString("type").equals("Tv")){
				searchkey = data.getString("search_key");
			}
		}
		return searchkey;
	}
}
