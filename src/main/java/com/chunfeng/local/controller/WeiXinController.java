package com.chunfeng.local.controller;

import com.chunfeng.local.common.Result;
import com.chunfeng.local.service.WeiXinService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author by 春风能解释
 * 微信公众号等微信相关功能
 * 2022/4/3
 */
@RestController
public class WeiXinController {

    @Autowired
    private WeiXinService weiXinService;

    @PostMapping("/manualFullUpdate")
    public Result<String> manualFullUpdate() {
        return weiXinService.updateUserInfo();
    }
}