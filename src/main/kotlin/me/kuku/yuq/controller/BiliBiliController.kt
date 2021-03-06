package me.kuku.yuq.controller

import com.IceCreamQAQ.Yu.annotation.Action
import com.IceCreamQAQ.Yu.annotation.Before
import com.alibaba.fastjson.JSONObject
import com.icecreamqaq.yuq.annotation.GroupController
import com.icecreamqaq.yuq.annotation.PathVar
import com.icecreamqaq.yuq.annotation.QMsg
import com.icecreamqaq.yuq.controller.BotActionContext
import com.icecreamqaq.yuq.message.Message
import com.icecreamqaq.yuq.mif
import com.icecreamqaq.yuq.toMessage
import me.kuku.yuq.entity.BiliBiliEntity
import me.kuku.yuq.logic.BiliBiliLogic
import me.kuku.yuq.pojo.BiliBiliPojo
import me.kuku.yuq.pojo.CommonResult
import me.kuku.yuq.service.BiliBiliService
import me.kuku.yuq.utils.BotUtils
import me.kuku.yuq.utils.removeSuffixLine
import javax.inject.Inject

@GroupController
class BiliBiliController {
    @Inject
    private lateinit var biliBiliLogic: BiliBiliLogic
    @Inject
    private lateinit var biliBiliService: BiliBiliService

    @Before
    fun before(qq: Long, message: Message, actionContext: BotActionContext, group: Long){
        val whiteList = arrayOf("bilibili")
        val list = message.toPath()
        if (list.isNotEmpty() && whiteList.contains(list[0])){
            return
        }
        var biliBiliEntity = biliBiliService.findByQQ(qq)
        if (biliBiliEntity == null && list[0] == "哔哩哔哩开播提醒")
            biliBiliEntity = BiliBiliEntity(null, qq, group)
        else if (biliBiliEntity == null || biliBiliEntity.cookie == "") throw mif.at(qq).plus("您还没有绑定哔哩哔哩账号，无法继续！！！")
        actionContext["biliBiliEntity"] = biliBiliEntity
    }

    @Action("bilibili {username}")
    fun searchDynamic(username: String, @PathVar(value = 2, type = PathVar.Type.Integer) num: Int?, qq: Long): Message{
        val commonResult = this.queryDynamic(username, num)
        val biliBiliPojo = commonResult.t ?: return mif.at(qq).plus(commonResult.msg)
        return biliBiliLogic.convertStr(biliBiliPojo).toMessage()
    }

    @Action("bilibilimy")
    fun searchMyFriendDynamic(biliBiliEntity: BiliBiliEntity, @PathVar(value = 1, type = PathVar.Type.Integer) num :Int?, qq: Long): Message{
        val commonResult = biliBiliLogic.getFriendDynamic(biliBiliEntity)
        val list = commonResult.t ?: return mif.at(qq).plus(commonResult.msg)
        if (list.isEmpty()) return mif.at(qq).plus("您的好友没有任何动态呢！！")
        val newNum = this.parseNum(list, num)
        return biliBiliLogic.convertStr(list[newNum - 1]).toMessage()
    }

    @Action("哔哩哔哩关注监控 {status}")
    @QMsg(at = true)
    fun biliBiliMonitor(biliBiliEntity: BiliBiliEntity, status: Boolean): String{
        biliBiliEntity.monitor = status
        biliBiliService.save(biliBiliEntity)
        return "哔哩哔哩我的关注监控已${if (status) "开启" else "关闭"}"
    }

    @Action("哔哩哔哩开播提醒 {username}")
    @QMsg(at = true)
    fun biliBiliLive(username: String, biliBiliEntity: BiliBiliEntity): String{
        val commonResult = this.searchToJsonObject(username)
        val jsonObject = commonResult.t ?: return commonResult.msg
        val liveJsonArray = biliBiliEntity.getLiveJsonArray()
        liveJsonArray.add(jsonObject)
        biliBiliEntity.liveList = liveJsonArray.toString()
        biliBiliService.save(biliBiliEntity)
        return "添加用户[${jsonObject["name"]}]的开播提醒成功！！"
    }

    @Action("查哔哩哔哩开播提醒")
    @QMsg(at = true)
    fun queryBiliBiliLive(biliBiliEntity: BiliBiliEntity): String{
        val liveJsonArray = biliBiliEntity.getLiveJsonArray()
        val sb = StringBuilder().appendln("您的开播提醒的用户如下：")
        liveJsonArray.forEach {
            val jsonObject = it as JSONObject
            sb.appendln("${jsonObject["name"]}-${jsonObject["id"]}")
        }
        return sb.removeSuffixLine().toString()
    }

    @Action("删哔哩哔哩开播提醒 {username}")
    @QMsg(at = true)
    fun delBiliBiliLive(biliBiliEntity: BiliBiliEntity, username: String): String{
        val liveJsonArray = biliBiliEntity.getLiveJsonArray()
        val list = BotUtils.delMonitorList(liveJsonArray, username)
        list.forEach { liveJsonArray.remove(it) }
        biliBiliEntity.liveList = liveJsonArray.toString()
        biliBiliService.save(biliBiliEntity)
        return "删除用户[$username]的开播提醒成功！！"
    }

    @Action("哔哩哔哩直播签到")
    @QMsg(at = true)
    fun biliBiliLiveSign(biliBiliEntity: BiliBiliEntity) = biliBiliLogic.liveSign(biliBiliEntity)

    private fun parseNum(list: List<*>, num: Int?): Int{
        var newNum = num ?: 1
        if (newNum > list.size - 1) newNum = 1
        return newNum
    }

    private fun queryDynamic(username: String, num: Int?): CommonResult<BiliBiliPojo>{
        val idResult = biliBiliLogic.getIdByName(username)
        val idList = idResult.t ?: return CommonResult(500, idResult.msg)
        val dynamicResult = biliBiliLogic.getDynamicById(idList[0].userId)
        val biliBiliList = dynamicResult.t ?: return CommonResult(500, dynamicResult.msg)
        if (biliBiliList.isEmpty()) return CommonResult(500, "这个用户没有发现动态哦！！")
        var newNum = num ?: 1
        if (newNum > biliBiliList.size) newNum = 1
        return CommonResult(200, "", biliBiliList[newNum - 1])
    }

    private fun searchToJsonObject(username: String): CommonResult<JSONObject>{
        val commonResult = biliBiliLogic.getIdByName(username)
        val list = commonResult.t ?: return CommonResult(500, commonResult.msg)
        val biliBiliPojo = list[0]
        val jsonObject = JSONObject()
        jsonObject["id"] = biliBiliPojo.userId
        jsonObject["name"] = biliBiliPojo.name
        return CommonResult(200, "", jsonObject)
    }
}