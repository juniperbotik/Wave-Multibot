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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class SessionListener extends SessionAdapter {
    private final Bot client;
    private int joinCount = 1;

    public SessionListener(Bot client) {
        this.client = client;

        if (!((boolean) Wave.getInstance().getValues().get("move")))
            return;

        Runnable mover = () -> {
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
        };

        if (Wave.getInstance().getValues().get("threads").equals("thread-method"))
            new Thread(mover).start();

        if (Wave.getInstance().getValues().get("threads").equals("pool-method"))
            Wave.getInstance().getThreadPool().execute(mover);
    }

    @Override
    public void disconnected(DisconnectedEvent event) {
        if (joinCount <= 2 && (boolean) Wave.getInstance().getValues().get("doubleJoin")) {
            ThreadUtils.sleep(6000L);
            client.connect();

            joinCount++;
        }
    }

    @Override
    public void packetReceived(PacketReceivedEvent receiveEvent) {
        if (receiveEvent.getPacket() instanceof ServerJoinGamePacket) {
            joinCount++;

            System.out.println(" * (" + client.getSession().getHost() + ":" + client.getSession().getPort() + ") (" + client.getGameProfile().getName() + ") Подключился.");
            client.register();

            Runnable messageSender = () -> {
                while (client.isOnline()) {
                    for (Object obj : (ArrayList) Wave.getInstance().getValues().get("commands")) {
                        String str = (String) obj;
                        str = str.trim();

                        if (str.contains("wait") && !str.contains("{MESSAGE}")) {
                            if (str.contains("ms"))
                                ThreadUtils.sleep(Long.parseLong(str.substring(str.indexOf(" "), str.indexOf("ms")).trim()));
                            else if (str.contains("s"))
                                ThreadUtils.sleep(TimeUnit.SECONDS.toMillis(Long.parseLong(str.substring(str.indexOf(" "), str.indexOf("s")).trim())));
                        } else {
                            client.getSession().send(new ClientChatPacket(str.replace("{MESSAGE}", (String) Wave.getInstance().getValues().get("message"))));
                        }
                    }
                }
            };

            if (Wave.getInstance().getValues().get("threads").equals("thread-method"))
                new Thread(messageSender).start();

            if (Wave.getInstance().getValues().get("threads").equals("pool-method"))
                Wave.getInstance().getThreadPool().execute(messageSender);

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
