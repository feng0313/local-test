package com.chunfeng.local.utils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.chunfeng.local.model.weixin.OpenIdResult;
import com.chunfeng.local.model.weixin.WeiXinUserList;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 13994
 */
@Slf4j
@Component
public class WeiXinHelper {

    /**
     * 微信提供获取access_token接口地址
     */
    private static final String ACCESS_TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential";
    /**
     * 请求用户列表
     */
    private static final String GET_USER_LIST_URL = "https://api.weixin.qq.com/cgi-bin/user/get?access_token=";
    /**
     * 使用openid获取用户信息每次一百条
     */
    private static final String BATCH_GET = "https://api.weixin.qq.com/cgi-bin/user/info/batchget?access_token=";
    /**
     * 第三方用户唯一凭证
     */
    private static final String APPID = "wxc1a6fd958d34d23f";
    /**
     * 第三方用户唯一凭证密钥，即appSecret
     */
    private static final String SECRET = "eb531d3335341ed23497a849d462bc4e";


    /**
     * 获取微信公众号请求token
     *
     * @return
     */
    @SneakyThrows
    public String getAccessToken() {
        String result = HttpClientUtil.sendHttpGetRequest(ACCESS_TOKEN_URL + "&appid=" + APPID + "&secret=" + SECRET);
        if (StringUtils.isNotBlank(JSONObject.parseObject(result).getString("access_token"))) {
            String token = JSONObject.parseObject(result).getString("access_token");
            log.info("获取access_token成功");
            return token;
        } else {
            log.error("获取access_token失败===>{}", result);
            return null;
        }
    }

    /**
     * 以token获取关注用户列表
     * 每次最大一万条
     *
     * @param token
     * @return
     */
    @SneakyThrows
    public List<String> getUserList(String token) {
        List<String> allOpenIds = new ArrayList<>();
        String nextOpenId = "";
        while (true) {
            String response = HttpClientUtil.sendHttpGetRequest(GET_USER_LIST_URL + token + "&next_openid=" + nextOpenId);
            if (StringUtils.isNotBlank(JSONObject.parseObject(response).getString("data"))) {
                OpenIdResult currentResult = JSONObject.parseObject(response, new TypeReference<OpenIdResult>() {
                });
                allOpenIds.addAll(currentResult.getData().getOpenid());
                nextOpenId = currentResult.getNext_openid();
                log.info("获取关注用户列表成功");
                log.info("关注用户总数：{}，本次传入用户数：{}，最后一个关注用户的openId:{}",
                        currentResult.getTotal(),
                        currentResult.getCount(),
                        nextOpenId);
                if ( currentResult.getCount() < 10000) {
                    break;
                }
            } else {
                log.error("获取关注用户列表失败===>{}", response);
                return null;
            }
        }
        return allOpenIds;
    }

    /**
     * 以openid获取用户信息
     *
     * @param weiXinUserList
     * @param token
     * @return
     */
    @SneakyThrows
    public JSONArray getWeiXinUserInfo(WeiXinUserList weiXinUserList, String token) {
        String result = HttpClientUtil.sendHttpPostRequest(BATCH_GET + token, JSONObject.toJSONString(weiXinUserList));
        if (StringUtils.isNotBlank(JSONObject.parseObject(result).getString("user_info_list"))) {
            log.info("获取关注用户信息成功");
            return JSONObject.parseObject(result).getJSONArray("user_info_list");
        } else {
            log.error("获取关注用户信息失败===>{}", result);
            return null;
        }
    }
}