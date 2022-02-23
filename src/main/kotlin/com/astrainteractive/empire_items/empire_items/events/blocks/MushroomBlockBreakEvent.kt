package com.astrainteractive.empire_items.empire_items.events.blocks

import com.astrainteractive.astralibs.EventListener
import com.astrainteractive.empire_items.api.items.BlockParser
import com.astrainteractive.empire_items.api.items.data.ItemApi
import org.bukkit.event.EventHandler
import org.bukkit.event.block.BlockBreakEvent

class MushroomBlockBreakEvent: EventListener {
    public override fun onDisable(){
        BlockBreakEvent.getHandlerList().unregister(this)
    }
    @EventHandler
    fun blockBreak(e:BlockBreakEvent){
        if (e.isCancelled)
            return
        val player = e.player
        val block = e.block
        val data = BlockParser.getBlockData(block)?:return
        val id = ItemApi.getBlockInfoByData(data)?.id?:return
        e.isDropItems=false
    }
}