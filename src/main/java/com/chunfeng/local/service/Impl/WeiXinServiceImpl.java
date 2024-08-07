package com.chunfeng.local.service.Impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chunfeng.local.common.Result;
import com.chunfeng.local.mapper.weixin.AppUserWxmpMapper;
import com.chunfeng.local.model.weixin.AppUserWxmp;
import com.chunfeng.local.model.weixin.OpenIdResult;
import com.chunfeng.local.model.weixin.WechatUser;
import com.chunfeng.local.model.weixin.WeiXinUserList;
import com.chunfeng.local.service.WeiXinService;
import com.chunfeng.local.utils.WeiXinHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author 13994
 */
@Slf4j
@Service
public class WeiXinServiceImpl implements WeiXinService {

    @Value("${backend.scheduled.enable}")
    Boolean enable;

    @Autowired
    private WeiXinHelper weiXinHelper;

    @Autowired
    private AppUserWxmpMapper appUserWxmpMapper;

    @Override
    public Result<String> updateUserInfo() {
        log.info("获取公众号关注用户信息===>开始");
        String token = weiXinHelper.getAccessToken();
        OpenIdResult openIdResult = weiXinHelper.getUserList(token);
        if (openIdResult == null) {
            return Result.error("获取失败");
        }
        List<String> openidList = openIdResult.getData().getOpenid();
        List<AppUserWxmp> appUserWxmpList = appUserWxmpMapper.selectList(new LambdaQueryWrapper<AppUserWxmp>()
                .orderByDesc(AppUserWxmp::getCreateTime));
        int i = 0;
        for (int start = 0; start < openidList.size(); start += 100) {
            int end = Math.min(start + 100, openidList.size());
            List<String> currentBatch = openidList.subList(start, end);
            WeiXinUserList weiXinUserList = new WeiXinUserList();
            List<WeiXinUserList.OpenIdEntry> user_list = currentBatch.stream()
                    .map(openId -> {
                        WeiXinUserList.OpenIdEntry entry = new WeiXinUserList.OpenIdEntry();
                        entry.setLang("zh_CN");
                        entry.setOpenid(openId);
                        return entry;
                    })
                    .collect(Collectors.toList());
            weiXinUserList.setUser_list(user_list);
            JSONArray jsonArray = weiXinHelper.getWeiXinUserInfo(weiXinUserList, token);
            if (Objects.nonNull(jsonArray)) {
                List<String> unionIdList = appUserWxmpList.stream()
                        .map(AppUserWxmp::getUnionId)
                        .collect(Collectors.toList());
                for (Object o : jsonArray) {
                    WechatUser wechatUser = JSONObject.parseObject(o.toString(), new TypeReference<WechatUser>() {
                    });
                    if (wechatUser.getSubscribe() == 1 && wechatUser.getUnionid() != null) {
                        AppUserWxmp appUserWxmp = new AppUserWxmp();
                        appUserWxmp.setUnionId(wechatUser.getUnionid());
                        appUserWxmp.setOpenId(wechatUser.getOpenid());
                        appUserWxmp.setSubscribe(wechatUser.getSubscribe());
                        appUserWxmp.setSubscribeTime(Long.valueOf(wechatUser.getSubscribe_time()));
                        appUserWxmp.setLanguage(wechatUser.getLanguage());
                        appUserWxmp.setRemark(wechatUser.getRemark());
                        appUserWxmp.setGroupId(wechatUser.getGroupid());
                        appUserWxmp.setTagidList(wechatUser.getTagid_list().toString());
                        appUserWxmp.setSubscribeScene(wechatUser.getSubscribe_scene());
                        appUserWxmp.setQrScene(Long.valueOf(wechatUser.getQr_scene()));
                        appUserWxmp.setQrSceneStr(wechatUser.getQr_scene_str());
                        if (!unionIdList.contains(wechatUser.getUnionid())) {
                            appUserWxmp.setCreateTime(LocalDateTime.now());
                            appUserWxmp.setUpdateTime(appUserWxmp.getCreateTime());
                            int insert = appUserWxmpMapper.insert(appUserWxmp);
                            i += insert;
                        }
//                        } else {
//                            appUserWxmp.setUpdateTime(LocalDateTime.now());
//                            int update = appUserWxmpMapper.update(appUserWxmp, new LambdaQueryWrapper<AppUserWxmp>()
//                                    .eq(AppUserWxmp::getUnionId, appUserWxmp.getUnionId()));
//                            u += update;
//                        }
                    }
                }
            }
        }
        log.info("获取公众号关注用户信息成功===>新增{}条", i);
        log.info("获取公众号关注用户信息===>结束");
        return Result.success("更新成功");
    }

    @Scheduled(fixedRate = 7190000)
    private void enable() {
        if (!enable) {
            return;
        }
        this.updateUserInfo();
    }
}