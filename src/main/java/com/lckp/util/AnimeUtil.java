package com.lckp.util;

/*
@author manduted
@date 2022-12-10
*/

import com.alibaba.fastjson.JSONObject;
import com.lckp.anitomyJ.AnitomyJ;
import com.lckp.anitomyJ.Element;
import liquibase.pro.packaged.E;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AnimeUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnimeUtil.class);
    private String ru = "(?i)S\\d{2}\\s*-\\s*S\\d{2}|\\s+S\\d{2}\\s*-\\s*\\d{2}\\s+|\\[?S\\d{1,2}\\]?|\\s+S\\d{1,2}\\s+|EP?\\d{2,4}\\s*-\\s*EP?\\d{2,4}|\\[?EP?\\d{2,4}\\]?|\\s+EP?\\d{1,4}|\\[\\d{2}\\]|\\[?第\\d{1,2}[集话]\\]?";
    private String ru1 = "(?i)S([1-9]+)";
    private final Map<String, String> ordinals = new HashMap<String, String>() {{
        //@formatter:off
        put("1st", "S01"); put("First", "S01");
        put("2nd", "S02"); put("Second", "S02");
        put("3rd", "S03"); put("Third", "S03");
        put("4th", "S04"); put("Fourth", "S04");
        put("5th", "S05"); put("Fifth", "S05");
        put("6th", "S06"); put("Sixth", "S06");
        put("7th", "S07"); put("Seventh", "S07");
        put("8th", "S08"); put("Eighth", "S08");
        put("9th", "S09"); put("Ninth", "S09");}};

    public JSONObject AnimeInfo(String param,String SearchSeason){
        List<Element> elements = AnitomyJ.parse(param);
        List list = null;
        String[] parts = null;
        Map<String, String> resultA = new HashMap<String, String>();
        JSONObject resultB = new JSONObject();
        list = (elements.stream()
                .map(e -> e.getCategory().name() + "=" + e.getValue())
                .collect(Collectors.toList()));
        System.out.println(list);
        if (list.size() > 0 & !list.isEmpty()) {
            for (Object o : list) {
                parts = o.toString().split("=", 2);
                resultA.put(parts[0], parts[1]);
            }
        }
        if(resultA.containsKey("kElementAnimeSeason") || resultA.containsKey("kElementEpisodeTitle")){
            if (resultA.containsKey("kElementAnimeSeason")){
                resultB.put("kElementAnimeSeason",AnimeSeason(resultA.get("kElementAnimeSeason")));
            }
            else{
                resultB.put("kElementEpisodeTitle",AnimeSeason(resultA.get("kElementEpisodeTitle")));
            }
            if(resultA.containsKey("kElementEpisodeNumber")){
                resultB.put("kElementEpisodeNumber",AnimeEpisode(resultA.get("kElementEpisodeNumber")));
            }
        }
        else{
            resultB.put("OtherEpisode",OtherEpisode(param));
        }
        return resultB;
    }

    private String AnimeSeason(String number){
        Pattern r = Pattern.compile(ru1);
        Matcher m = r.matcher(number);
        if (m.find()){
            if(m.group(1) != null && m.group(1).length() <2){
                number = number.replaceAll(ru1,"S0"+m.group(1).trim());
            }
        }
        else{
            if (number.length() == 2){
                number = "S" + number;
            }
            else if (number.length() == 1){
                number = "S0" + number;
            }
        }
        number = number.replaceAll("\\[","");
        number = number.replaceAll("\\]","");
        number = EpisodeParse(number);
        return number;
    }

    private String AnimeEpisode(String number){
        number = "E" + number;
        return number;
    }

    private String OtherEpisode(String Title){
        List<String> list = new ArrayList<>();
        String nTitle = null;
        Pattern r = Pattern.compile(ru);
        Matcher m = r.matcher(Title);
        while(m.find()) {
            list.add(m.group());
        }
        if (list.size() == 2){
            nTitle = list.get(0).trim() + list.get(1).trim();
        }
        else if (list.size() == 4){
            nTitle = list.get(0).trim() + list.get(1).trim();
            nTitle = nTitle + "-" + list.get(2).trim() + list.get(3).trim();
        }
        else if (list.size() == 1){
            nTitle = list.get(0).trim();
        }
        else{
            nTitle = "null";
        }
        LOGGER.debug("处理集数List日志：{}", list);

        if (nTitle.contains("EP")){
            nTitle = nTitle.replaceAll("EP", "E").trim();
        }
        else{
            nTitle = nTitle.replaceAll("\\[","E");
            nTitle = nTitle.replaceAll("\\]","");
        }
        Pattern r1 = Pattern.compile(ru1);
        Matcher m1 = r1.matcher(nTitle);
        if (m1.find()){
            if(m1.group(1) != null && m1.group(1).length() <2){
                nTitle = nTitle.replaceAll(ru1,"S0"+m1.group(1).trim());
            }
        }
        return nTitle;
    }

    private String EpisodeParse(String Episode){
        if(ordinals.containsKey(Episode.trim())){
            Episode = ordinals.get(Episode);
        }
        return Episode;
    }

    public String EpisodeText(String Text) {
        String mapKey = null;
        for (String s : ordinals.keySet()){
            if (Text.contains(s)){
                mapKey = s;
            }
        }
        String Re = "\\[?\\s?"+ mapKey +"\\]?\\s?";
        Text = Text.replaceAll(Re,"").trim();
        LOGGER.debug("其他集数文本处理规则：{}", Re);
        LOGGER.debug("其他集数文本处理日志：{}", Text);
        return Text;
    }
}
