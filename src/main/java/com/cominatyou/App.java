package com.cominatyou;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.awt.AWTException;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.TrayIcon.MessageType;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.emoji.CustomEmojiBuilder;
import org.javacord.api.entity.emoji.KnownCustomEmoji;
import org.javacord.api.entity.server.Server;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class App {
    static void displayNotification(String title, String description, TrayIcon trayIcon, MessageType type) throws AWTException {
        trayIcon.displayMessage(title, description, type);
    }
    public static void main(String[] argv) throws AWTException, IOException, InterruptedException, ParseException {
        final TrayIcon trayIcon = SystemTray.isSupported() ? WinTrayIcon.createTrayIcon() : null;
        JSONObject config = (JSONObject) new JSONParser().parse(new FileReader("./config.json"));
        DiscordApi api = null;
        try {
            api = new DiscordApiBuilder().setToken((String) config.get("token")).login().join();
        }
        catch (Exception e) {
            displayNotification("Unable to Connect to Discord", "We couldn't connect to Discord. This is most likely caused by an incorrect token.", trayIcon, MessageType.ERROR);
            System.exit(1);
        }
        final String folderPath = "\\\\UbuntuNAS\\CDN\\DiscordEmotes";
        final boolean suppressNotifications = !SystemTray.isSupported();
        Path dir = Paths.get(folderPath);
        Server server = null;
        try {
            server = api.getServerById((String) config.get("serverID")).get();
        }
        catch (Exception e) {
            displayNotification("Invalid Server ID", "Unable to access the provided server ID. Make sure that the bot is in the server, and that the ID is correct.", trayIcon, MessageType.ERROR);
            System.exit(1);
        }
        if (dir.toFile().exists() == false) {
            System.out.println("Directory does not exist!");
            displayNotification("Invalid Directory", "The provided directory path doesn't seem to exist.", trayIcon, MessageType.ERROR);
            System.exit(1);
        }
        WatchService watcher = FileSystems.getDefault().newWatchService();
        WatchKey key = null;
        try {
            key = dir.register(watcher, ENTRY_CREATE /*, ENTRY_DELETE */);
        }
        catch (IOException e) {
            System.err.println(e);
        }
        while (1 < 2) { // if you know, you know
            try {
                key = watcher.take();
            }
            catch (InterruptedException e) {
                return;
            }
            for (WatchEvent<?> event: key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();
                if (kind == OVERFLOW) continue;
                @SuppressWarnings("unchecked")
                WatchEvent<Path> ev = (WatchEvent<Path>) event;
                Path filename = ev.context();
                try {
                    Path child = dir.resolve(filename);
                    if (!Files.probeContentType(child).equals("image/png") && !Files.probeContentType(child).equals("image/jpeg") && !Files.probeContentType(child).equals("image/gif")) {
                        if (!suppressNotifications) displayNotification("Invalid File", filename.toString() + " is not a recognized image type.", trayIcon, MessageType.ERROR);
                        continue;
                    }
                    // else if (child.toFile().length() > 256000 && !Files.probeContentType(child).equals("image/gif")) {
                    //     if (!suppressNotifications) displayNotification("File Too Large", child.toFile().getName() + " is " + (double) child.toFile().length() / 1000000d + " MB. The maximum file size is 256kb.", trayIcon, MessageType.ERROR);
                    //     System.out.println("File delete status: " + child.toFile().delete());
                    //     continue;
                    // }
                    else {
                        System.out.println(filename.toFile().getName() + " found!");
                    }
                } catch (IOException e) {
                    System.err.println(e);
                }
                File emojiImage = dir.resolve(filename).toFile();
                String emojiName = emojiImage.getName().substring(0, emojiImage.getName().lastIndexOf('.'));
                try {
                    Thread.sleep(500);
                    CustomEmojiBuilder emote = new CustomEmojiBuilder(server).setImage(emojiImage).setName(emojiName);
                    KnownCustomEmoji createdEmoji = null;
                    try {
                        createdEmoji = emote.create().get();
                    }
                    catch (Exception e) {
                        displayNotification("Unable to Create Emoji", "There was an error when trying to create the emoji.", trayIcon, MessageType.ERROR);
                    }
                    if (createdEmoji != null) {
                        System.out.println("Created emoji " + createdEmoji.getName());
                        if (!suppressNotifications) displayNotification("Emoji Created", createdEmoji.getName() + " has been added to " + server.getName() + ".", trayIcon, MessageType.INFO);
                    }
                }
                catch (Exception e) {
                    System.err.println(e);
                    continue;
                }
                boolean valid = key.reset();
                if (!valid) break;
            }
        }
    }
}
