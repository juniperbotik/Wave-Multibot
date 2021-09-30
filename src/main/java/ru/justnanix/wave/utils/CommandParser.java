package ru.justnanix.wave.utils;

import com.github.steveice10.mc.protocol.packet.ingame.client.ClientChatPacket;
import ru.justnanix.wave.Wave;
import ru.justnanix.wave.bot.Bot;

import java.util.concurrent.TimeUnit;

public class CommandParser {
    public static void parseCommand(String str, Bot client) {
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
