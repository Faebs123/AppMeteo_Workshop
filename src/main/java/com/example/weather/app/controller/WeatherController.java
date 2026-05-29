package com.example.weather.app.controller;

import com.example.weather.shared.model.DailyData;
import com.example.weather.shared.model.GeocodeResult;
import com.example.weather.shared.model.WeatherData;
import com.example.weather.shared.service.WeatherService;
import com.example.weather.shared.ui.Labels;
import com.example.weather.shared.model.Theme;
import com.example.weather.shared.service.ThemeEngine;
import com.example.weather.app.view.MainFrame;

import javax.swing.*;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

public class WeatherController {
    private final WeatherService service;
    private final MainFrame view;
    private WeatherData lastData;
    private double lastLat;
    private double lastLon;
    private String timezone;

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
                timezone = loc.timezone();
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

    private Theme themeForTimezone() {
        String tz = timezone != null ? timezone : "UTC";
        try {
            int hour = ZonedDateTime.now(ZoneId.of(tz)).getHour();
            return ThemeEngine.forHour(hour);
        } catch (Exception e) {
            return lastData != null && lastData.day() ? Theme.DAY : Theme.NIGHT;
        }
    }

    public void showHome() {
        String label = cityLabel();
        Theme theme = themeForTimezone();
        Labels labels = new Labels(theme);
        view.setTimezone(timezone);
        view.showHome(label, labels, theme);
    }

    public void showCurrent() {
        String label = cityLabel();
        String desc = weatherDescription(lastData.code());
        Theme theme = themeForTimezone();
        Labels labels = new Labels(theme);
        view.setTimezone(timezone);
        view.showCurrentPanel(label, lastData, desc, labels, theme);
    }

    public void showForecast() {
        String label = cityLabel();
        Theme theme = themeForTimezone();
        Labels labels = new Labels(theme);
        view.setTimezone(timezone);
        view.showForecastPanel(label, labels, theme);
    }

    private String cityLabel() {
        return lastData.city() + (lastData.country().isEmpty() ? "" : ", " + lastData.country());
    }

    public void suggestCities(String query, DefaultListModel<String> listModel, JPopupMenu popup, JTextField field) {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                java.util.List<String[]> suggestions = service.suggestCities(query);
                SwingUtilities.invokeLater(() -> {
                    listModel.clear();
                    if (suggestions.isEmpty()) { popup.setVisible(false); return; }
                    if (suggestions.size() == 1) {
                        popup.setVisible(false);
                        return;
                    }
                    for (String[] s : suggestions)
                        listModel.addElement(s[0] + (s[1].isEmpty() ? "" : ", " + s[1]));
                    popup.show(field, 0, field.getHeight());
                    field.requestFocusInWindow();
                });
                return null;
            }
        };
        worker.execute();
    }

    public void geolocate() {
        view.showError("Geolocalizzazione non disponibile. Digita il nome della citt\u00e0.");
    }

    public void generateReport(String startDate, String endDate) {
        SwingWorker<List<DailyData>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<DailyData> doInBackground() throws Exception {
                return service.fetchForecast(lastLat, lastLon, startDate, endDate);
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
