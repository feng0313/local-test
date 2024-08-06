package com.chunfeng.local.controller;

import com.chunfeng.local.common.Result;
import com.chunfeng.local.service.WeiXinService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

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
    public Result<String> manualFullUpdate(@RequestBody Map<String, String> map) {
        if (map.get("nextOpenId") == null) {
            return Result.error("nextOpenId不能为空");
        }
        weiXinService.updateUserInfo(map.get("nextOpenId"));
        return Result.success("更新成功");
    }
}