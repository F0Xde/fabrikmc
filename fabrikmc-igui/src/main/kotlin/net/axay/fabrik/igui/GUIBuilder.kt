@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package net.axay.fabrik.igui

import net.axay.fabrik.igui.elements.*
import net.minecraft.item.ItemStack
import kotlin.math.absoluteValue

fun igui(
    type: GUIType,
    guiCreator: GUICreator = IndividualGUICreator(),
    builder: GUIBuilder.() -> Unit,
) = GUIBuilder(type, guiCreator).apply(builder).build()

class GUIBuilder(
    val type: GUIType,
    private val guiCreator: GUICreator
) {

    /**
     * The title of this GUI.
     * This title will be visible for every page of
     * this GUI.
     */
    var title: String = ""

    /**
     * The transition applied, if another GUI redirects to
     * this GUI.
     */
    var transitionTo: InventoryChangeEffect? = null

    /**
     * The transition applied, if this GUI redirects to
     * another GUI and the other GUI has no transitionTo
     * value defined.
     */
    var transitionFrom: InventoryChangeEffect? = null

    /**
     * The default page will be loaded first for every
     * GUI instance.
     */
    var defaultPage = 1

    private val guiSlots = HashMap<Int, GUIPage>()

    private var onClickElement: ((GUIClickEvent) -> Unit)? = null

    /**
     * Opens the builder for a new page and adds
     * the new page to the GUI.
     * @param page The index of the page.
     */
    fun page(page: Int, builder: GUIPageBuilder.() -> Unit) {
        guiSlots[page] = GUIPageBuilder(type, page).apply(builder).build()
    }

    /**
     * A callback executed when the user clicks on
     * any GUI elements on any page in this GUI.
     */
    fun onClickElement(onClick: (GUIClickEvent) -> Unit) {
        onClickElement = onClick
    }

    internal fun build() = guiCreator.createInstance(
        GUIData(type, title, guiSlots, defaultPage, transitionTo, transitionFrom, onClickElement)
    )

}

class GUIPageBuilder(
    private val type: GUIType,
    val page: Int
) {

    private val guiSlots = HashMap<Int, GUISlot>()

    var transitionTo: PageChangeEffect? = null
    var transitionFrom: PageChangeEffect? = null

    internal fun build() = GUIPage(page, guiSlots, transitionTo, transitionFrom)

    private fun defineSlots(slots: InventorySlotCompound, element: GUISlot) =
        slots.withInvType(type).forEach { curSlot ->
            curSlot.realSlotIn(type.dimensions)?.let { guiSlots[it] = element }
        }

    /**
     * A button is an item protected from any player
     * actions. If clicked, the specified [onClick]
     * function is invoked.
     */
    fun button(slots: InventorySlotCompound, itemStack: ItemStack, onClick: (GUIClickEvent) -> Unit) =
        defineSlots(slots, GUIButton(itemStack, onClick))

    /**
     * An item protected from any player actions.
     * This is not a button.
     */
    fun placeholder(slots: InventorySlotCompound, itemStack: ItemStack) =
        defineSlots(slots, GUIPlaceholder(itemStack))

    /**
     * A free slot does not block any player actions.
     * The player can put items in this slot or take
     * items out of it.
     */
    fun freeSlot(slots: InventorySlotCompound) = defineSlots(slots, GUIFreeSlot())

    /**
     * This is a button which loads the specified
     * [toPage] if clicked.
     */
    fun pageChanger(
        slots: InventorySlotCompound,
        icon: ItemStack,
        toPage: Int,
        onChange: ((GUIClickEvent) -> Unit)? = null
    ) = defineSlots(
        slots, GUIButtonPageChange(
            icon,
            GUIPageChangeCalculator.GUIConsistentPageCalculator(toPage),
            onChange
        )
    )

    /**
     * This button always tries to find the previous
     * page if clicked, and if a previous page
     * exists it is loaded.
     */
    fun previousPage(
        slots: InventorySlotCompound,
        icon: ItemStack,
        onChange: ((GUIClickEvent) -> Unit)? = null
    ) = defineSlots(
        slots, GUIButtonPageChange(
            icon,
            GUIPageChangeCalculator.GUIPreviousPageCalculator,
            onChange
        )
    )

    /**
     * This button always tries to find the next
     * page if clicked, and if a next page
     * exists it is loaded.
     */
    fun nextPage(
        slots: InventorySlotCompound,
        icon: ItemStack,
        onChange: ((GUIClickEvent) -> Unit)? = null
    ) = defineSlots(
        slots, GUIButtonPageChange(
            icon,
            GUIPageChangeCalculator.GUINextPageCalculator,
            onChange
        )
    )

    /**
     * By pressing this button, the player switches to another
     * GUI. The transition effect is applied.
     */
    fun changeGUI(
        slots: InventorySlotCompound,
        icon: ItemStack,
        newGUI: () -> GUI,
        newPage: Int? = null,
        onChange: ((GUIClickEvent) -> Unit)? = null
    ) = defineSlots(
        slots, GUIButtonInventoryChange(
            icon,
            newGUI,
            newPage,
            onChange
        )
    )

    /**
     * Creates a new compound, holding simple compound elements.
     */
    fun createSimpleCompound() = createCompound<GUICompoundElement>(
        iconGenerator = { it.icon },
        onClick = { clickEvent, element -> element.onClick?.invoke(clickEvent) }
    )

    /**
     * Creates a new compound, holding data which can be displayed
     * in any compound space.
     */
    fun <E> createCompound(
        iconGenerator: (E) -> ItemStack,
        onClick: ((clickEvent: GUIClickEvent, element: E) -> Unit)? = null
    ) = GUISpaceCompound(type, iconGenerator, onClick)

    /**
     * Defines an area where the content of the given compound
     * is displayed.
     */
    fun <E> compoundSpace(
        slots: InventorySlotCompound,
        compound: GUISpaceCompound<E>
    ) {
        compound.addSlots(slots)
        defineSlots(
            slots,
            GUISpaceCompoundElement(compound)
        )
    }

    /**
     * Creates a new compound, holding simple compound elements.
     * This compound is strictly a rectangle.
     * The space is automatically defined.
     *
     * This method sets the element type to
     * [GUICompoundElement]. The iconGenerator and onClick callback
     * are automatically defined.
     */
    fun createSimpleRectCompound(
        fromSlot: SingleInventorySlot,
        toSlot: SingleInventorySlot
    ) = createRectCompound<GUICompoundElement>(

        fromSlot, toSlot,

        iconGenerator = { it.icon },
        onClick = { clickEvent, element -> element.onClick?.invoke(clickEvent) }

    )

    /**
     * Creates a new compound, holding custom element data.
     * This compound is strictly a rectangle.
     * The space is automatically defined.
     */
    fun <E> createRectCompound(
        fromSlot: SingleInventorySlot,
        toSlot: SingleInventorySlot,
        iconGenerator: (E) -> ItemStack,
        onClick: ((clickEvent: GUIClickEvent, element: E) -> Unit)? = null
    ): GUIRectSpaceCompound<E> {
        val rectSlotCompound = fromSlot rectTo toSlot
        return GUIRectSpaceCompound(
            type,
            iconGenerator,
            onClick,
            (rectSlotCompound.endInclusive.slotInRow - rectSlotCompound.start.slotInRow) + 1
        ).apply {
            addSlots(rectSlotCompound)
            defineSlots(
                rectSlotCompound,
                GUISpaceCompoundElement(this)
            )
        }
    }

    /**
     * By pressing this button,
     * the user scrolls forwards or backwards in the compound.
     */
    fun compoundScroll(
        slots: InventorySlotCompound,
        icon: ItemStack,
        compound: GUISpaceCompound<*>,
        scrollDistance: Int = 1,
        scrollTimes: Int = 1,
        reverse: Boolean = false
    ) = defineSlots(
        slots,
        GUISpaceCompoundScrollButton(icon, compound, scrollDistance.absoluteValue, scrollTimes, reverse)
    )

    /**
     * By pressing this button,
     * the user scrolls forwards or backwards in the compound.
     */
    fun compoundScroll(
        slots: InventorySlotCompound,
        icon: ItemStack,
        compound: GUIRectSpaceCompound<*>,
        scrollTimes: Int = 1,
        reverse: Boolean = false
    ) = defineSlots(
        slots,
        GUISpaceCompoundScrollButton(icon, compound, scrollTimes, reverse)
    )

}