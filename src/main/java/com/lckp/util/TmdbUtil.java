package com.lckp.util;

/*
@author manduted
@date 2022-10-04
*/

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.lckp.config.JProxyConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TmdbUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(TmdbUtil.class);

    public static Boolean tmdbSTATUS(){
        Boolean result = null;
        String tSTATUS = null;
        tSTATUS = JProxyConfiguration.keyconfig.getTmdbSTATUS();
        if (tSTATUS == null){
            result = false;
        }
        else if (tSTATUS.equals("true")){
            result = true;
        }
        return result;
    }

    public static Boolean tmdbChineseSTATUS(){
        Boolean result = null;
        String tSTATUS = null;
        tSTATUS = JProxyConfiguration.keyconfig.getTmdbCHINESE();
        if (tSTATUS == null){
            result = false;
        }
        else if (tSTATUS.equals("true")){
            result = true;
        }
        return result;
    }

    public static Boolean tmdbPROXYSTATUS(){
        Boolean STATUS = null;
        String tSTATUS = null;
        tSTATUS = JProxyConfiguration.keyconfig.getTmdbPROXYSTATUS();
        if (tSTATUS == null) {
            STATUS = false;
        }
        else if (tSTATUS.equals("true")){
            STATUS = true;
        }
        return STATUS;
    }

    public static JSONObject tmdbPROXY(){
        String proxyIP = null;
        String proxyPort = null;
        JSONObject result = new JSONObject();
        if (tmdbPROXYSTATUS()){
            proxyIP = JProxyConfiguration.keyconfig.getTmdbPROXYIP();
            proxyPort = JProxyConfiguration.keyconfig.getTmdbPROXYPORT();
            result.put("status","true");
            result.put("proxyIP", proxyIP);
            result.put("proxyPort", proxyPort);
        }
        else{
            result.put("status","false");
            result.put("proxyIP", "null");
            result.put("proxyPort", "null");
        }
        LOGGER.debug("代理配置: {}",result);
        return result;
    }

    public static JSONObject tmdbTYPE(String SearchKey){

        JSONObject tmdbResult = new JSONObject();
        String tmdbRegex = "(?i)(.*)(\\d{4}$|s[0-9]+e*p*[0-9]+)";
        String tmdbRegex2 = "\\d{4}$";
        String tmdbRegex3 = "(?i)s[0-9]+e*p*[0-9]+";
        String tmdbRegex4 = "(?i)(.*)(s[0-9]+.+s[0-9]+e*p*[0-9]+)";
        List<String> tmdbRegexList = Arrays.asList(
                "1st","2nd","3rd","4th","5th","6th","7th","8th","9th","10th",
                "First","Second","Third","Fourth","Fifth","Sixth","Seventh","Eighth","Ninth","Tenth",
                "第一部","第二部","第三部","第四部","第五部","第六部","第七部","第八部","第九部","第十部",
                "第一季","第二季","第三季","第四季","第五季","第六季","第七季","第八季","第九季","第十季",
                "Ⅰ","Ⅱ","Ⅲ","Ⅳ","Ⅴ","Ⅵ","Ⅶ","Ⅷ","Ⅸ","Ⅹ",
                "I","II","III","IV","V","VI","VII","VIII","IX","X"
        );

        if (SearchKey != null)
        {
            Pattern r = Pattern.compile(tmdbRegex);
            Matcher m = r.matcher(SearchKey);
            if (m.find()){
                Pattern r2 = Pattern.compile(tmdbRegex2);
                Matcher m2 = r2.matcher(m.group(2));
                Pattern r3 = Pattern.compile(tmdbRegex3);
                Matcher m3 = r3.matcher(m.group(2));
                Pattern r4 = Pattern.compile(tmdbRegex4);
                Matcher m4 = r4.matcher(SearchKey);
                if (m2.find()){
                    tmdbResult.put("name",m.group(1).trim());
                    tmdbResult.put("year",m.group(2));
                    tmdbResult.put("type","Movie");
                }
                else if (m3.find() ){
                    if (m4.find()){
                        tmdbResult.put("name",m4.group(1).trim());
                        tmdbResult.put("year",m4.group(2));
                    }
                    else {
                        tmdbResult.put("name",m.group(1).trim());
                        tmdbResult.put("year",m.group(2));
                    }
                    tmdbResult.put("type","Tv");
                }
            }
            else{
                tmdbResult.put("name",SearchKey);
                tmdbResult.put("year","null");
                tmdbResult.put("type","Tv");
            }
            if (tmdbResult.getString("type").equals("Tv") && tmdbResult.getString("name") != null){
                String  Name = tmdbResult.getString("name").trim();
                String[] split = null;
                for(String s : tmdbRegexList){
                    if(Name.endsWith(s)){
                        split = Name.split(s);
                        Name = split[0].trim();
                    }
                }
                tmdbResult.put("name",Name);
            }
        }
        else {
            tmdbResult.put("name","null");
            tmdbResult.put("year","null");
            tmdbResult.put("type","Error");
        }
        return tmdbResult;
    }

    public static JSONObject tmdbName(String SearchKey) throws Exception {
        JSONObject tResult = new JSONObject();
        JSONObject tName = new JSONObject();
        JSONObject jsonObject = new JSONObject();
        JSONArray array = new JSONArray();
        String Apikey = null;
        String url = null;
        String Body = null;
        Apikey = JProxyConfiguration.keyconfig.getTmdbAPIKEY();
        tResult = tmdbTYPE(SearchKey);

        if (Apikey != null && tResult.getString("type").equals("Movie")) {
            url = "http://api.themoviedb.org/3/search/movie?api_key="+URLEncoder.encode(Apikey,"UTF-8")+"&language=zh-CN&query="+URLEncoder.encode(tResult.getString("name").replaceAll("\\s+$", ""),"UTF-8")+"&page=1&include_adult=false&year="+URLEncoder.encode(tResult.getString("year"),"UTF-8");
            LOGGER.debug("TMDB_URL: {}",url);
            jsonObject = doGet(url);
            Body = String.valueOf(jsonObject);
            if (jsonObject.getString("tmdb_result").equals("success")){
                array = jsonObject.getJSONArray("results");
                tName = new JSONObject(tmdbFilterName(array,"Movie"));
                tName.put("search_key",tResult.getString("name"));
                tName.put("year",tResult.getString("year"));
                tName.put("type",tResult.getString("type"));
            }
            else {
                tName.put("title", "null");
                tName.put("original_title", "null");
                tName.put("search_key","null");
                tName.put("language","null");
                tName.put("year","null");
                tName.put("type","null");
            }
        }
        else if(Apikey != null && tResult.getString("type").equals("Tv")){
            url = "http://api.themoviedb.org/3/search/tv?api_key="+URLEncoder.encode(Apikey,"UTF-8")+"&language=zh-CN&page=1&query="+URLEncoder.encode(tResult.getString("name").replaceAll("\\s+$", ""),"UTF-8")+"&include_adult=false";
            LOGGER.debug("TMDB_URL: {}",url);
            jsonObject = doGet(url);
            Body = String.valueOf(jsonObject);
            if (jsonObject.getString("tmdb_result").equals("success")){
                array = jsonObject.getJSONArray("results");
                tName = new JSONObject(tmdbFilterName(array,"Tv"));
                tName.put("search_key",tResult.getString("name"));
                tName.put("year",tResult.getString("year"));
                tName.put("type",tResult.getString("type"));
            }
            else {
                tName.put("title", "null");
                tName.put("original_title", "null");
                tName.put("search_key","null");
                tName.put("language","null");
                tName.put("year","null");
                tName.put("type","null");
            }
        }
        else{
            Body = "null";
            tName.put("title", "null");
            tName.put("original_title", "null");
            tName.put("search_key","null");
            tName.put("language","null");
            tName.put("year","null");
            tName.put("type","null");
        }
        LOGGER.debug("TMDB Result: {}", Body);
        LOGGER.debug("TMDB_RESULT: {}", array);
        LOGGER.debug("TMDB_NAME: {}", tName);
        return tName;
    }

    public static JSONObject doGet(String url) {
        JSONObject proxyJSON = new JSONObject(tmdbPROXY());
        HttpURLConnection con = null;
        LOGGER.info("请求连接：{}", "开始连接TMDB");
        JSONObject object = new JSONObject();
        try {
            URL encode = new URL(url);
            if (proxyJSON.getString("status").equals("false")) {
                con = (HttpURLConnection) encode.openConnection();
                LOGGER.info("连接类型：{}", "直连TMDB");
            } else if (proxyJSON.getString("status").equals("true")) {
                Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyJSON.getString("proxyIP"), Integer.parseInt(proxyJSON.getString("proxyPort"))));
                con = (HttpURLConnection) encode.openConnection(proxy);
                LOGGER.info("连接类型：{}", "代理TMDB");
            }
            con.setUseCaches(false);
            con.setConnectTimeout(10000);
            con.setReadTimeout(10000);
            con.setRequestMethod("GET");
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("Cache-Control", "no-cache");

            InputStream inStream = con.getInputStream();
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len = 0;
            while ((len = inStream.read(buffer)) != -1) {
                outStream.write(buffer, 0, len);
            }
            outStream.close();
            inStream.close();
            //网页的二进制数据
            String rtn = outStream.toString("utf-8");
            if (StringUtils.isNotBlank(rtn)) {
                object = JSONObject.parseObject(rtn);
                object.put("tmdb_result","success");
                return object;
            }
        } catch (SocketTimeoutException e) {
            object.put("tmdb_result","fail");
            LOGGER.error("请求连接超时：{}", e.getMessage());
        } catch (Exception e) {
            object.put("tmdb_result","fail");
            LOGGER.error("请求异常：{}", e.getMessage());
        } finally {
            if (con != null) {
                con.disconnect();
                LOGGER.info("连接关闭：{}", "关闭HTTP连接");
            }
        }
        return object;
    }

    public static JSONObject tmdbFilterName(JSONArray rtn,String Type){
        JSONObject rtn1 = new JSONObject();
        rtn1 = (JSONObject) rtn.get(0);
        JSONObject ResultName = new JSONObject();
        if (Type.equals("Movie")){
            String tmdb_tn = rtn1.getString("title");
            String tmdb_on = rtn1.getString("original_title");
            String language = rtn1.getString("original_language");
            ResultName.put("title", tmdb_tn);
            ResultName.put("original_title",tmdb_on);
            ResultName.put("language",language);
        }
        else if (Type.equals("Tv")){
            String tmdb_tn = rtn1.getString("name");
            String tmdb_on = rtn1.getString("original_name");
            String language = rtn1.getString("original_language");
            ResultName.put("title", tmdb_tn);
            ResultName.put("original_title",tmdb_on);
            ResultName.put("language",language);
        }
        return ResultName;
    }

    public static Boolean testProxy(JSONObject PROXY) {
        HttpURLConnection con = null;
        String proxyip = null;
        String proxyport = null;
        LOGGER.info("请求连接：{}","开始连接测试");
        try {
            URL url = new URL("http://api.themoviedb.org/3/");
            if (PROXY.getString("status").equals("true")){
                proxyip = PROXY.getString("proxyIP");
                proxyport= PROXY.getString("proxyPort");
                Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyip, Integer.parseInt(proxyport)));
                con = (HttpURLConnection) url.openConnection(proxy);
                LOGGER.info("连接类型：{}","代理测试");
            }
            else{
                con = (HttpURLConnection) url.openConnection();
                LOGGER.info("连接类型：{}","直连测试");
            }
            con.setUseCaches(false);
            con.setConnectTimeout(10000);
            con.setReadTimeout(10000);
            con.setRequestMethod("GET");
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("Cache-Control", "no-cache");
            con.connect();
            return true;
        } catch (SocketTimeoutException e) {
            LOGGER.error("请求连接超时：{}", e.getMessage());
            return false;
        } catch (Exception e) {
            LOGGER.error("请求异常：{}", e.getMessage());
            return false;
        } finally {
            if (con != null){
                con.disconnect();
                LOGGER.info("连接关闭：{}", "关闭HTTP连接");
            }
        }
    }
}
