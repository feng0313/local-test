package com.chunfeng.local.model.weixin;

import lombok.Data;

import java.util.List;

@Data
public class WechatUser {

    private Integer subscribe;
    private String openid;
    private String language;
    private Integer subscribe_time;
    private String unionid;
    private String remark;
    private Integer groupid;
    private List<Integer> tagid_list;
    private String subscribe_scene;
    private Integer qr_scene;
    private String qr_scene_str;
}