package com.chunfeng.local.model.weixin;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author 13994
 */
@Data
@TableName("app_user_wxmp")
public class AppUserWxmp {

    @TableId("id")
    private Long id;
    /**
     * 只有在用户将公众号绑定到微信开放平台账号后，才会出现该字段。
     */
    @TableField("union_id")
    private String unionId;

    /**
     * 用户的标识，对当前公众号唯一
     */
    @TableField("open_id")
    private String openId;

    /**
     * 用户是否订阅该公众号标识，值为0时，代表此用户没有关注该公众号，拉取不到其余信息。
     */
    @TableField("subscribe")
    private Integer subscribe;

    /**
     * 用户关注时间，为时间戳。如果用户曾多次关注，则取最后关注时间
     */
    @TableField("subscribe_time")
    private Long subscribeTime;

    /**
     * 用户的语言，简体中文为zh_CN
     */
    @TableField("language")
    private String language;

    /**
     * 公众号运营者对粉丝的备注，公众号运营者可在微信公众平台用户管理界面对粉丝添加备注
     */
    @TableField("remark")
    private String remark;

    /**
     * 用户所在的分组ID（暂时兼容用户分组旧接口）
     */
    @TableField("group_id")
    private Integer groupId;

    /**
     * 用户被打上的标签ID列表
     */
    @TableField("tagid_list")
    private String tagidList;

    /**
     * 返回用户关注的渠道来源，ADD_SCENE_SEARCH 公众号搜索，
     * ADD_SCENE_ACCOUNT_MIGRATION 公众号迁移，
     * ADD_SCENE_PROFILE_CARD 名片分享，
     * ADD_SCENE_QR_CODE 扫描二维码，
     * ADD_SCENE_PROFILE_LINK 图文页内名称点击，
     * ADD_SCENE_PROFILE_ITEM 图文页右上角菜单，
     * ADD_SCENE_PAID 支付后关注，
     * ADD_SCENE_WECHAT_ADVERTISEMENT 微信广告，
     * ADD_SCENE_REPRINT 他人转载 ，
     * ADD_SCENE_LIVESTREAM 视频号直播，
     * ADD_SCENE_CHANNELS 视频号,
     * ADD_SCENE_OTHERS 其他
     */
    @TableField("subscribe_scene")
    private String subscribeScene;

    /**
     * 二维码扫码场景（开发者自定义）
     */
    @TableField("qr_scene")
    private Long qrScene;

    /**
     * 二维码扫码场景描述（开发者自定义）
     */
    @TableField("qr_scene_str")
    private String qrSceneStr;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}