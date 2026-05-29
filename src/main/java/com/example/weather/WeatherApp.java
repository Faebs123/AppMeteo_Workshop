package com.example.weather;

import com.example.weather.app.WeatherController;
import com.example.weather.shared.service.OpenMeteoService;
import com.example.weather.shared.service.WeatherService;
import com.example.weather.shared.widget.Labels;
import com.example.weather.shared.widget.Theme;
import com.example.weather.app.MainFrame;

import javax.swing.*;

public class WeatherApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            WeatherService service = new OpenMeteoService();
            Theme theme = Theme.NIGHT;
            Labels labels = new Labels(theme);
            MainFrame frame = new MainFrame(labels, theme);
            WeatherController controller = new WeatherController(service, frame);
            frame.setController(controller);
            frame.setVisible(true);
        });
    }
}
