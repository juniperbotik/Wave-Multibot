package ru.justnanix.wave.bot;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientChatPacket;
import com.github.steveice10.packetlib.Client;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.tcp.TcpSessionFactory;
import org.jsoup.internal.FieldsAreNonnullByDefault;
import ru.justnanix.wave.Wave;
import ru.justnanix.wave.bot.listener.SessionListener;
import ru.justnanix.wave.utils.ThreadUtils;

import java.net.Proxy;

public class Bot {
    private final MinecraftProtocol account;

    private String host;
    private int port;

    private Session session;
    private Proxy proxy;

    private double posX;
    private double posY;
    private double posZ;

    public Bot(MinecraftProtocol account, String host, int port, Proxy proxy) {
        this.account = account;
        this.proxy = proxy;

        this.host = host;
        this.port = port;
    }

    public void connect() {
        Client client = new Client(host, port, account, new TcpSessionFactory(proxy));

        client.getSession().addListener(new SessionListener(this));
        client.getSession().connect();

        this.session = client.getSession();
    }

    public void register() {
        String password = (boolean) Wave.getInstance().getValues().get("randomNicks") ? String.valueOf(Wave.getInstance().getRandom().nextInt(100000000)) : "4321qq4321";
        ThreadUtils.sleep(500L);

        session.send(new ClientChatPacket(String.format("/register %s %s", password, password)));
        session.send(new ClientChatPacket(String.format("/login %s", password)));
    }

    public boolean isOnline() {
        return session != null && session.isConnected();
    }

    public Session getSession() {
        return session;
    }

    public GameProfile getGameProfile() {
        return account.getProfile();
    }

    public double getPosX() {
        return posX;
    }

    public void setPosX(double posX) {
        this.posX = posX;
    }

    public double getPosY() {
        return posY;
    }

    public void setPosY(double posY) {
        this.posY = posY;
    }

    public double getPosZ() {
        return posZ;
    }

    public void setPosZ(double posZ) {
        this.posZ = posZ;
    }
}
