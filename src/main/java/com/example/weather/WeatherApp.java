package com.example.weather;

import com.example.weather.app.WeatherController;
import com.example.weather.shared.service.OpenMeteoService;
import com.example.weather.shared.service.WeatherService;
import com.example.weather.shared.widget.Labels;
import com.example.weather.shared.widget.Theme;
import com.example.weather.shared.widget.ThemeEngine;
import com.example.weather.app.MainFrame;

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
