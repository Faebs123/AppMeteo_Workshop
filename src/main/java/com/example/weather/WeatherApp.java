package com.example.weather;

import com.example.weather.app.controller.WeatherController;
import com.example.weather.shared.service.OpenMeteoService;
import com.example.weather.shared.service.WeatherService;
import com.example.weather.shared.ui.Labels;
import com.example.weather.shared.model.Theme;
import com.example.weather.shared.service.ThemeEngine;
import com.example.weather.app.view.MainFrame;

import com.formdev.flatlaf.FlatDarculaLaf;

import javax.swing.*;
import java.awt.*;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class WeatherApp {
    public static void main(String[] args) {
        FlatDarculaLaf.setup();
        configureWeatherTheme();

        SwingUtilities.invokeLater(() -> {
            int hour = ZonedDateTime.now(ZoneId.systemDefault()).getHour();
            Theme theme = ThemeEngine.forHour(hour);
            Labels labels = new Labels(theme);
            WeatherService service = new OpenMeteoService();
            MainFrame frame = new MainFrame(labels, theme);
            WeatherController controller = new WeatherController(service, frame);
            frame.setController(controller);
            frame.setVisible(true);
        });
    }

    private static void configureWeatherTheme() {
        Color nightBlue = new Color(0x1a, 0x2a, 0x3a);
        Color mediumBlue = new Color(0x2c, 0x53, 0x64);
        Color skyBlue = new Color(0x4F, 0xC3, 0xF7);
        Color sunGold = new Color(0xFF, 0xD7, 0x00);
        Color warmOrange = new Color(0xFF, 0x6B, 0x35);
        Color darkBg = new Color(0x0f, 0x20, 0x27);

        UIManager.put("Panel.background", darkBg);
        UIManager.put("Panel.foreground", Color.WHITE);

        UIManager.put("TextField.background", mediumBlue);
        UIManager.put("TextField.foreground", Color.WHITE);
        UIManager.put("TextField.caretForeground", Color.WHITE);
        UIManager.put("TextField.selectionBackground", skyBlue);
        UIManager.put("TextField.selectionForeground", Color.BLACK);
        UIManager.put("TextField.arc", 12);

        UIManager.put("FormattedTextField.background", mediumBlue);
        UIManager.put("FormattedTextField.foreground", Color.WHITE);
        UIManager.put("FormattedTextField.arc", 12);

        UIManager.put("Spinner.background", mediumBlue);
        UIManager.put("Spinner.foreground", Color.WHITE);
        UIManager.put("Spinner.buttonBackground", nightBlue);
        UIManager.put("Spinner.buttonForeground", Color.WHITE);
        UIManager.put("Component.arc", 12);

        UIManager.put("Button.background", mediumBlue);
        UIManager.put("Button.foreground", Color.WHITE);
        UIManager.put("Button.arc", 16);
        UIManager.put("Button.hoverBackground", skyBlue);
        UIManager.put("Button.focusedBackground", mediumBlue);
        UIManager.put("Button.default.background", skyBlue);

        UIManager.put("Label.foreground", Color.WHITE);
        UIManager.put("Label.disabledForeground", new Color(180, 180, 180));

        UIManager.put("ScrollPane.background", darkBg);
        UIManager.put("ScrollPane.foreground", Color.WHITE);
        UIManager.put("ScrollBar.thumb", skyBlue);
        UIManager.put("ScrollBar.thumbArc", 12);
        UIManager.put("ScrollBar.track", darkBg);
        UIManager.put("ScrollBar.width", 10);

        UIManager.put("List.background", new Color(0x1e, 0x1e, 0x2e));
        UIManager.put("List.foreground", Color.WHITE);
        UIManager.put("List.selectionBackground", skyBlue);
        UIManager.put("List.selectionForeground", Color.BLACK);

        UIManager.put("Separator.foreground", new Color(255, 255, 255, 50));

        UIManager.put("PopupMenu.background", new Color(0x1e, 0x1e, 0x2e));
        UIManager.put("PopupMenu.foreground", Color.WHITE);

        UIManager.put("OptionPane.background", darkBg);
        UIManager.put("OptionPane.foreground", Color.WHITE);
        UIManager.put("OptionPane.messageForeground", Color.WHITE);

        UIManager.put("Component.focusColor", skyBlue);
        UIManager.put("Component.focusWidth", 2);

        UIManager.put("ProgressBar.arc", 12);
        UIManager.put("ProgressBar.foreground", sunGold);

        String fontName = "SansSerif";
        if (Font.decode(fontName).getFamily().equals(fontName)) {
            UIManager.put("defaultFont", new Font(fontName, Font.PLAIN, 14));
        }
    }
}
