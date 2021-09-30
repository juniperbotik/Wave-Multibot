package ru.justnanix.wave;

import com.github.steveice10.mc.protocol.MinecraftProtocol;
import org.yaml.snakeyaml.Yaml;
import ru.justnanix.wave.bot.Bot;
import ru.justnanix.wave.parser.NicksParser;
import ru.justnanix.wave.parser.ProxyParser;
import ru.justnanix.wave.parser.ServerParser;
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
        try {
            System.setProperty("socksProxyVersion", "4");

            values = new Yaml().load(new FileInputStream("config.yml"));
            instance = this;
        } catch (Exception e) {
            System.out.println(" * (System) Возникла ошибка при загрузке config.yml!");

            ThreadUtils.sleep(2000L);
            e.printStackTrace();

            ThreadUtils.sleep(10000L);
            System.exit(0);
        }
    }

    public void launch() {
        System.out.println(" -> [ Wave v1.3 by JustNanix ] <- ");

        System.out.println("\n * (System) Запуск Wave...");

        proxyParser.init();
        serverParser.init((String) values.get("URL"));
        nicksParser.init();

        Runnable bots = () -> {
            while (true) {
                Proxy proxy = proxyParser.nextProxy();

                String server = serverParser.nextServer();
                String nick = nicksParser.nextNick();

                for (int i = 0; i < (int) values.get("botsCount"); i++) {
                    try {
                        String host = server.split(":")[0];
                        int port = Integer.parseInt(server.split(":")[1]);

                        if (values.get("threads").equals("pool-method"))
                            threadPool.execute(() -> new Bot(new MinecraftProtocol(nick), host, port, proxy).connect());
                        if (values.get("threads").equals("thread-method"))
                            new Thread(() -> new Bot(new MinecraftProtocol(nick), host, port, proxy).connect()).start();
                    } catch (Exception ignored) {}
                }

                ThreadUtils.sleep((int) values.get("joinDelay"));
            }
        };

        if (values.get("threads").equals("pool-method"))
            threadPool.execute(bots);

        if (values.get("threads").equals("thread-method"))
            new Thread(bots).start();
    }

    public Map<String, Object> getValues() {
        return values;
    }

    public ExecutorService getThreadPool() {
        return threadPool;
    }

    public static Wave getInstance() {
        return instance;
    }

    public Random getRandom() {
        return random;
    }
}
