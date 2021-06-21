package com.cominatyou;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class WinTrayIcon {
    public static TrayIcon createTrayIcon() throws AWTException {
        ActionListener listener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (e.getActionCommand().equals("Exit")) {
                    System.exit(0);
                }
            }
        };
        SystemTray tray = SystemTray.getSystemTray();
        Image image = Toolkit.getDefaultToolkit().createImage(WinTrayIcon.class.getResource("/trayicon.png"));
        PopupMenu popup = new PopupMenu();
        MenuItem exitMenuItem = new MenuItem("Exit");
        popup.add(exitMenuItem);
        popup.addActionListener(listener);
        TrayIcon trayIcon = new TrayIcon(image, "Custom Emoji Uploader", popup);
        trayIcon.setImageAutoSize(true);
        trayIcon.setToolTip("Custom Emoji Uploader");
        tray.add(trayIcon);
        return trayIcon;
    }
}
