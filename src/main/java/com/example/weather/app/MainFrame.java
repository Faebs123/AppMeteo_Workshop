package com.example.weather.app;

import com.example.weather.shared.model.DailyData;
import com.example.weather.shared.model.WeatherData;
import com.example.weather.shared.widget.GradientPanel;
import com.example.weather.shared.widget.Labels;
import com.example.weather.shared.widget.RoundedPanel;
import com.example.weather.shared.widget.StyledButton;
import com.example.weather.shared.widget.Theme;
import com.example.weather.current.CurrentPanel;
import com.example.weather.forecast.ForecastPanel;
import com.example.weather.search.HomePanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

public class MainFrame extends JFrame {
    private final JPanel contentPanel;
    private final JLabel loadingLabel;
    private final JLabel errorLabel;
    private final JTextField cityField;
    private final Labels labels;
    private final GradientPanel root;
    private final JLabel title;
    private final JPanel titlePanel;
    private WeatherController controller;
    private ForecastPanel currentForecastPanel;

    public MainFrame(Labels labels, Theme theme) {
        super("Meteo App");
        this.labels = labels;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(700, 720);
        setLocationRelativeTo(null);

        root = new GradientPanel(theme);
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(new EmptyBorder(24, 20, 20, 20));

        title = labels.title("Meteo App");

        titlePanel = new RoundedPanel(12);
        titlePanel.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
        titlePanel.setBackground(theme.cardBackground());
        titlePanel.setBorder(BorderFactory.createEmptyBorder(6, 20, 6, 20));
        titlePanel.add(title);
        titlePanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel titleWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        titleWrapper.setOpaque(false);
        titleWrapper.add(titlePanel);

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

        loadingLabel = labels.loading("Caricamento...");
        loadingLabel.setVisible(false);

        javax.swing.Timer pulse = new javax.swing.Timer(600, e -> {
            Color tp = theme.textPrimary();
            float a = loadingLabel.getForeground().getAlpha() == 200 ? 100 : 200;
            loadingLabel.setForeground(new Color(tp.getRed(), tp.getGreen(), tp.getBlue(), (int) a));
        });
        pulse.setRepeats(true);
        loadingLabel.addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                if (loadingLabel.isShowing()) pulse.start();
                else pulse.stop();
            }
        });

        errorLabel = labels.error("");
        errorLabel.setVisible(false);

        contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);

        root.add(titleWrapper);
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

    private void updateTheme(Theme theme) {
        root.applyTheme(theme);
        title.setForeground(theme.textPrimary());
        titlePanel.setBackground(theme.cardBackground());
    }

    public void showHome(String cityLabel, Labels labels, Theme theme) {
        clearContent();
        updateTheme(theme);
        contentPanel.add(new HomePanel(cityLabel,
            () -> controller.showCurrent(),
            () -> controller.showForecast(),
            labels));
        refresh();
    }

    public void showCurrentPanel(String cityLabel, WeatherData data, String description, Labels labels, Theme theme) {
        clearContent();
        updateTheme(theme);
        contentPanel.add(backHeader(cityLabel, () -> controller.showHome(), labels));
        contentPanel.add(Box.createVerticalStrut(6));
        contentPanel.add(new CurrentPanel(data, description, labels, theme));
        refresh();
    }

    public void showForecastPanel(String cityLabel, Labels labels, Theme theme) {
        clearContent();
        updateTheme(theme);
        contentPanel.add(backHeader(cityLabel, () -> controller.showHome(), labels));
        contentPanel.add(Box.createVerticalStrut(4));
        currentForecastPanel = new ForecastPanel(days -> controller.generateReport(days), labels, theme);
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

    private JPanel backHeader(String cityLabel, Runnable onBack, Labels labels) {
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

        JLabel label = labels.subheading(cityLabel);

        header.add(backBtn);
        header.add(label);
        return header;
    }
}
