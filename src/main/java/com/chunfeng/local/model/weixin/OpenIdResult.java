package com.chunfeng.local.model.weixin;

import lombok.Data;

import java.util.List;

/**
 * 公众号关注列表
 *
 * @author 13994
 */
@Data
public class OpenIdResult {
    /**拉取的OPENID个数，最大值为10000*/
    private int count;
    /**列表数据，OPENID的列表*/
    private OpenIdList data;
    /**关注该公众账号的总用户数*/
    private int total;
    /**拉取列表的最后一个用户的OPENID*/
    private String next_openid;

    @Data
    public class OpenIdList {
        private List<String> openid;
    }
}