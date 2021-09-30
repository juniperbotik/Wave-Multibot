package ru.justnanix.wave.parser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.justnanix.wave.Wave;
import ru.justnanix.wave.utils.FindUtils;
import ru.justnanix.wave.utils.ThreadUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

public class ServerParser {
    private List<String> servers = new CopyOnWriteArrayList<>();
    private int number = -1;

    public void init(String URL) {
        System.out.println("\n * (ServerParser) -> Парсю сервера...");

        try {
            Document document = Jsoup.connect(URL).get();

            String pattern = "\\b(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?):\\d{1,5}\\b";
            Elements elements = document.getElementsByAttributeValue("class", "server");

            for (Element element : elements)
                servers.addAll(FindUtils.findStringsByRegex(element.text(), Pattern.compile(pattern)));

        } catch (Throwable ignored) {}

        new Thread(() -> {
            while (true) {
                ThreadUtils.sleep(10000L);

                try {
                    Document document = Jsoup.connect(URL).get();

                    String pattern = "\\b(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?):\\d{1,5}\\b";
                    Elements elements = document.getElementsByAttributeValue("class", "server");

                    List<String> temp = new ArrayList<>(servers);

                    for (Element element : elements)
                        temp.addAll(FindUtils.findStringsByRegex(element.text(), Pattern.compile(pattern)));

                    servers = temp;
                } catch (Throwable ignored) {}
            }
        }).start();

        System.out.printf(" * (ServerParser) -> Загружено %s серверов.\n\n", servers.size());
    }

    public String nextServer() {
        ++number;

        if (number >= servers.size())
            number = 0;

        return servers.get(number);
    }

    public List<String> getServers() {
        return servers;
    }
}
