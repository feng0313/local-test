package com.chunfeng.local.service;

import com.chunfeng.local.common.Result;

/**
 * @author 13994
 */
public interface WeiXinService {
    /**
     * 全量同步微信公众号用户信息
     *
     * @param openId 用户的openId
     * @return 用户信息
     */
    Result<String> updateUserInfo(String openId);
}