package com.example.weather.app.view;

import com.example.weather.shared.model.DailyData;
import com.example.weather.shared.model.WeatherData;
import com.example.weather.shared.ui.GradientPanel;
import com.example.weather.shared.ui.Labels;
import com.example.weather.shared.ui.RoundedPanel;
import com.example.weather.shared.ui.SkeletonPanel;
import com.example.weather.shared.ui.StyledButton;
import com.example.weather.shared.model.Theme;
import com.example.weather.app.controller.WeatherController;
import com.example.weather.current.view.CurrentPanel;
import com.example.weather.forecast.view.ForecastPanel;
import com.example.weather.search.view.HomePanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class MainFrame extends JFrame {
    private final JPanel contentPanel;
    private final JLabel errorLabel;
    private final JTextField cityField;
    private final Labels labels;
    private final GradientPanel root;
    private final JLabel title;
    private final JPanel titlePanel;
    private final SkeletonPanel skeleton;
    private boolean searchDone;
    private WeatherController controller;
    private ForecastPanel currentForecastPanel;
    private final JLabel timeLabel;
    private Timer clockTimer;

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

        JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        searchRow.setOpaque(false);

        RoundedPanel glassBar = new RoundedPanel(24);
        glassBar.setLayout(new BorderLayout(0, 0));
        glassBar.setBackground(new Color(0, 0, 0, 100));
        glassBar.setBorder(BorderFactory.createEmptyBorder(2, 14, 2, 4));
        glassBar.setMaximumSize(new Dimension(380, 44));
        glassBar.setPreferredSize(new Dimension(380, 44));

        JLabel searchIcon = new JLabel("\uD83D\uDD0D");
        searchIcon.setFont(new Font("SansSerif", Font.PLAIN, 16));
        searchIcon.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));

        cityField = new JTextField(16);
        cityField.putClientProperty("JTextField.placeholderText", "Roma, Milano, Tokyo...");
        cityField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        cityField.setBorder(null);
        cityField.setOpaque(false);
        cityField.setForeground(Color.WHITE);
        cityField.setCaretColor(Color.WHITE);
        cityField.addActionListener(e -> onSearch());
        cityField.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                glassBar.setBackground(new Color(0, 0, 0, 150));
            }
            public void focusLost(FocusEvent e) {
                glassBar.setBackground(new Color(0, 0, 0, 100));
            }
        });

        JPopupMenu suggestionPopup = new JPopupMenu();
        suggestionPopup.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        suggestionPopup.setFocusable(false);

        DefaultListModel<String> listModel = new DefaultListModel<>();
        JList<String> suggestionList = new JList<>(listModel);
        suggestionList.setFont(new Font("SansSerif", Font.PLAIN, 13));
        suggestionList.setForeground(Color.WHITE);
        suggestionList.setBackground(new Color(30, 30, 30, 230));
        suggestionList.setSelectionBackground(new Color(59, 130, 246));
        suggestionList.setSelectionForeground(Color.WHITE);
        suggestionList.setFixedCellHeight(28);
        suggestionList.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));

        suggestionList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int idx = suggestionList.locationToIndex(e.getPoint());
                if (idx >= 0) {
                    selectSuggestion(listModel.getElementAt(idx), listModel, suggestionPopup);
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(suggestionList);
        scrollPane.setBorder(null);
        scrollPane.setPreferredSize(new Dimension(280, 140));
        suggestionPopup.add(scrollPane);
        suggestionPopup.setOpaque(false);

        javax.swing.Timer debounce = new javax.swing.Timer(300, e -> {
            String text = cityField.getText().trim();
            if (text.length() >= 1 && !searchDone && controller != null)
                controller.suggestCities(text, listModel, suggestionPopup, cityField);
        });
        debounce.setRepeats(false);

        cityField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { searchDone = false; debounce.restart(); }
            public void removeUpdate(DocumentEvent e) { searchDone = false; debounce.restart(); }
            public void changedUpdate(DocumentEvent e) { searchDone = false; debounce.restart(); }
        });
        cityField.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DOWN && suggestionPopup.isVisible()) {
                    int i = suggestionList.getSelectedIndex();
                    suggestionList.setSelectedIndex(Math.min(i + 1, listModel.getSize() - 1));
                    suggestionList.ensureIndexIsVisible(suggestionList.getSelectedIndex());
                }
                if (e.getKeyCode() == KeyEvent.VK_UP && suggestionPopup.isVisible()) {
                    int i = suggestionList.getSelectedIndex();
                    suggestionList.setSelectedIndex(Math.max(i - 1, 0));
                    suggestionList.ensureIndexIsVisible(suggestionList.getSelectedIndex());
                }
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    String selected = suggestionList.getSelectedValue();
                    if (selected != null && suggestionPopup.isVisible()) {
                        selectSuggestion(selected, listModel, suggestionPopup);
                    } else {
                        onSearch();
                    }
                }
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
                    suggestionPopup.setVisible(false);
            }
        });

        cityField.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) {
                Component opposite = e.getOppositeComponent();
                if (opposite == null || (!SwingUtilities.isDescendingFrom(opposite, suggestionPopup)
                    && opposite != cityField))
                    suggestionPopup.setVisible(false);
            }
        });

        JButton geoBtn = new JButton("\uD83C\uDFAF");
        geoBtn.setFont(new Font("SansSerif", Font.PLAIN, 18));
        geoBtn.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        geoBtn.setFocusPainted(false);
        geoBtn.setOpaque(false);
        geoBtn.setContentAreaFilled(false);
        geoBtn.setBorderPainted(false);
        geoBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        geoBtn.addActionListener(e -> onGeolocate());
        geoBtn.setToolTipText("Geolocalizzazione");

        JPanel glassLeft = new JPanel(new BorderLayout(0, 0));
        glassLeft.setOpaque(false);
        glassLeft.add(searchIcon, BorderLayout.WEST);
        glassLeft.add(cityField, BorderLayout.CENTER);

        glassBar.add(glassLeft, BorderLayout.CENTER);
        glassBar.add(geoBtn, BorderLayout.EAST);
        glassBar.setAlignmentX(Component.CENTER_ALIGNMENT);

        searchRow.add(glassBar);

        timeLabel = new JLabel("");
        timeLabel.setFont(new Font("SansSerif", Font.BOLD, 36));
        timeLabel.setForeground(new Color(255, 215, 0));
        timeLabel.setHorizontalAlignment(SwingConstants.CENTER);
        timeLabel.setVisible(false);

        errorLabel = labels.error("");
        errorLabel.setVisible(false);

        skeleton = new SkeletonPanel(480, 200, 4);
        skeleton.setVisible(false);

        contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);

        root.add(titleWrapper);
        root.add(Box.createVerticalStrut(14));
        root.add(searchRow);
        root.add(Box.createVerticalStrut(4));
        JPanel timeWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        timeWrapper.setOpaque(false);
        timeWrapper.add(timeLabel);
        root.add(timeWrapper);
        root.add(Box.createVerticalStrut(4));
        root.add(errorLabel);
        root.add(Box.createVerticalStrut(2));
        root.add(skeleton);
        root.add(Box.createVerticalStrut(12));
        root.add(contentPanel);

        add(root);
    }

    public void setController(WeatherController controller) {
        this.controller = controller;
    }

    private void onSearch() {
        String city = cityField.getText().trim();
        if (!city.isEmpty() && controller != null) {
            searchDone = true;
            controller.search(city);
        }
    }

    private void selectSuggestion(String entry, DefaultListModel<String> listModel, JPopupMenu popup) {
        String city = entry.replaceFirst(",.*", "").trim();
        cityField.setText(city);
        popup.setVisible(false);
        searchDone = true;
        onSearch();
    }

    private void onGeolocate() {
        if (controller != null) controller.geolocate();
    }

    public void showLoading(boolean visible) {
        skeleton.setVisible(visible);
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

    public void setTimezone(String tz) {
        if (clockTimer != null) clockTimer.stop();
        if (tz == null || tz.isBlank()) {
            timeLabel.setVisible(false);
            return;
        }
        timeLabel.setVisible(true);
        clockTimer = new Timer(1000, e -> {
            ZonedDateTime now = ZonedDateTime.now(ZoneId.of(tz));
            timeLabel.setText("\uD83D\uDD50 " + now.format(DateTimeFormatter.ofPattern("HH:mm")));
        });
        clockTimer.start();
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
        currentForecastPanel = new ForecastPanel(dates -> controller.generateReport(dates[0], dates[1]), labels, theme);
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
