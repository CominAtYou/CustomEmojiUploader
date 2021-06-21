package com.cominatyou;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.awt.AWTException;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.TrayIcon.MessageType;
import java.io.File;
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

public class App {
    static void displayNotification(String title, String description, TrayIcon trayIcon, MessageType type) throws AWTException {
        trayIcon.displayMessage(title, description, type);
    }
    public static void main(String[] argv) throws AWTException, IOException, InterruptedException {
        DiscordApi api = new DiscordApiBuilder().setToken(Config.token).login().join();
        final String folderPath = "\\\\UbuntuNAS\\CDN\\DiscordEmotes";
        final boolean suppressNotifications = !SystemTray.isSupported();
        final TrayIcon trayIcon = SystemTray.isSupported() ? WinTrayIcon.createTrayIcon() : null;
        Path dir = Paths.get(folderPath);
        Server server = null;
        try {
            server = api.getServerById("766356648012283934").get();
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
            key = dir.register(watcher, ENTRY_CREATE);
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
                    // else if (tempFile.length() > 256000 && !Files.probeContentType(child).equals("image/gif")) {
                    //     if (!suppressNotifications) displayNotification("File Too Large", child.getFileName() + " is " + (double) tempFile.length() / 1000000d + " MB. The maximum file size is 256kb.", trayIcon, MessageType.ERROR);
                    //     System.out.println("File delete status: " + tempFile.delete());
                    //     continue;
                    // }
                    else {
                        System.out.println(filename.toFile().getName() + " found!");
                        // System.out.println("File delete status: " + tempFile.delete());
                    }
                } catch (IOException e) {
                    System.err.println(e);
                }
                File emojiImage = dir.resolve(filename).toFile();
                String emojiName = emojiImage.getName().substring(0, emojiImage.getName().lastIndexOf('.'));
                try {
                    // IOException (file in use) gets thrown if I don't delay execution
                    // There probably is a better way to handle this but I have already worked on this for five hours today, so this will suffice
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
