package com.example.weather;

import com.example.weather.app.controller.WeatherController;
import com.example.weather.shared.service.OpenMeteoService;
import com.example.weather.shared.service.WeatherService;
import com.example.weather.shared.ui.Labels;
import com.example.weather.shared.model.Theme;
import com.example.weather.shared.service.ThemeEngine;
import com.example.weather.app.view.MainFrame;

import javax.swing.*;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class WeatherApp {
    public static void main(String[] args) {
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
}
