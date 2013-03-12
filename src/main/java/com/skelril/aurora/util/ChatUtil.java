package com.skelril.aurora.util;

import com.sk89q.commandbook.util.PlayerUtil;
import com.sk89q.minecraft.util.commands.CommandException;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * @author Turtle9598
 */
public class ChatUtil {

    public static void sendNotice(CommandSender sender, String notice) {

        sender.sendMessage(ChatColor.YELLOW + notice);
    }

    public static void sendNotice(String playerName, String notice) {

        try {
            Player player = PlayerUtil.matchPlayerExactly(null, playerName);
            player.sendMessage(ChatColor.YELLOW + notice);
        } catch (CommandException ignored) {}
    }

    public static void sendNotice(CommandSender[] senders, String notice) {

        for (CommandSender sender : senders) {

            sendNotice(sender, notice);
        }
    }

    public static void sendNotice(CommandSender sender, ChatColor chatColor, String notice) {

        sender.sendMessage(chatColor + notice);
    }

    public static void sendNotice(CommandSender[] senders, ChatColor chatColor, String notice) {

        for (CommandSender sender : senders) {

            sendNotice(sender, chatColor, notice);
        }
    }

    public static void sendError(CommandSender sender, String error) {

        sender.sendMessage(ChatColor.RED + error);
    }

    public static void sendError(CommandSender[] senders, String error) {

        for (CommandSender sender : senders) {

            sendError(sender, error);
        }
    }

    public static void sendWarning(CommandSender sender, String warning) {

        sender.sendMessage(ChatColor.RED + warning);
    }

    public static void sendWarning(CommandSender[] senders, String warning) {

        for (CommandSender sender : senders) {
            sendWarning(sender, warning);
        }
    }

    public static String makeCountString(int value, String currencyName) {

        return makeCountString(ChatColor.YELLOW, String.valueOf(value), currencyName);
    }

    public static String makeCountString(double value, String currencyName) {

        return makeCountString(ChatColor.YELLOW, String.valueOf(value), currencyName);
    }

    public static String makeCountString(String value, String currencyName) {

        return makeCountString(ChatColor.YELLOW, value, currencyName);
    }

    public static String makeCountString(ChatColor color, int value, String currencyName) {

        return ChatColor.WHITE + String.valueOf(value) + color + currencyName;
    }

    public static String makeCountString(ChatColor color, double value, String currencyName) {

        return ChatColor.WHITE + String.valueOf(value) + color + currencyName;
    }

    public static String makeCountString(ChatColor color, String value, String currencyName) {

        return ChatColor.WHITE + value + color + currencyName;
    }

    public static String loonyCharacter() {

        switch (ChanceUtil.getRandom(6)) {
            case 6:
                return "&";
            case 5:
                return "!";
            case 4:
                return "#";
            case 3:
                return "@";
            case 2:
                return "%";
            case 1:
                return "$";
            default:
                return "";
        }
    }

    public static ChatColor loonyColor() {

        switch (ChanceUtil.getRandom(6)) {
            case 6:
                return ChatColor.RED;
            case 5:
                return ChatColor.GREEN;
            case 4:
                return ChatColor.BLUE;
            case 3:
                return ChatColor.AQUA;
            case 2:
                return ChatColor.YELLOW;
            case 1:
                return ChatColor.DARK_PURPLE;
            default:
                return ChatColor.WHITE;
        }
    }

    public static String runeizeString(String string) {

        StringBuilder out = new StringBuilder();

        char[] chars = string.toCharArray();
        boolean next = true;

        for (int i = 0; i < chars.length; i++) {

            if (!Character.isAlphabetic(chars[i])) {
                next = true;
                continue;
            }

            if (!next) {
                chars[i] = Character.toLowerCase(chars[i]);
            }
            next = false;
        }

        for (char character : chars) {
            out.append(character);
        }

        return out.toString();
    }
}