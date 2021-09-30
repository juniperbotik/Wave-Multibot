package ru.justnanix.wave;

import com.github.steveice10.mc.protocol.MinecraftProtocol;
import ru.justnanix.wave.bot.Bot;
import ru.justnanix.wave.parser.NicksParser;
import ru.justnanix.wave.parser.ProxyParser;
import ru.justnanix.wave.parser.ServerParser;
import ru.justnanix.wave.utils.ThreadUtils;

import java.net.Proxy;
import java.util.Arrays;
import java.util.Base64;
import java.util.Random;
import java.util.Scanner;

public class Wave {
    private static Wave instance;

    private final ServerParser serverParser;
    private final ProxyParser proxyParser;
    private final NicksParser nicksParser;
    private final Random random;

    private boolean randomNicks = false;
    private int message_delay = 3500;

    private String message;
    private String mode;

    {
        System.setProperty("socksProxyVersion", "4");

        serverParser = new ServerParser();
        proxyParser = new ProxyParser();
        nicksParser = new NicksParser();

        random = new Random(System.currentTimeMillis());
        instance = this;
    }

    public void launch() {
        System.out.println(new String(Base64.getDecoder().decode(Base64.getDecoder().decode("SUMwK0lGc2dWMkYyWlNCMk1TNHlJR0o1SUVwMWMzUk9ZVzVwZUNCZElEd3RJQT09"))) + "\n");

        try (Scanner sc = new Scanner(System.in, "IBM866")) {
            System.out.println(" * (System) Введите режим: ");

            System.out.println(" * (System) 1. Обычный ");
            System.out.println(" * (System) 2. Расширенный");
            System.out.print(" > ");

            mode = sc.nextLine();

            if (mode.equals("1") || mode.equalsIgnoreCase("Обычный")) {
                System.out.println("\n * (System) Введите сообщение: ");
                System.out.print(" > ");

                message = sc.nextLine();

                System.out.println("\n * (System) Запуск Wave...");

                proxyParser.init();
                serverParser.init("https://monitoringminecraft.ru/novie-servera");
                nicksParser.init();

                new Thread(() -> {
                    while (true) {
                        Proxy proxy = proxyParser.nextProxy();

                        String server = serverParser.nextServer();
                        String nick = nicksParser.nextNick();

                        for (int i = 0; i < 3; i++) {
                            try {
                                new Thread(() ->
                                        new Bot(new MinecraftProtocol(nick), proxy).connect(server.split(":")[0], Integer.parseInt(server.split(":")[1]))
                                ).start();
                            } catch (Exception ignored) {}
                        }

                        ThreadUtils.sleep(150L);
                    }
                }).start();
            }

            if (mode.equals("2") || mode.equalsIgnoreCase("Расширенный")) {
                System.out.println("\n * (System) Введите ссылку для парсинга серверов (дефолт https://monitoringminecraft.ru/novie-servera)");
                System.out.print(" > ");
                String URL = sc.nextLine();

                System.out.println(" * (System) Введите сообщение");
                System.out.print(" > ");
                message = sc.nextLine();

                System.out.println(" * (System) Введите задержку отправки сообщения (мс)");
                System.out.print(" > ");
                message_delay = Integer.parseInt(sc.nextLine());

                System.out.println(" * (System) Введите задержку (дефолт 150)");
                System.out.print(" > ");
                int delay = Integer.parseInt(sc.nextLine());

                System.out.println(" * (System) Введите кол-во ботов на один сервер (дефолт 3)");
                System.out.print(" > ");
                int bots = Integer.parseInt(sc.nextLine());

                System.out.println(" * (System) Использовать ли рандомные ники и пароли? (д/н)");
                System.out.print(" > ");
                randomNicks = sc.nextLine().equalsIgnoreCase("д");

                System.out.println("\n * (System) Запуск Wave...");

                proxyParser.init();
                serverParser.init(URL);

                if (!randomNicks)
                    nicksParser.init();

                new Thread(() -> {
                    while (true) {
                        Proxy proxy = proxyParser.nextProxy();
                        String server = serverParser.nextServer();

                        String nick = randomNicks ? String.valueOf(random.nextInt(10000000)) : nicksParser.nextNick();

                        for (int i = 0; i < bots; i++) {
                            new Thread(() ->
                                    new Bot(new MinecraftProtocol(nick), proxy).connect(server.split(":")[0], Integer.parseInt(server.split(":")[1]))
                            ).start();
                        }

                        ThreadUtils.sleep(delay);
                    }
                }).start();
            }
        }
    }

    public static Wave getInstance() {
        return instance;
    }

    public Random getRandom() {
        return random;
    }

    public String getMessage() {
        return message;
    }

    public int getMessageDelay() {
        return message_delay;
    }

    public boolean isRandomNicks() {
        return randomNicks;
    }
}
