package ru.justnanix.wave.bot.listener;

import com.github.steveice10.mc.protocol.data.game.ClientRequest;
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientRequestPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerPositionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.world.ClientTeleportConfirmPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerJoinGamePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerHealthPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerPositionRotationPacket;
import com.github.steveice10.packetlib.event.session.DisconnectedEvent;
import com.github.steveice10.packetlib.event.session.PacketReceivedEvent;
import com.github.steveice10.packetlib.event.session.SessionAdapter;
import org.apache.commons.lang3.StringUtils;
import ru.justnanix.wave.Wave;
import ru.justnanix.wave.bot.Bot;
import ru.justnanix.wave.utils.CaptchaUtils;
import ru.justnanix.wave.utils.ThreadUtils;

public class SessionListener extends SessionAdapter {
    private final Bot client;

    public SessionListener(Bot client) {
        this.client = client;

        new Thread(() -> {
            ThreadUtils.sleep(10000L);

            double posX = -0.2;
            double posZ = -0.2;

            while (client.isOnline()) {
                for (int i = 0; i < Wave.getInstance().getRandom().nextInt(200); i++) {
                    client.getSession().send(new ClientPlayerPositionPacket(true, client.getPosX() + posX, client.getPosY(), client.getPosZ()));
                    client.setPosX(client.getPosX() + posX);

                    ThreadUtils.sleep(50L);
                }

                for (int i = 0; i < Wave.getInstance().getRandom().nextInt(200); i++) {
                    client.getSession().send(new ClientPlayerPositionPacket(true, client.getPosX(), client.getPosY(), client.getPosZ() + posZ));
                    client.setPosZ(client.getPosZ() + posZ);

                    ThreadUtils.sleep(50L);
                }

                posX = -posX;
                posZ = -posZ;
            }
        }).start();
    }

    @Override
    public void packetReceived(PacketReceivedEvent receiveEvent) {
        if (receiveEvent.getPacket() instanceof ServerJoinGamePacket) {
            System.out.println(" * (" + client.getSession().getHost() + ":" + client.getSession().getPort() + ") (" + client.getGameProfile().getName() + ") Подключился.");
            client.register();

            new Thread(() -> {
                while (client.isOnline()) {
                    client.getSession().send(new ClientChatPacket(Wave.getInstance().getMessage()));
                    ThreadUtils.sleep(Wave.getInstance().getMessageDelay());
                }
            }).start();
        }  else if (receiveEvent.getPacket() instanceof ServerChatPacket) {
            ServerChatPacket packet = receiveEvent.getPacket();
            String message = packet.getMessage().getFullText();

            if (!StringUtils.containsIgnoreCase(message, "chat.type"))
                System.out.println(" * (" + client.getSession().getHost() + ":" + client.getSession().getPort() + ") (" + client.getGameProfile().getName() + ") Чат: " + message);

            CaptchaUtils.solveCaptcha(message, client);
        } else if (receiveEvent.getPacket() instanceof ServerPlayerPositionRotationPacket) {
            ServerPlayerPositionRotationPacket packet = receiveEvent.getPacket();

            client.setPosX(packet.getX());
            client.setPosY(packet.getY());
            client.setPosZ(packet.getZ());

            client.getSession().send(new ClientTeleportConfirmPacket(packet.getTeleportId()));
        } else if (receiveEvent.getPacket() instanceof ServerPlayerHealthPacket) {
            if (((ServerPlayerHealthPacket) receiveEvent.getPacket()).getHealth() < 1) client.getSession().send(new ClientRequestPacket(ClientRequest.RESPAWN));
        }
    }
}
