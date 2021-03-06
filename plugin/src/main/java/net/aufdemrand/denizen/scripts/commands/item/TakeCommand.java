package net.aufdemrand.denizen.scripts.commands.item;

import net.aufdemrand.denizen.BukkitScriptEntryData;
import net.aufdemrand.denizen.objects.dInventory;
import net.aufdemrand.denizen.objects.dItem;
import net.aufdemrand.denizen.utilities.debugging.dB;
import net.aufdemrand.denizen.utilities.depends.Depends;
import net.aufdemrand.denizencore.exceptions.CommandExecutionException;
import net.aufdemrand.denizencore.exceptions.InvalidArgumentsException;
import net.aufdemrand.denizencore.objects.Element;
import net.aufdemrand.denizencore.objects.aH;
import net.aufdemrand.denizencore.objects.dList;
import net.aufdemrand.denizencore.scripts.ScriptEntry;
import net.aufdemrand.denizencore.scripts.commands.AbstractCommand;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class TakeCommand extends AbstractCommand {

    private enum Type {MONEY, ITEMINHAND, ITEM, INVENTORY, BYDISPLAY, SLOT, BYCOVER, SCRIPTNAME}

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {

        for (aH.Argument arg : aH.interpret(scriptEntry.getArguments())) {

            if (!scriptEntry.hasObject("type")
                    && arg.matches("money", "coins")) {
                scriptEntry.addObject("type", Type.MONEY);
            }
            else if (!scriptEntry.hasObject("type")
                    && arg.matches("item_in_hand", "iteminhand")) {
                scriptEntry.addObject("type", Type.ITEMINHAND);
            }
            else if (!scriptEntry.hasObject("qty")
                    && arg.matchesPrefix("q", "qty", "quantity")
                    && arg.matchesPrimitive(aH.PrimitiveType.Double)) {
                scriptEntry.addObject("qty", arg.asElement());
            }
            else if (!scriptEntry.hasObject("items")
                    && arg.matchesPrefix("bydisplay")
                    && !scriptEntry.hasObject("type")) {
                scriptEntry.addObject("type", Type.BYDISPLAY);
                scriptEntry.addObject("displayname", arg.asElement());
            }
            else if (!scriptEntry.hasObject("type")
                    && !scriptEntry.hasObject("items")
                    && arg.matchesPrefix("bycover")) {
                scriptEntry.addObject("type", Type.BYCOVER);
                scriptEntry.addObject("cover", arg.asType(dList.class));
            }
            else if (!scriptEntry.hasObject("type")
                    && !scriptEntry.hasObject("items")
                    && arg.matchesPrefix("script", "scriptname")) {
                scriptEntry.addObject("type", Type.SCRIPTNAME);
                scriptEntry.addObject("scriptitem", arg.asType(dItem.class));
            }
            else if (!scriptEntry.hasObject("slot")
                    && !scriptEntry.hasObject("type")
                    && arg.matchesPrefix("slot")
                    && arg.matchesPrimitive(aH.PrimitiveType.Integer)) {
                scriptEntry.addObject("type", Type.SLOT);
                scriptEntry.addObject("slot", arg.asElement());
            }
            else if (!scriptEntry.hasObject("items")
                    && !scriptEntry.hasObject("type")
                    && arg.matchesArgumentList(dItem.class)) {
                scriptEntry.addObject("items", dList.valueOf(arg.raw_value.replace("item:", "")).filter(dItem.class, scriptEntry));
            }
            else if (!scriptEntry.hasObject("inventory")
                    && arg.matchesPrefix("f", "from")
                    && arg.matchesArgumentType(dInventory.class)) {
                scriptEntry.addObject("inventory", arg.asType(dInventory.class));
            }
            else if (!scriptEntry.hasObject("type")
                    && arg.matches("inventory")) {
                scriptEntry.addObject("type", Type.INVENTORY);
            }
            else if (!scriptEntry.hasObject("inventory")
                    && arg.matches("npc")) {
                scriptEntry.addObject("inventory", ((BukkitScriptEntryData) scriptEntry.entryData).getNPC().getDenizenEntity().getInventory());
            }

        }

        scriptEntry.defaultObject("type", Type.ITEM)
                .defaultObject("qty", new Element(1));

        Type type = (Type) scriptEntry.getObject("type");

        if (type != Type.MONEY && scriptEntry.getObject("inventory") == null) {
            scriptEntry.addObject("inventory", ((BukkitScriptEntryData) scriptEntry.entryData).hasPlayer() ? ((BukkitScriptEntryData) scriptEntry.entryData).getPlayer().getInventory() : null);
        }

        if (!scriptEntry.hasObject("inventory") && type != Type.MONEY) {
            throw new InvalidArgumentsException("Must specify an inventory to take from!");
        }

        if (type == Type.ITEM && scriptEntry.getObject("items") == null) {
            throw new InvalidArgumentsException("Must specify item/items!");
        }

    }

    @Override
    public void execute(ScriptEntry scriptEntry) throws CommandExecutionException {

        dInventory inventory = (dInventory) scriptEntry.getObject("inventory");
        Element qty = scriptEntry.getElement("qty");
        Element displayname = scriptEntry.getElement("displayname");
        dItem scriptitem = scriptEntry.getdObject("scriptitem");
        Element slot = scriptEntry.getElement("slot");
        dList titleAuthor = scriptEntry.getdObject("cover");
        Type type = (Type) scriptEntry.getObject("type");

        Object items_object = scriptEntry.getObject("items");
        List<dItem> items = null;

        if (items_object != null) {
            items = (List<dItem>) items_object;
        }

        if (scriptEntry.dbCallShouldDebug()) {

            dB.report(scriptEntry, getName(),
                    aH.debugObj("Type", type.name())
                            + qty.debug()
                            + (inventory != null ? inventory.debug() : "")
                            + (displayname != null ? displayname.debug() : "")
                            + (scriptitem != null ? scriptitem.debug() : "")
                            + aH.debugObj("Items", items)
                            + (slot != null ? slot.debug() : "")
                            + (titleAuthor != null ? titleAuthor.debug() : ""));

        }

        switch (type) {

            case INVENTORY:
                inventory.clear();
                break;

            case ITEMINHAND:
                int inHandAmt = ((BukkitScriptEntryData) scriptEntry.entryData).getPlayer().getPlayerEntity().getItemInHand().getAmount();
                int theAmount = (int) qty.asDouble();
                ItemStack newHandItem = new ItemStack(Material.AIR);
                if (theAmount > inHandAmt) {
                    dB.echoDebug(scriptEntry, "...player did not have enough of the item in hand, so Denizen just took as many as it could. To avoid this situation, use an IF <PLAYER.ITEM_IN_HAND.QTY>.");
                    ((BukkitScriptEntryData) scriptEntry.entryData).getPlayer().getPlayerEntity().setItemInHand(newHandItem);
                }
                else {

                    // amount is just right!
                    if (theAmount == inHandAmt) {
                        ((BukkitScriptEntryData) scriptEntry.entryData).getPlayer().getPlayerEntity().setItemInHand(newHandItem);
                    }
                    else {
                        // amount is less than what's in hand, need to make a new itemstack of what's left...
                        newHandItem = ((BukkitScriptEntryData) scriptEntry.entryData).getPlayer().getPlayerEntity().getItemInHand().clone();
                        newHandItem.setAmount(inHandAmt - theAmount);
                        ((BukkitScriptEntryData) scriptEntry.entryData).getPlayer().getPlayerEntity().setItemInHand(newHandItem);
                        ((BukkitScriptEntryData) scriptEntry.entryData).getPlayer().getPlayerEntity().updateInventory();
                    }
                }
                break;

            case MONEY:
                if (Depends.economy != null) {
                    Depends.economy.withdrawPlayer(((BukkitScriptEntryData) scriptEntry.entryData).getPlayer().getOfflinePlayer(), qty.asDouble());
                }
                else {
                    dB.echoError(scriptEntry.getResidingQueue(), "No economy loaded! Have you installed Vault and a compatible economy plugin?");
                }
                break;

            case ITEM:
                for (dItem item : items) {
                    ItemStack is = item.getItemStack();
                    is.setAmount(qty.asInt());

                    if (!inventory.removeItem(item, item.getAmount())) {
                        dB.echoDebug(scriptEntry, "Inventory does not contain at least "
                                + qty.asInt() + " of " + item.getFullString() +
                                "... Taking as much as possible...");
                    }
                }
                break;

            case BYDISPLAY:
                int found_items = 0;
                if (displayname == null) {
                    dB.echoError(scriptEntry.getResidingQueue(), "Must specify a displayname!");
                    return;
                }
                for (ItemStack it : inventory.getContents()) {
                    if (found_items < qty.asInt() && it != null && it.hasItemMeta() && it.getItemMeta().hasDisplayName() &&
                            it.getItemMeta().getDisplayName().equalsIgnoreCase(displayname.identify())) {
                        int amt = it.getAmount();
                        if (found_items + it.getAmount() <= qty.asInt()) {
                            inventory.getInventory().removeItem(it);
                        }
                        else {
                            it.setAmount(it.getAmount() - (qty.asInt() - found_items));
                            break;
                        }
                        found_items += amt;
                    }
                }
                break;

            case SCRIPTNAME:
                if (scriptitem == null || scriptitem.getScriptName() == null) {
                    dB.echoError(scriptEntry.getResidingQueue(), "Must specify a valid script name!");
                    return;
                }

                int script_items = 0;
                for (ItemStack it : inventory.getContents()) {
                    if (script_items < qty.asInt()
                            && it != null
                            && scriptitem.getScriptName().equalsIgnoreCase(new dItem(it).getScriptName())) {
                        int amt = it.getAmount();
                        if (script_items + amt <= qty.asInt()) {
                            inventory.getInventory().removeItem(it);
                            script_items += amt;
                        }
                        else {
                            it.setAmount(amt - (qty.asInt() - script_items));
                            break;
                        }
                    }
                }
                break;

            case SLOT:
                inventory.setSlots(slot.asInt() - 1, new ItemStack(Material.AIR));
                break;

            case BYCOVER:
                inventory.removeBook(titleAuthor.get(0),
                        titleAuthor.size() > 1 ? titleAuthor.get(1) : null,
                        qty.asInt());
                break;

        }
    }
}
