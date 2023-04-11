package io.icker.factions.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.icker.factions.FactionsMod;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.util.Command;
import io.icker.factions.util.Message;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CreateCommand implements Command {
    private int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String name = StringArgumentType.getString(context, "name");

        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        if (FactionsMod.CONFIG.DISPLAY.NAME_BLACKLIST.contains(name.toLowerCase(Locale.ROOT))) {
            new Message("Cannot create a faction with this name as it is on the blacklist").fail().send(player, false);
            return 0;
        }

        if (FactionsMod.CONFIG.DISPLAY.NAME_MAX_LENGTH >= 0 & FactionsMod.CONFIG.DISPLAY.NAME_MAX_LENGTH < name.length()) {
            new Message("Cannot create a faction with this name as it is too long").fail().send(player, false);
            return 0;
        }

        if (Faction.getByName(name) != null) {
            new Message("Cannot create a faction as a one with that name already exists").fail().send(player, false);
            return 0;
        }

        if (FactionsMod.CONFIG.ITEMS.REQUIRE_ITEMS && !User.get(player.getUuid()).bypass) {

            PlayerInventory inventory = player.getInventory();

            List<String> missing = new ArrayList<>();
            List<ItemStack> stacks = new ArrayList<>();

            FactionsMod.CONFIG.ITEMS.ITEMS.forEach((item, count) -> {
                Identifier itemId = Identifier.tryParse(item);
                if (itemId == null) return;
                ItemStack stack = Registries.ITEM.get(itemId).getDefaultStack();
                if (stack == null) return;
                stack.setCount(count);
                stacks.add(stack);
                if (!inventory.contains(stack)) missing.add(stack.getName().getString() + " x" + count);
            });

            if (!missing.isEmpty()) {
                new Message("Cannot create a faction as you don't have the required items, missing:\n" + String.join("\n", missing)).fail().send(player, false);
                return 0;
            } else {
                stacks.forEach((ItemStack stack) -> {
                    int slot = inventory.getSlotWithStack(stack);
                    inventory.removeStack(slot, stack.getCount());
                });
            }
        }

        Faction faction = new Faction(name, "No description set", "No faction MOTD set", Formatting.WHITE, false, FactionsMod.CONFIG.POWER.BASE + FactionsMod.CONFIG.POWER.MEMBER);
        Faction.add(faction);
        Command.getUser(player).joinFaction(faction.getID(), User.Rank.OWNER);

        if (FactionsMod.CONFIG.AUTO_CLAIM.ENABLE) {
            if(ClaimCommand.addForced(context, FactionsMod.CONFIG.AUTO_CLAIM.CLAIM_RADIUS) == 0) {
                new Message("Cannot create a faction as another faction is too close").fail().send(player, false);
                faction.remove();
                return 0;
            }
        }

        source.getServer().getPlayerManager().sendCommandTree(player);
        new Message("Successfully created faction").send(player, false);
        return 1;
    }

    public LiteralCommandNode<ServerCommandSource> getNode() {
        return CommandManager
            .literal("create")
            .requires(Requires.multiple(Requires.isFactionless(), Requires.hasPerms("factions.create", 0)))
            .then(
                CommandManager.argument("name", StringArgumentType.greedyString()).executes(this::run)
            )
            .build();
    }
}