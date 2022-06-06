package com.idormy.sms.forwarder.utils


import android.text.TextUtils
import android.util.Base64
import com.google.gson.Gson
import com.idormy.sms.forwarder.core.Core
import com.idormy.sms.forwarder.entity.CloneInfo
import com.idormy.sms.forwarder.server.model.BaseRequest
import com.xuexiang.xutil.app.AppUtils
import com.yanzhenjie.andserver.error.HttpException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * HttpServer工具类
 */
class HttpServerUtils private constructor() {

    companion object {

        //是否启用HttpServer开机自启
        @JvmStatic
        var enableServerAutorun: Boolean
            get() = MMKVUtils.getBoolean(SP_ENABLE_SERVER_AUTORUN, false)
            set(enableServerAutorun) {
                MMKVUtils.put(SP_ENABLE_SERVER_AUTORUN, enableServerAutorun)
            }

        //服务端签名密钥
        @JvmStatic
        var serverSignKey: String?
            get() = MMKVUtils.getString(SP_SERVER_SIGN_KEY, "")
            set(serverSignKey) {
                MMKVUtils.put(SP_SERVER_SIGN_KEY, serverSignKey)
            }

        //服务地址
        @JvmStatic
        var serverAddress: String?
            get() = MMKVUtils.getString(SP_SERVER_ADDRESS, "")
            set(clientSignKey) {
                MMKVUtils.put(SP_SERVER_ADDRESS, clientSignKey)
            }

        //客户端签名密钥
        @JvmStatic
        var clientSignKey: String?
            get() = MMKVUtils.getString(SP_CLIENT_SIGN_KEY, "")
            set(clientSignKey) {
                MMKVUtils.put(SP_CLIENT_SIGN_KEY, clientSignKey)
            }

        //是否启用一键克隆
        @JvmStatic
        var enableApiClone: Boolean
            get() = MMKVUtils.getBoolean(SP_ENABLE_API_CLONE, false)
            set(enableApiClone) {
                MMKVUtils.put(SP_ENABLE_API_CLONE, enableApiClone)
            }

        //是否启用远程发短信
        @JvmStatic
        var enableApiSmsSend: Boolean
            get() = MMKVUtils.getBoolean(SP_ENABLE_API_SMS_SEND, false)
            set(enableApiSendSms) {
                MMKVUtils.put(SP_ENABLE_API_SMS_SEND, enableApiSendSms)
            }

        //是否启用远程查短信
        @JvmStatic
        var enableApiSmsQuery: Boolean
            get() = MMKVUtils.getBoolean(SP_ENABLE_API_SMS_QUERY, false)
            set(enableApiQuerySms) {
                MMKVUtils.put(SP_ENABLE_API_SMS_QUERY, enableApiQuerySms)
            }

        //是否启用远程查通话
        @JvmStatic
        var enableApiCallQuery: Boolean
            get() = MMKVUtils.getBoolean(SP_ENABLE_API_CALL_QUERY, false)
            set(enableApiQueryCall) {
                MMKVUtils.put(SP_ENABLE_API_CALL_QUERY, enableApiQueryCall)
            }

        //是否启用远程查话簿
        @JvmStatic
        var enableApiContactQuery: Boolean
            get() = MMKVUtils.getBoolean(SP_ENABLE_API_CONTACT_QUERY, false)
            set(enableApiQueryLinkman) {
                MMKVUtils.put(SP_ENABLE_API_CONTACT_QUERY, enableApiQueryLinkman)
            }

        //是否启用远程查电量
        @JvmStatic
        var enableApiBatteryQuery: Boolean
            get() = MMKVUtils.getBoolean(SP_ENABLE_API_BATTERY_QUERY, false)
            set(enableApiQueryBattery) {
                MMKVUtils.put(SP_ENABLE_API_BATTERY_QUERY, enableApiQueryBattery)
            }

        //计算签名
        fun calcSign(timestamp: String, signSecret: String): String {
            val stringToSign = "$timestamp\n" + signSecret
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(signSecret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
            val signData = mac.doFinal(stringToSign.toByteArray(StandardCharsets.UTF_8))
            return URLEncoder.encode(String(Base64.encode(signData, Base64.NO_WRAP)), "UTF-8")
        }

        //校验签名
        @Throws(HttpException::class)
        fun checkSign(req: BaseRequest<*>) {
            val signSecret = serverSignKey
            if (TextUtils.isEmpty(signSecret)) return

            if (TextUtils.isEmpty(req.sign)) throw HttpException(500, "服务端启用签名密钥，sign节点必传")
            if (req.timestamp == 0L) throw HttpException(500, "服务端启用签名密钥，timestamp节点必传")

            val timestamp = System.currentTimeMillis()
            val diffTime = kotlin.math.abs(timestamp - req.timestamp)
            if (diffTime > 3600000L) {
                throw HttpException(500, "timestamp校验失败，与服务器时间($timestamp)误差不能超过1小时(diffTime=$diffTime)")
            }

            val sign = calcSign(req.timestamp.toString(), signSecret.toString())
            if (sign != req.sign) throw HttpException(500, "签名校验失败")
        }

        //判断版本是否一致
        @Throws(HttpException::class)
        fun compareVersion(cloneInfo: CloneInfo) {
            val versionCodeRequest = cloneInfo.versionCode
            if (versionCodeRequest == 0) throw HttpException(500, "version_code节点必传")
            val versionCodeLocal = AppUtils.getAppVersionCode().toString().substring(1)
            if (!versionCodeRequest.toString().endsWith(versionCodeLocal)) throw HttpException(500, "客户端与服务端的App版本不一致")
        }

        //导出设置
        fun exportSettings(): CloneInfo {
            val cloneInfo = CloneInfo()
            cloneInfo.versionCode = AppUtils.getAppVersionCode()
            cloneInfo.versionName = AppUtils.getAppVersionName()
            cloneInfo.enableSms = SettingUtils.enableSms
            cloneInfo.enablePhone = SettingUtils.enablePhone
            cloneInfo.callType1 = SettingUtils.enableCallType1
            cloneInfo.callType2 = SettingUtils.enableCallType2
            cloneInfo.callType3 = SettingUtils.enableCallType3
            cloneInfo.enableAppNotify = SettingUtils.enableAppNotify
            cloneInfo.cancelAppNotify = SettingUtils.enableCancelAppNotify
            cloneInfo.enableNotUserPresent = SettingUtils.enableNotUserPresent
            cloneInfo.duplicateMessagesLimits = SettingUtils.duplicateMessagesLimits
            cloneInfo.enableBatteryReceiver = SettingUtils.enableBatteryReceiver
            cloneInfo.batteryLevelMin = SettingUtils.batteryLevelMin
            cloneInfo.batteryLevelMax = SettingUtils.batteryLevelMax
            cloneInfo.batteryLevelOnce = SettingUtils.batteryLevelOnce
            cloneInfo.enableBatteryCron = SettingUtils.enableBatteryCron
            cloneInfo.batteryCronStartTime = SettingUtils.batteryCronStartTime
            cloneInfo.batteryCronInterval = SettingUtils.batteryCronInterval
            cloneInfo.enableExcludeFromRecents = SettingUtils.enableExcludeFromRecents
            cloneInfo.enablePlaySilenceMusic = SettingUtils.enablePlaySilenceMusic
            cloneInfo.requestRetryTimes = SettingUtils.requestRetryTimes
            cloneInfo.requestDelayTime = SettingUtils.requestDelayTime
            cloneInfo.requestTimeout = SettingUtils.requestTimeout
            cloneInfo.notifyContent = SettingUtils.notifyContent
            cloneInfo.enableSmsTemplate = SettingUtils.enableSmsTemplate
            cloneInfo.smsTemplate = SettingUtils.smsTemplate
            cloneInfo.enableHelpTip = SettingUtils.enableHelpTip
            cloneInfo.senderList = Core.sender.all
            cloneInfo.ruleList = Core.rule.all

            return cloneInfo
        }

        //还原设置
        fun restoreSettings(cloneInfo: CloneInfo): Boolean {
            return try {
                //应用配置
                SettingUtils.enableSms = cloneInfo.enableSms
                SettingUtils.enablePhone = cloneInfo.enablePhone
                SettingUtils.enableCallType1 = cloneInfo.callType1
                SettingUtils.enableCallType2 = cloneInfo.callType2
                SettingUtils.enableCallType3 = cloneInfo.callType3
                SettingUtils.enableAppNotify = cloneInfo.enableAppNotify
                SettingUtils.enableCancelAppNotify = cloneInfo.cancelAppNotify
                SettingUtils.enableNotUserPresent = cloneInfo.enableNotUserPresent
                SettingUtils.duplicateMessagesLimits = cloneInfo.duplicateMessagesLimits
                SettingUtils.enableBatteryReceiver = cloneInfo.enableBatteryReceiver
                SettingUtils.batteryLevelMin = cloneInfo.batteryLevelMin
                SettingUtils.batteryLevelMax = cloneInfo.batteryLevelMax
                SettingUtils.batteryLevelOnce = cloneInfo.batteryLevelOnce
                SettingUtils.enableBatteryCron = cloneInfo.enableBatteryCron
                SettingUtils.batteryCronStartTime = cloneInfo.batteryCronStartTime
                SettingUtils.batteryCronInterval = cloneInfo.batteryCronInterval
                SettingUtils.enableExcludeFromRecents = cloneInfo.enableExcludeFromRecents
                SettingUtils.enablePlaySilenceMusic = cloneInfo.enablePlaySilenceMusic
                SettingUtils.requestRetryTimes = cloneInfo.requestRetryTimes
                SettingUtils.requestDelayTime = cloneInfo.requestDelayTime
                SettingUtils.requestTimeout = cloneInfo.requestTimeout
                SettingUtils.notifyContent = cloneInfo.notifyContent
                SettingUtils.enableSmsTemplate = cloneInfo.enableSmsTemplate
                SettingUtils.smsTemplate = cloneInfo.smsTemplate
                SettingUtils.enableHelpTip = cloneInfo.enableHelpTip
                //删除发送通道、转发规则、转发日志
                Core.sender.deleteAll()
                //发送通道
                for (sender in cloneInfo.senderList!!) {
                    Core.sender.insert(sender)
                }
                //转发规则
                for (rule in cloneInfo.ruleList!!) {
                    Core.rule.insert(rule)
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                throw HttpException(500, e.message)
                //false
            }
        }

        //返回统一结构报文
        fun response(output: Any?): String {
            val resp: MutableMap<String, Any> = mutableMapOf()
            if (output is String && output != "success") {
                resp["code"] = HTTP_FAILURE_CODE
                resp["msg"] = output
            } else {
                resp["code"] = HTTP_SUCCESS_CODE
                resp["msg"] = "success"
                if (output != null) resp["data"] = output
            }

            val timestamp = System.currentTimeMillis()
            resp["timestamp"] = timestamp
            if (!TextUtils.isEmpty(serverSignKey)) {
                resp["sign"] = calcSign(timestamp.toString(), serverSignKey.toString())
            }

            return Gson().toJson(resp)
        }
    }
}