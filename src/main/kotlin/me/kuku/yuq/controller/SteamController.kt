package me.kuku.yuq.controller

import com.IceCreamQAQ.Yu.annotation.Action
import com.IceCreamQAQ.Yu.annotation.Before
import com.icecreamqaq.yuq.annotation.GroupController
import com.icecreamqaq.yuq.annotation.QMsg
import com.icecreamqaq.yuq.message.Message
import com.icecreamqaq.yuq.message.MessageItemFactory
import me.kuku.yuq.dao.SteamDao
import me.kuku.yuq.entity.SteamEntity
import me.kuku.yuq.logic.SteamLogic
import javax.inject.Inject

@GroupController
class SteamController{

    @Inject
    private lateinit var steamDao: SteamDao
    @Inject
    private lateinit var mif: MessageItemFactory
    @Inject
    private lateinit var steamLogic: SteamLogic

    @Before
    fun checkBind(qq: Long) = steamDao.findByQQ(qq) ?: throw mif.text("您还没有绑定steam账号").toMessage()

    @Action("更名")
    @QMsg(at = true)
    fun changeName(steamEntity: SteamEntity, message: Message): String{
        val list = message.toPath()
        return if (list.size > 1) {
            var name = ""
            for (i in 1 until list.size) name += list[i] + " "
            steamLogic.changeName(steamEntity, name.trim())
        }
        else "呀，缺少参数，您需要修改的名字"
    }

    @Action("buff")
    @QMsg(at = true)
    fun buff(steamEntity: SteamEntity): String{
        val commonResult = steamLogic.loginToBuff(steamEntity)
        return if (commonResult.code == 200){
            steamEntity.buffCookie = commonResult.t!!
            steamDao.saveOrUpdate(steamEntity)
            "绑定网易buff的cookie成功"
        }else commonResult.msg
    }

}