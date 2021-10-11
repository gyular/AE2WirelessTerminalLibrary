package de.mari_023.fabric.ae2wtlib.client;

import appeng.util.item.AEItemStack;
import de.mari_023.fabric.ae2wtlib.ae2wtlib;
import de.mari_023.fabric.ae2wtlib.ae2wtlibConfig;
import de.mari_023.fabric.ae2wtlib.trinket.TrinketsHelper;
import de.mari_023.fabric.ae2wtlib.wct.CraftingTerminalHandler;
import de.mari_023.fabric.ae2wtlib.wit.WITScreen;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class NetworkingClient {
    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(new Identifier(ae2wtlib.MOD_NAME, "interface_terminal"), (client, handler, buf, responseSender) -> {
            buf.retain();
            client.execute(() -> {
                if(client.player == null) return;

                final Screen screen = MinecraftClient.getInstance().currentScreen;
                if(screen instanceof WITScreen) {
                    NbtCompound tag = buf.readNbt();
                    if(tag != null)
                        ((WITScreen) screen).postUpdate(false, tag);
                }
                buf.release();
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(new Identifier(ae2wtlib.MOD_NAME, "update_restock"), (client, handler, buf, responseSender) -> {
            buf.retain();
            client.execute(() -> {
                if(client.player == null) return;
                client.player.getInventory().getStack(buf.readInt()).setCount(buf.readInt());
                buf.release();
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(new Identifier(ae2wtlib.MOD_NAME, "update_wut"), (client, handler, buf, responseSender) -> {
            buf.retain();
            client.execute(() -> {
                if(client.player == null) return;
                int slot = buf.readInt();
                ItemStack is;
                NbtCompound tag = buf.readNbt();
                if(slot >= 100 && slot < 200 && ae2wtlibConfig.INSTANCE.allowTrinket())
                    is = TrinketsHelper.getTrinketsInventory(client.player).getStackInSlot(slot - 100);
                else is = client.player.getInventory().getStack(slot);
                is.setNbt(tag);
                buf.release();
                CraftingTerminalHandler.getCraftingTerminalHandler(client.player).invalidateCache();
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(new Identifier(ae2wtlib.MOD_NAME, "restock_amounts"), (client, handler, buf, responseSender) -> {
            buf.retain();
            client.execute(() -> {
                if(client.player == null) return;
                CraftingTerminalHandler ctHandler = CraftingTerminalHandler.getCraftingTerminalHandler(client.player);
                List<AEItemStack> items = new ArrayList<>();
                while(buf.isReadable()) items.add(AEItemStack.fromPacket(buf));
                ctHandler.setRestockAbleItems(items);
            });
        });
    }
}
