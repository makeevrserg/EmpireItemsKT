package com.makeevrserg.empireprojekt.ESSENTIALS.NPCS.interact

import com.makeevrserg.empireprojekt.ESSENTIALS.NPCS.EmpireNPC
import net.minecraft.server.level.EntityPlayer
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class RightClickNPC(val player: Player, val npc: EmpireNPC) : Event(), Cancellable {


    private var isCancelled = false
    override fun isCancelled(): Boolean {
        return isCancelled
    }

    override fun setCancelled(cancel: Boolean) {
        isCancelled = cancel
    }

    override fun getHandlers(): HandlerList {
        return HANDLERS
    }
    companion object {
        val HANDLERS = HandlerList()
        @JvmStatic
        fun getHandlerList(): HandlerList = HANDLERS
    }
}








