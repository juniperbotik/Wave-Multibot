package ru.justnanix.wave.bot.listener;

import com.github.steveice10.mc.protocol.data.game.ClientRequest;
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
import ru.justnanix.wave.utils.*;

import java.util.concurrent.atomic.AtomicInteger;

public class SessionListener extends SessionAdapter {
    private final Bot client;
    private int disconnects = 0;

    public SessionListener(Bot client) {
        this.client = client;

        if (!Options.move)
            return;

        new Thread(() -> {
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
    public void disconnected(DisconnectedEvent event) {
        Statistics.botList.remove(client);

        if (Options.doubleJoin)
            disconnects++;

        if (!event.getReason().equals("bf") && disconnects < 2 && Options.doubleJoin) {
            ThreadUtils.sleep(5000L);
            client.connect();
        }
    }

    @Override
    public void packetReceived(PacketReceivedEvent receiveEvent) {
        if (receiveEvent.getPacket() instanceof ServerJoinGamePacket) {
            if (Options.infoFormat < 1)
                System.out.println(" * (" + client.getSession().getHost() + ":" + client.getSession().getPort() + ") (" + client.getGameProfile().getName() + ") Подключился.");

            Statistics.botList.add(client);

            new Thread(client::register).start();
            new Thread(() -> {
                while (client.isOnline()) {
                    for (Object obj : Options.commands) {
                        try {
                            CommandParser.parseCommand((String) obj, client);
                        } catch (Exception ignored) {}
                    }
                }
            }).start();
        }  else if (receiveEvent.getPacket() instanceof ServerChatPacket) {
            ServerChatPacket packet = receiveEvent.getPacket();
            String message = packet.getMessage().getFullText();

            if (Options.antiBotFilter && message.contains("Ожидайте завершения проверки...")) {
                String ip = client.getSession().getHost() + ":" + client.getSession().getPort();

                if (Options.infoFormat < 1)
                    System.out.println("\n * (AntiBotFilter) Удаляю сервер " + ip + "\n");

                Wave.getInstance().getServerParser().getServers().remove(ip);
                client.getSession().disconnect("bf");

                return;
            }

            if (!StringUtils.containsIgnoreCase(message, "chat.type") && Options.infoFormat < 1)
                System.out.println(" * (" + client.getSession().getHost() + ":" + client.getSession().getPort() + ") (" + client.getGameProfile().getName() + ") Чат: " + message);

            CaptchaUtils.solveCaptcha(message, client);
        } else if (receiveEvent.getPacket() instanceof ServerPlayerPositionRotationPacket) {
            ServerPlayerPositionRotationPacket packet = receiveEvent.getPacket();

            client.setPosX(packet.getX());
            client.setPosY(packet.getY());
            client.setPosZ(packet.getZ());

            client.getSession().send(new ClientTeleportConfirmPacket(packet.getTeleportId()));
        } else if (receiveEvent.getPacket() instanceof ServerPlayerHealthPacket) {
            if (((ServerPlayerHealthPacket) receiveEvent.getPacket()).getHealth() < 1) {
                client.getSession().send(new ClientRequestPacket(ClientRequest.RESPAWN));
            }
        }
    }
}
