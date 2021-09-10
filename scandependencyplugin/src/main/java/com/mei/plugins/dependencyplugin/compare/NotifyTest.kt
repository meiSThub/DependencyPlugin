package com.mei.plugins.dependencyplugin.compare

import com.google.gson.Gson
import okhttp3.*
import java.io.IOException


/**
 * @date 2021/8/24
 * @author mxb
 * @desc 通知测试，依赖库有更新
 * @desired
 */
class NotifyTest {

    companion object {
        /**
         * 默认webhook url，机器人发送消息的url
         */
        const val DEFAULT_WEBHOOK_URL =
            "https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=1460ba40-cfca-4b88-8e70-2f6c588f2a80"
    }

    /**
     * 往测试群推送消息
     *
     * @param diffDepInfo 依赖库的版本更新信息
     */
    fun postMessage(diffDepInfo: String, webhookUrl: String?) {
        println("NotifyTest,推送消息start...")
        val paramJson = Gson().toJson(buildMsgBody(diffDepInfo))
        println("paramJson=$paramJson")
        val requestBody = RequestBody.create(MediaType.parse("application/json;charset=utf-8"),
            paramJson)
        val request = Request.Builder()
            .url(webhookUrl ?: DEFAULT_WEBHOOK_URL)
            .post(requestBody)
            .build()
        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                println("NotifyTest,版本更新消息推送$e")
            }

            override fun onResponse(call: Call, response: Response) {
                println("NotifyTest,版本更新消息推送${if (response.isSuccessful) "成功" else "失败"}")
            }
        })
    }

    /**
     * 构建推送消息实体
     *
     * @param list
     */
    private fun buildMsgBody(diffDepInfo: String): MessageInfo {
        println("NotifyTest,buildMsgBody:$diffDepInfo")
        return MessageInfo("text", MsgContent(diffDepInfo, arrayListOf("@all")))
    }
}

/**
 * 发送消息的消息体
 *
 * @property msgtype 消息类型
 * @property text 消息内容
 */
data class MessageInfo(
    var msgtype: String = "text",

    var text: MsgContent? = null,
)

class MsgContent(
    /**
     * 文本内容，最长不超过2048个字节，必须是utf8编码
     */
    var content: String = "",
    /**
     * userid的列表，提醒群中的指定成员(@某个成员)，@all表示提醒所有人，如果开发者获取不到userid，可以使用mentioned_mobile_list
     */
    var mentioned_list: MutableList<String>? = null,
    /**
     * 	手机号列表，提醒手机号对应的群成员(@某个成员)，@all表示提醒所有人
     */
    var mentioned_mobile_list: MutableList<String>? = null,
)
