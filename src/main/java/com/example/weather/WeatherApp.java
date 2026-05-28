package com.example.weather;

import com.example.weather.app.controller.WeatherController;
import com.example.weather.shared.service.OpenMeteoService;
import com.example.weather.shared.service.WeatherService;
import com.example.weather.app.view.MainFrame;

import javax.swing.*;

public class WeatherApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            WeatherService service = new OpenMeteoService();
            MainFrame frame = new MainFrame();
            WeatherController controller = new WeatherController(service, frame);
            frame.setController(controller);
            frame.setVisible(true);
        });
    }
}
