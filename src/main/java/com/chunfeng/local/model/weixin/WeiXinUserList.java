package com.chunfeng.local.model.weixin;

import lombok.Data;

import java.util.List;

@Data
public class WeiXinUserList {
    private List<OpenIdEntry> user_list;

    @Data
    public static class OpenIdEntry {
        private String openid;
        private String lang;
    }
}