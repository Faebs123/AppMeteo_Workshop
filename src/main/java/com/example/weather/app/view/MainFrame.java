package com.example.weather.app.view;

import com.example.weather.app.controller.WeatherController;
import com.example.weather.shared.model.DailyData;
import com.example.weather.shared.model.WeatherData;
import com.example.weather.shared.widget.GradientPanel;
import com.example.weather.shared.widget.StyledButton;
import com.example.weather.current.view.CurrentPanel;
import com.example.weather.forecast.view.ForecastPanel;
import com.example.weather.search.view.HomePanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

public class MainFrame extends JFrame {
    private final JPanel contentPanel;
    private final JLabel loadingLabel;
    private final JLabel errorLabel;
    private final JTextField cityField;
    private WeatherController controller;
    private ForecastPanel currentForecastPanel;

    public MainFrame() {
        super("Meteo App");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(700, 720);
        setLocationRelativeTo(null);

        JPanel root = new GradientPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(new EmptyBorder(24, 20, 20, 20));

        JLabel title = new JLabel("Meteo App", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 26));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        searchRow.setOpaque(false);

        cityField = new JTextField(18);
        cityField.putClientProperty("JTextField.placeholderText", "Roma, Milano, Tokyo...");
        cityField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        cityField.setPreferredSize(new Dimension(220, 40));
        cityField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(255, 255, 255, 80), 1, true),
            BorderFactory.createEmptyBorder(8, 14, 8, 14)));
        cityField.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                cityField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(255, 255, 255, 200), 2, true),
                    BorderFactory.createEmptyBorder(7, 13, 7, 13)));
            }
            public void focusLost(FocusEvent e) {
                cityField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(255, 255, 255, 80), 1, true),
                    BorderFactory.createEmptyBorder(8, 14, 8, 14)));
            }
        });

        JButton searchBtn = new StyledButton("CERCA", new Color(59, 130, 246));
        searchBtn.addActionListener(e -> onSearch());
        cityField.addActionListener(e -> onSearch());

        searchRow.add(cityField);
        searchRow.add(searchBtn);

        loadingLabel = new JLabel("Caricamento...", SwingConstants.CENTER);
        loadingLabel.setFont(new Font("SansSerif", Font.PLAIN, 16));
        loadingLabel.setForeground(new Color(255, 255, 255, 200));
        loadingLabel.setVisible(false);
        loadingLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        javax.swing.Timer pulse = new javax.swing.Timer(600, e -> {
            float a = loadingLabel.getForeground().getAlpha() == 200 ? 100 : 200;
            loadingLabel.setForeground(new Color(255, 255, 255, (int) a));
        });
        pulse.setRepeats(true);
        loadingLabel.addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                if (loadingLabel.isShowing()) pulse.start();
                else pulse.stop();
            }
        });

        errorLabel = new JLabel("", SwingConstants.CENTER);
        errorLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        errorLabel.setForeground(new Color(255, 200, 200));
        errorLabel.setVisible(false);
        errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);

        root.add(title);
        root.add(Box.createVerticalStrut(14));
        root.add(searchRow);
        root.add(Box.createVerticalStrut(6));
        root.add(errorLabel);
        root.add(Box.createVerticalStrut(2));
        root.add(loadingLabel);
        root.add(Box.createVerticalStrut(12));
        root.add(contentPanel);

        add(root);
    }

    public void setController(WeatherController controller) {
        this.controller = controller;
    }

    private void onSearch() {
        String city = cityField.getText().trim();
        if (!city.isEmpty() && controller != null)
            controller.search(city);
    }

    public void showLoading(boolean visible) {
        loadingLabel.setVisible(visible);
    }

    public void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    public void clearError() {
        errorLabel.setVisible(false);
    }

    public void clearContent() {
        contentPanel.removeAll();
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    public void showHome(String cityLabel) {
        clearContent();
        contentPanel.add(new HomePanel(cityLabel,
            () -> controller.showCurrent(),
            () -> controller.showForecast()));
        refresh();
    }

    public void showCurrentPanel(String cityLabel, WeatherData data, String description) {
        clearContent();
        contentPanel.add(backHeader(cityLabel, () -> controller.showHome()));
        contentPanel.add(Box.createVerticalStrut(6));
        contentPanel.add(new CurrentPanel(data, description));
        refresh();
    }

    public void showForecastPanel(String cityLabel) {
        clearContent();
        contentPanel.add(backHeader(cityLabel, () -> controller.showHome()));
        contentPanel.add(Box.createVerticalStrut(4));
        currentForecastPanel = new ForecastPanel(days -> controller.generateReport(days));
        contentPanel.add(currentForecastPanel);
        refresh();
    }

    public void updateChart(java.util.List<DailyData> data) {
        if (currentForecastPanel != null)
            currentForecastPanel.setChart(data);
        refresh();
    }

    private void refresh() {
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private JPanel backHeader(String cityLabel, Runnable onBack) {
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        header.setOpaque(false);
        header.setMaximumSize(new Dimension(700, 40));
        header.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton backBtn = new JButton("\u25C0");
        backBtn.setFont(new Font("SansSerif", Font.PLAIN, 18));
        backBtn.setForeground(Color.WHITE);
        backBtn.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        backBtn.setFocusPainted(false);
        backBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backBtn.setOpaque(false);
        backBtn.setContentAreaFilled(false);
        backBtn.setBorderPainted(false);
        backBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { backBtn.setForeground(new Color(180, 220, 255)); }
            public void mouseExited(MouseEvent e) { backBtn.setForeground(Color.WHITE); }
        });
        backBtn.addActionListener(e -> onBack.run());

        JLabel label = new JLabel(cityLabel);
        label.setFont(new Font("SansSerif", Font.BOLD, 14));
        label.setForeground(new Color(255, 255, 255, 220));

        header.add(backBtn);
        header.add(label);
        return header;
    }
}
