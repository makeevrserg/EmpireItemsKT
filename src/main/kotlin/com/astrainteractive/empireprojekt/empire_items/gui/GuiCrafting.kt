package com.astrainteractive.empireprojekt.empire_items.gui

import com.astrainteractive.empireprojekt.empire_items.api.drop.DropManager
import com.astrainteractive.astralibs.HEX
import com.astrainteractive.empireprojekt.empire_items.gui.data.GuiConfig
import com.astrainteractive.astralibs.menu.AstraMenuSize
import com.astrainteractive.astralibs.runAsyncTask
import com.astrainteractive.empireprojekt.EmpirePlugin
import com.astrainteractive.empireprojekt.empire_items.api.crafting.CraftingManager
import com.astrainteractive.empireprojekt.empire_items.api.items.data.ItemManager
import com.astrainteractive.empireprojekt.empire_items.api.items.data.ItemManager.getAstraID
import com.astrainteractive.empireprojekt.empire_items.api.items.data.ItemManager.toAstraItemOrItem
import com.astrainteractive.empireprojekt.empire_items.api.upgrade.UpgradeManager
import com.astrainteractive.empireprojekt.empire_items.api.utils.setDisplayName
import com.astrainteractive.empireprojekt.empire_items.api.v_trades.AstraVillagerTrade
import com.astrainteractive.empireprojekt.empire_items.api.v_trades.VillagerTradeManager
import com.astrainteractive.empireprojekt.empire_items.util.EmpirePermissions
import org.bukkit.ChatColor
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.*

class GuiCrafting(playerMenuUtility: PlayerMenuUtility) :
    AstraPaginatedMenu() {

    val guiSettings = GuiConfig.getGuiConfig()
    val itemID = playerMenuUtility.prevItems.last()
    val recipes = ItemManager.getItemRecipes(itemID)
    val usedInCraftIDS = CraftingManager.usedInCraft(itemID)
    val usedInCraftItemStacks = usedInCraftIDS.map { it.toAstraItemOrItem() }


    override var menuName: String = guiSettings.settings.workbenchText +  (itemID.toAstraItemOrItem()?.itemMeta?.displayName ?: "Крафтинг")

    override val menuSize: AstraMenuSize = AstraMenuSize.XL
    override val playerMenuUtility: PlayerMenuUtility = playerMenuUtility
    override val backPageButton: ItemStack = guiSettings.settings.backButton
    override val maxItemsPerPage = 9
    override val maxItemsAmount: Int = usedInCraftItemStacks.size
    override val nextPageButton: ItemStack = guiSettings.settings.nextButton
    override var page: Int = playerMenuUtility.craftingPage
    override val prevPageButton: ItemStack = guiSettings.settings.prevButton
    var currentRecipe = 0
    override fun loadPage(next: Int) {
        super.loadPage(next)
        playerMenuUtility.craftingPage += next
    }

    override fun handleMenu(e: InventoryClickEvent) {
        super.handleMenu(e)
        when (e.slot) {
            backButtonIndex -> {
                runAsyncTask {
                    playerMenuUtility.prevItems.removeLast()
                    if (playerMenuUtility.prevItems.isEmpty())
                        GuiCategory(playerMenuUtility).open()
                    else GuiCrafting(playerMenuUtility).open()
                }
            }
            8 -> {
                if (e.isRightClick)
                    currentRecipe++
                else
                    currentRecipe--
                clearMenu()
                setMenuItems()
            }
            11, 12, 13, 20, 21, 22, 29, 30, 31 -> {
                val itemStack = inventory.getItem(e.slot)
                playerMenuUtility.prevItems.add(itemStack?.getAstraID() ?: itemStack?.type?.name ?: return)
                GuiCrafting(playerMenuUtility).open()
            }

            36, 37, 38, 39, 40, 41, 42, 43, 44 -> {
                val itemStack = inventory.getItem(e.slot)
                playerMenuUtility.prevItems.add(itemStack?.getAstraID() ?: itemStack?.type?.name ?: return)
                GuiCrafting(playerMenuUtility).open()
            }
            34 -> {
                if (playerMenuUtility.player.hasPermission(EmpirePermissions.EMPGIVE))
                    playerMenuUtility.player.inventory.addItem(itemID.toAstraItemOrItem() ?: return)
            }
        }
    }


    fun setCraftingTable(recipe: ShapedRecipe) {
        var invPos = 11
        inventory.setItem(invPos++, recipe.ingredientMap[recipe.shape.getOrNull(0)?.getOrNull(0)])
        inventory.setItem(invPos++, recipe.ingredientMap[recipe.shape.getOrNull(0)?.getOrNull(1)])
        inventory.setItem(invPos++, recipe.ingredientMap[recipe.shape.getOrNull(0)?.getOrNull(2)])
        invPos += 9 - 3
        inventory.setItem(invPos++, recipe.ingredientMap[recipe.shape.getOrNull(1)?.getOrNull(0)])
        inventory.setItem(invPos++, recipe.ingredientMap[recipe.shape.getOrNull(1)?.getOrNull(1)])
        inventory.setItem(invPos++, recipe.ingredientMap[recipe.shape.getOrNull(1)?.getOrNull(2)])
        invPos += 9 - 3
        inventory.setItem(invPos++, recipe.ingredientMap[recipe.shape.getOrNull(2)?.getOrNull(0)])
        inventory.setItem(invPos++, recipe.ingredientMap[recipe.shape.getOrNull(2)?.getOrNull(1)])
        inventory.setItem(invPos++, recipe.ingredientMap[recipe.shape.getOrNull(2)?.getOrNull(2)])
        inventory.setItem(25,inventory.getItem(25).apply { this?.amount = recipe.result.amount })
        recipeType="Верстак"
    }

    fun setFurnaceRecipe(recipe: FurnaceRecipe) {
        inventory.setItem(21, recipe.input)
        inventory.setItem(25,inventory.getItem(25).apply { this?.amount = recipe.result.amount })
        recipeType="Печь"
    }

    fun setShapelessRecipe(recipe: ShapelessRecipe) {

        inventory.setItem(11, recipe.ingredientList.getOrNull(0))
        inventory.setItem(12, recipe.ingredientList.getOrNull(1))
        inventory.setItem(13, recipe.ingredientList.getOrNull(2))

        inventory.setItem(21, recipe.ingredientList.getOrNull(3))
        inventory.setItem(22, recipe.ingredientList.getOrNull(4))
        inventory.setItem(23, recipe.ingredientList.getOrNull(5))

        inventory.setItem(32, recipe.ingredientList.getOrNull(6))
        inventory.setItem(33, recipe.ingredientList.getOrNull(7))
        inventory.setItem(34, recipe.ingredientList.getOrNull(8))
        inventory.setItem(25,inventory.getItem(25).apply { this?.amount = recipe.result.amount })
        recipeType="Верстак"
    }

    var recipeType="Верстак"
    fun setRecipe() {
        if (currentRecipe >= recipes?.size ?: return)
            currentRecipe = 0
        else if (currentRecipe < 0)
            currentRecipe = recipes.size - 1

        val recipe = recipes[currentRecipe]
        if (recipe is ShapedRecipe)
            setCraftingTable(recipe)
        if (recipe is FurnaceRecipe)
            setFurnaceRecipe(recipe)
        if (recipe is ShapelessRecipe)
            setShapelessRecipe(recipe)
    }

    fun clearMenu() {
        inventory.clear()
    }
    fun setVillagerInfo(){
        val v = VillagerTradeManager.villagerTrades.mapNotNull {
            val filtered = it.trades.filter { it.resultItem.id==itemID }
            if (filtered.isEmpty())
                null
            else
            AstraVillagerTrade(it.profession,filtered)
        }
        if (v.isEmpty())
            return
        val item = guiSettings.settings.moreButton.clone().apply {
            val meta = itemMeta!!
            meta.setDisplayName((EmpirePlugin.translations.ITEM_INFO_DROP_COLOR+"Можно купить у жителя:").HEX())
            meta.lore = v.map { "${ChatColor.GRAY}${it.profession}" }
            itemMeta = meta
        }
        inventory.setItem(backButtonIndex-2,item)
    }
    fun setUpgradeInfo(){
        val u = UpgradeManager.getAvailableUpgradesForItemStack(itemID.toAstraItemOrItem()?:return)
        if (u.isEmpty())
            return
        val item = guiSettings.settings.moreButton.clone().apply {
            val meta = itemMeta!!
            meta.setDisplayName((EmpirePlugin.translations.ITEM_INFO_DROP_COLOR+"Улучшает:").HEX())
            meta.lore = u.map { "${ChatColor.GRAY}${UpgradeManager.attrMap[it.attribute.name]} [${it.addMin};${it.addMax}]" }
            itemMeta = meta
        }
        inventory.setItem(backButtonIndex+2,item)

    }
    fun setBlockInfo(){
        val b = ItemManager.getItemInfo(itemID)?.block?.generate?:return
        val item = guiSettings.settings.moreButton.clone().apply {
            val meta = itemMeta!!
            meta.setDisplayName((EmpirePlugin.translations.ITEM_INFO_DROP_COLOR+"Генерируется:").HEX())
            meta.lore = listOf(
            ("${ChatColor.GRAY}На высоте [${b.minY};${b.maxY}]"),
            ("${ChatColor.GRAY}Количество в чанке [${b.minPerChunk};${b.maxPerChunk}]"),
            ("${ChatColor.GRAY}Количество в депозите [${b.minPerDeposit};${b.maxPerDeposit}]"),
            ("${ChatColor.GRAY}В мире: ${b.world ?: "любом"}"),
            ("${ChatColor.GRAY}С шансом: ${b.generateInChunkChance}%"))
            itemMeta = meta
        }
        inventory.setItem(backButtonIndex+1,item)
    }
    fun setDropInfo(){
        val drops = DropManager.getDropsById(itemID)
        if (drops.isEmpty())
            return
        val item = guiSettings.settings.moreButton.clone().apply {
            val meta = itemMeta!!
            meta.setDisplayName((EmpirePlugin.translations.ITEM_INFO_DROP_COLOR+EmpirePlugin.translations.ITEM_INFO_DROP).HEX())
            meta.lore =  drops.map { "${ChatColor.GRAY}${it.dropFrom}: [${it.minAmount};${it.maxAmount}] ${it.chance}%" }
            itemMeta = meta
        }
        inventory.setItem(backButtonIndex-1,item)
    }
    override fun setMenuItems() {
        addManageButtons()
        for (i in 36 until 36 + 9) {
            val index = getIndex(i - 36)
            inventory.setItem(i, usedInCraftItemStacks.getOrNull(index))
        }
        setDropInfo()
        setBlockInfo()
        setVillagerInfo()
        setUpgradeInfo()
        inventory.setItem(25, itemID.toAstraItemOrItem())
        setRecipe()
        if (recipes?.isNotEmpty() == true)
            inventory.setItem(8, guiSettings.settings.craftingTableButton.apply { setDisplayName(recipeType)})
        inventory.setItem(34, guiSettings.settings.giveButton)


    }
}