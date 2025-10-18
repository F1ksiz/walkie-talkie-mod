package fr.flaton.walkietalkie.network.packet.c2s;

import dev.architectury.networking.NetworkManager;
import fr.flaton.walkietalkie.Util;
import fr.flaton.walkietalkie.config.ModConfig;
import fr.flaton.walkietalkie.screen.SpeakerScreenHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;

public class UpdateSpeakerC2SPacket {

    public static void receive(PacketByteBuf packetByteBuf, NetworkManager.PacketContext packetContext) {
        ServerPlayerEntity player = (ServerPlayerEntity) packetContext.getPlayer();

        int index = packetByteBuf.readInt();

        ScreenHandler screenHandler = player.currentScreenHandler;

        if (screenHandler == null) {
            return;
        }

        if (screenHandler instanceof SpeakerScreenHandler speakerScreenHandler) {

            boolean activate = speakerScreenHandler.isActivate();
            int canal = speakerScreenHandler.getCanal();

            switch (index) {
                case 0 -> activate = !activate;
                case 2 -> {
                    // Direct frequency update (stored as int * 10)
                    int frequency = packetByteBuf.readInt();
                    if (frequency >= 100 && frequency <= 10000) { // 10.0 to 1000.0
                        canal = frequency;
                    }
                }
            }
            speakerScreenHandler.setPropertyDelegate(activate , canal);
        }
    }
}
