package ru.justnanix.wave;

import com.github.steveice10.mc.protocol.MinecraftProtocol;
import org.yaml.snakeyaml.Yaml;
import ru.justnanix.wave.bot.Bot;
import ru.justnanix.wave.parser.NicksParser;
import ru.justnanix.wave.parser.ProxyParser;
import ru.justnanix.wave.parser.ServerParser;
import ru.justnanix.wave.utils.Options;
import ru.justnanix.wave.utils.ThreadUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.Proxy;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Wave {
    private static Wave instance;

    private final ExecutorService threadPool = Executors.newCachedThreadPool();

    private final Random random = new Random(System.currentTimeMillis());

    private final ServerParser serverParser = new ServerParser();
    private final ProxyParser proxyParser = new ProxyParser();
    private final NicksParser nicksParser = new NicksParser();

    private Map<String, Object> values;

    {
        System.out.println(" -> [ Wave v1.3.2 by JustNanix ] <- \n");
        System.setProperty("socksProxyVersion", "4");

        try {
            values = new Yaml().load(new FileInputStream("config.yml"));

            {
                Options.message = (String) values.get("message");
                Options.URL = (String) values.get("URL");

                Options.botsCount = (int) values.get("botsCount");
                Options.joinDelay = (int) values.get("joinDelay");

                Options.randomNicks = (boolean) values.get("randomNicks");
                Options.move = (boolean) values.get("move");

                Options.doubleJoin = (boolean) values.get("doubleJoin");
                Options.antiBotFilter = (boolean) values.get("antiBotFilter");

                Options.isPoolMethod = values.get("threads").equals("pool-method");

                Options.commands = (ArrayList) values.get("commands");
            }

            instance = this;
        } catch (Exception e) {
            System.out.println(" * (System) Возникла ошибка при загрузке config.yml!");

            ThreadUtils.sleep(2000L);
            e.printStackTrace();

            ThreadUtils.sleep(10000L);
            System.exit(0);
        }

        System.err.close();
    }

    public void launch() {
        System.out.println(" * (System) Запуск Wave...");

        proxyParser.init();
        serverParser.init((String) values.get("URL"));
        nicksParser.init();

        while (true) {
            Proxy proxy = proxyParser.nextProxy();

            String server = serverParser.nextServer();
            String nick = Options.randomNicks ? random.nextInt(10000000) + "" : nicksParser.nextNick();

            for (int i = 0; i < Options.botsCount; i++) {
                try {
                    String host = server.split(":")[0];
                    int port = Integer.parseInt(server.split(":")[1]);

                    if (Options.isPoolMethod) threadPool.execute(() -> new Bot(new MinecraftProtocol(nick), host, port, proxy).connect());
                    else new Thread(() -> new Bot(new MinecraftProtocol(nick), host, port, proxy).connect()).start();
                } catch (OutOfMemoryError ignored) {}
            }

            ThreadUtils.sleep(Options.joinDelay);
        }
    }

    public static Wave getInstance() {
        return instance;
    }

    public Map<String, Object> getValues() {
        return values;
    }

    public ExecutorService getThreadPool() {
        return threadPool;
    }

    public ServerParser getServerParser() {
        return serverParser;
    }

    public ProxyParser getProxyParser() {
        return proxyParser;
    }

    public NicksParser getNicksParser() {
        return nicksParser;
    }

    public Random getRandom() {
        return random;
    }
}
