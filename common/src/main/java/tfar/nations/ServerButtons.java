package tfar.nations;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Items;
import tfar.nations.nation.Nation;
import tfar.nations.nation.NationData;
import tfar.nations.sgui.api.elements.GuiElementBuilder;
import tfar.nations.sgui.api.gui.SimpleGui;

import java.util.Comparator;
import java.util.List;

public class ServerButtons {

    public static GuiElementBuilder createNationsTop(ServerPlayer player, NationData nationData, Nation existingNation) {
        return new GuiElementBuilder()
                .setItem(Items.NETHER_STAR)
                .hideFlags()
                .setName(Component.literal("Top Nations"))
                .setCallback((index1, type1, action1, gui1) -> {
                    SimpleGui nationsTopGui = new SimpleGui(MenuType.GENERIC_9x3, player, false);
                    nationsTopGui.setTitle(Component.literal("Top Nations"));
                    List<Nation> nations = nationData.getNations();
                    nations.sort(Comparator.comparingInt(Nation::getTotalPower));
                    for (int i = 0; i < nations.size(); i++) {
                        Nation nation = nations.get(i);
                        nationsTopGui.setSlot(i, new GuiElementBuilder(Items.PLAYER_HEAD)
                                .setSkullOwner(nation.getOwner(), player.server)
                                .setName(Component.literal(nation.getOwner().getName() + " - "+nation.getTotalPower() +" Power"))
                                .setCallback((index2, type2, action2, gui2) -> {
                                })
                        );
                    }
                    nationsTopGui.open();
                });
    }
}
