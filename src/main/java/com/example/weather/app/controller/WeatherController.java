package com.example.weather.app.controller;

import com.example.weather.shared.model.DailyData;
import com.example.weather.shared.model.GeocodeResult;
import com.example.weather.shared.model.WeatherData;
import com.example.weather.shared.service.WeatherService;
import com.example.weather.app.view.MainFrame;

import javax.swing.*;
import java.util.List;

public class WeatherController {
    private final WeatherService service;
    private final MainFrame view;
    private WeatherData lastData;
    private double lastLat;
    private double lastLon;

    public WeatherController(WeatherService service, MainFrame view) {
        this.service = service;
        this.view = view;
    }

    public void search(String city) {
        view.clearError();
        view.showLoading(true);
        view.clearContent();

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                GeocodeResult loc = service.geocode(city);
                lastData = service.fetchCurrent(loc);
                lastLat = loc.lat();
                lastLon = loc.lon();
                return null;
            }

            @Override
            protected void done() {
                view.showLoading(false);
                try {
                    get();
                    showHome();
                } catch (Exception e) {
                    Throwable cause = e.getCause();
                    view.showError(cause != null ? cause.getMessage() : e.getMessage());
                }
            }
        };
        worker.execute();
    }

    public void showHome() {
        String label = lastData.city() + (lastData.country().isEmpty() ? "" : ", " + lastData.country());
        view.showHome(label);
    }

    public void showCurrent() {
        String desc = weatherDescription(lastData.code());
        view.showCurrentPanel(
            lastData.city() + (lastData.country().isEmpty() ? "" : ", " + lastData.country()),
            lastData, desc);
    }

    public void showForecast() {
        String label = lastData.city() + (lastData.country().isEmpty() ? "" : ", " + lastData.country());
        view.showForecastPanel(label);
    }

    public void generateReport(int days) {
        SwingWorker<List<DailyData>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<DailyData> doInBackground() throws Exception {
                return service.fetchForecast(lastLat, lastLon, days);
            }

            @Override
            protected void done() {
                try {
                    List<DailyData> data = get();
                    SwingUtilities.invokeLater(() -> updateChart(data));
                } catch (Exception e) {
                    Throwable cause = e.getCause();
                    view.showError(cause != null ? cause.getMessage() : e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void updateChart(List<DailyData> data) {
        view.updateChart(data);
    }

    private String weatherDescription(int code) {
        if (code == 0) return "\u2600\uFE0F Sereno";
        if (code <= 3) return "\u26C5 Nuvoloso";
        if (code <= 48) return "\uD83C\uDF2B\uFE0F Nebbia";
        if (code <= 57) return "\uD83C\uDF26 Pioggerella";
        if (code <= 67) return "\uD83C\uDF27 Pioggia";
        if (code <= 77) return "\u2744\uFE0F Neve";
        if (code <= 82) return "\uD83C\uDF28 Rovesci";
        if (code <= 86) return "\uD83C\uDF28 Nevischio";
        return "\u26A1 Temporale";
    }
}
