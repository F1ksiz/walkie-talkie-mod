package fr.flaton.walkietalkie.network.packet.c2s;

import dev.architectury.networking.NetworkManager;
import fr.flaton.walkietalkie.Util;
import fr.flaton.walkietalkie.config.ModConfig;
import fr.flaton.walkietalkie.item.WalkieTalkieItem;
import fr.flaton.walkietalkie.network.ModMessages;
import io.netty.buffer.Unpooled;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

public class UpdateWalkieTalkieC2SPacket {
    public static void receive(PacketByteBuf packetByteBuf, NetworkManager.PacketContext packetContext) {
        ServerPlayerEntity player = (ServerPlayerEntity) packetContext.getPlayer();

        ItemStack stack = Util.getWalkieTalkieInHand(player);
        if (!(stack.getItem() instanceof WalkieTalkieItem) && !stack.hasNbt()) {
            return;
        }

        int index = packetByteBuf.readInt();

        boolean activate = stack.getNbt().getBoolean(WalkieTalkieItem.NBT_KEY_ACTIVATE);
        boolean mute = stack.getNbt().getBoolean(WalkieTalkieItem.NBT_KEY_MUTE);
        int canal = stack.getNbt().getInt(WalkieTalkieItem.NBT_KEY_CANAL);

        switch (index) {
            case 0 -> activate = !activate;
            case 2 -> mute = !mute;
            case 3 -> {
                // Direct frequency update (stored as int * 10)
                int frequency = packetByteBuf.readInt();
                if (frequency >= 100 && frequency <= 10000) { // 10.0 to 1000.0
                    canal = frequency;
                }
            }
        }

        stack.getNbt().putBoolean(WalkieTalkieItem.NBT_KEY_ACTIVATE, activate);
        stack.getNbt().putBoolean(WalkieTalkieItem.NBT_KEY_MUTE, mute);
        stack.getNbt().putInt(WalkieTalkieItem.NBT_KEY_CANAL, canal);

        PacketByteBuf packet = new PacketByteBuf(Unpooled.buffer());
        packet.writeItemStack(stack);
        NetworkManager.sendToPlayer(player, ModMessages.UPDATE_WALKIETALKIE_S2C, packet);
    }
}
