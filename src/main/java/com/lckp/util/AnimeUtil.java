package com.lckp.util;

/*
@author manduted
@date 2022-12-10
*/

import com.alibaba.fastjson.JSONObject;
import com.lckp.anitomyJ.AnitomyJ;
import com.lckp.anitomyJ.Element;
import com.lckp.proxy.IndexerProxy;
import com.lckp.util.TextsplitUlit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Parameter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AnimeUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnimeUtil.class);
    private TextsplitUlit textsplitUlit = new TextsplitUlit();
    private String ru = "(?i)S\\d{2}\\s*-\\s*S\\d{2}|\\s+S\\d{1,2}\\s*-\\s*\\d{1,2}\\s+|\\[?S\\d{1,2}\\]?|\\s+S\\d{1,2}\\s+|EP?\\d{2,4}\\s*-\\s*EP?\\d{2,4}|\\[?EP?\\d{2,4}\\]?|\\s+EP?\\d{1,4}|\\[\\d{2}\\]|\\[?第\\d{1,2}[集话]\\]?";
    private String ru1 = "(?i)S([1-9]+)";
    private String ru2 = "\\d{1,2}";
    private String ru3 = "(?i)S\\d{1,2}E\\d{1,2}";
    private String ru4 = "(?i)S\\d{1,2}";
    private String ru5 = "(?i)S\\d{1,2}\\s*-\\s*\\d{1,2}";

    private String packageRule = "(?i)\\s?\\d{1,2}\\s?-\\s?\\d{1,2}\\s?(Fin|End)|[全合]集";

    private final Map<String, String> ordinals = new LinkedHashMap<String, String>() {{
        //@formatter:off
        put("1st", "S01"); put("First", "S01"); put("第一部","S01"); put("第一季","S01");
        put("2nd", "S02"); put("Second", "S02"); put("第二部","S02"); put("第二季","S02");
        put("3rd", "S03"); put("Third", "S03"); put("第三部","S03"); put("第三季","S03");
        put("4th", "S04"); put("Fourth", "S04"); put("第四部","S04"); put("第四季","S04");
        put("5th", "S05"); put("Fifth", "S05"); put("第五部","S05"); put("第五季","S05");
        put("6th", "S06"); put("Sixth", "S06"); put("第六部","S06"); put("第六季","S06");
        put("7th", "S07"); put("Seventh", "S07"); put("第七部","S07"); put("第七季","S07");
        put("8th", "S08"); put("Eighth", "S08"); put("第八部","S08"); put("第八季","S08");
        put("9th", "S09"); put("Ninth", "S09"); put("第九部","S09"); put("第九季","S09");
        put("10th", "S19"); put("Tenth", "S10"); put("第十部","S10"); put("第十季","S10");
        put("I","S01"); put("II","S02"); put("III","S03"); put("IV","S04"); put("V","S05"); put("VI","S06"); put("VII","S07"); put("VIII","S08"); put("IX","S09"); put("X","S10");
        put("Ⅰ","S01");put("Ⅱ","S02"); put("Ⅲ","S03"); put("Ⅳ","S04"); put("Ⅴ","S05"); put("Ⅵ","S06"); put("Ⅶ","S07"); put("Ⅷ","S08"); put("Ⅸ","S09"); put("Ⅹ","S10");
    }};

    public JSONObject AnimeInfo(String param,String OtherText,String Text){
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
            resultB.put("OtherEpisode",OtherEpisode(param,OtherText,Text));
        }
        return resultB;
    }

    private String AnimeSeason(String number){
        Pattern r = Pattern.compile(ru1);
        Pattern r1 = Pattern.compile(ru2);
        Matcher m = r.matcher(number);
        Matcher m1 = r1.matcher(number);
        if (m.find()){
            if(m.group(1) != null && m.group(1).length() <2){
                number = number.replaceAll(ru1,"S0"+m.group(1).trim());
            }
        }
        else{
            if (m1.find()){
                if (number.length() == 2){
                    number = "S" + number;
                }
                else if (number.length() == 1){
                    number = "S0" + number;
                }
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

    private String OtherEpisode(String Title,String OtherText,String Text){
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
            nTitle = nTitle.replaceAll("[第集话]","");
            nTitle = nTitle.replaceAll(" ","");
        }
        Pattern r1 = Pattern.compile(ru1);
        Matcher m1 = r1.matcher(nTitle);
        if (m1.find()){
            if(m1.group(1) != null && m1.group(1).length() <2){
                nTitle = nTitle.replaceAll(ru1,"S0"+m1.group(1).trim());
            }
        }
        Pattern r4 = Pattern.compile(ru5);
        Matcher m4 = r4.matcher(nTitle);
        if (m4.find()){
            nTitle = nTitle.replaceAll("-","E");
        }
        Pattern r2 = Pattern.compile(ru3);
        Matcher m2 = r2.matcher(nTitle);
        Pattern r3 = Pattern.compile(ru4);
        Matcher m3 = r3.matcher(nTitle);
        if(!m2.find() && !m3.find()){
            nTitle = SeasonHelper(nTitle,OtherText,Text);
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
        String Re = null;
        Text = textsplitUlit.Textsplit(Text);
        for (String s : ordinals.keySet()){
            Pattern p = Pattern.compile("\\b"+ s +"\\b");
            Matcher m = p.matcher(Text);
            if (m.find()){
                mapKey = s;
            }
        }
        Re = "\\["+mapKey +"\\]";
        Text = Text.replaceAll(Re,"").trim();
        LOGGER.debug("其他集数文本处理规则：{}", Re);
        return Text;
    }

    private String  SeasonHelper(String param,String OtherText,String Text){
        String mapValue = null;
        String[] split = null;
        String rule = "E\\d{1,4}-E\\d{1,4}";
        OtherText = textsplitUlit.Textsplit(OtherText);
        Text = textsplitUlit.Textsplit(Text);

        for (String s : ordinals.keySet()){
            Pattern p = Pattern.compile("\\b"+ s +"\\b");
            Matcher m = p.matcher(OtherText);
            Matcher m1 = p.matcher(Text);
            if(m.find()){
                mapValue = ordinals.get(s);
            }
            else{
                if (m1.find()){
                    mapValue = ordinals.get(s);
                }
            }
        }
        if (mapValue != null){
            Pattern r1 = Pattern.compile(rule);
            Matcher m1 = r1.matcher(param);
            if (m1.find()){
                split = param.split("-");
                param = mapValue+split[0]+"-"+mapValue+split[1];
            }
            else{
                if (!param.equals("null")){
                    param = mapValue + param;
                }
                else{
                    param = mapValue;
                }
            }
        }
        return param;
    }

}
