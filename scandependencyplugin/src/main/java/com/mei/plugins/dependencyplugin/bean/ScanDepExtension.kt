package com.mei.plugins.dependencyplugin.bean

/**
 * @date 2021/8/23
 * @author mxb
 * @desc  扫描依赖的扩展
 * @desired
 */
open class ScanDepExtension(
    /**
     * 依赖收集到之后，上传到指定的git工程中去
     */
    var gitUrl: String? = null,
    /**
     * 依赖等级转换关系
     */
    var levelMap: MutableMap<Int, MutableList<String>>? = null,

    /**
     * 是否允许推送消息
     */
    var postMessageEnable: Boolean = false,

    /**
     * 给企业微信机器人推送消息的url，需要手动申请
     */
    var webhookUrl: String? = null,
)