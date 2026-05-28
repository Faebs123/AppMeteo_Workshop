package com.example.weather;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class WeatherApp {
    private final HttpClient client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2).build();
    private final ObjectMapper mapper = new ObjectMapper();
    private JFrame frame;
    private JPanel contentPanel;
    private JLabel loadingLabel, errorLabel;
    private JTextField cityField;
    private WeatherData lastData;
    private double lastLat, lastLon;

    private record WeatherData(String city, String country, double temp, double feelsLike, int humidity, double wind, int code) {}
    private record DailyData(String date, double tempMax, double tempMin, double windMax) {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new WeatherApp().createAndShow());
    }

    private void createAndShow() {
        frame = new JFrame("Meteo App");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(700, 720);
        frame.setLocationRelativeTo(null);

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

        searchBtn.addActionListener(e -> search());
        cityField.addActionListener(e -> search());

        root.add(title);
        root.add(Box.createVerticalStrut(14));
        root.add(searchRow);
        root.add(Box.createVerticalStrut(6));
        root.add(errorLabel);
        root.add(Box.createVerticalStrut(2));
        root.add(loadingLabel);
        root.add(Box.createVerticalStrut(12));
        root.add(contentPanel);

        frame.add(root);
        frame.setVisible(true);
    }

    // --- Logic ---

    private void search() {
        String city = cityField.getText().trim();
        if (city.isEmpty()) return;
        errorLabel.setVisible(false);
        contentPanel.removeAll();
        contentPanel.revalidate();
        contentPanel.repaint();
        loadingLabel.setVisible(true);
        fetchWeather(city);
    }

    private void fetchWeather(String city) {
        String encoded = URLEncoder.encode(city, StandardCharsets.UTF_8);
        String geoUrl = "https://geocoding-api.open-meteo.com/v1/search?name=" + encoded
                + "&count=1&language=it&format=json";

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                JsonNode first = geocode(geoUrl, city);
                String cityName = first.get("name").asText();
                String country = first.has("country_code") ? first.get("country_code").asText().toUpperCase() : "";
                lastLat = first.get("latitude").asDouble();
                lastLon = first.get("longitude").asDouble();

                String weatherUrl = String.format(
                    "https://api.open-meteo.com/v1/forecast?latitude=%f&longitude=%f" +
                    "&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m",
                    lastLat, lastLon);
                JsonNode w = fetchJson(weatherUrl).at("/current");

                lastData = new WeatherData(cityName, country,
                    w.get("temperature_2m").asDouble(),
                    w.get("apparent_temperature").asDouble(),
                    w.get("relative_humidity_2m").asInt(),
                    w.get("wind_speed_10m").asDouble(),
                    w.get("weather_code").asInt());
                return null;
            }

            @Override
            protected void done() {
                loadingLabel.setVisible(false);
                try { get(); showChoiceButtons(); }
                catch (Exception e) {
                    errorLabel.setText(e.getCause().getMessage());
                    errorLabel.setVisible(true);
                }
            }
        };
        worker.execute();
    }

    // --- Views ---

    private void showChoiceButtons() {
        contentPanel.removeAll();

        JLabel cityLabel = new JLabel(lastData.city() + (lastData.country().isEmpty() ? "" : ", " + lastData.country()), SwingConstants.CENTER);
        cityLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
        cityLabel.setForeground(new Color(255, 255, 255, 218));
        cityLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 18, 0));
        btnRow.setOpaque(false);

        JButton currentBtn = new StyledButton("Condizioni attuali", new Color(5, 150, 105));
        currentBtn.addActionListener(e -> showCurrent());
        JButton reportBtn = new StyledButton("Report giornaliero", new Color(217, 119, 6));
        reportBtn.addActionListener(e -> showReport());

        btnRow.add(currentBtn);
        btnRow.add(reportBtn);

        contentPanel.add(cityLabel);
        contentPanel.add(Box.createVerticalStrut(6));
        contentPanel.add(btnRow);
        refresh();
    }

    private void showCurrent() {
        contentPanel.removeAll();

        contentPanel.add(backHeader());
        contentPanel.add(Box.createVerticalStrut(6));

        RoundedPanel card = new RoundedPanel(20);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setOpaque(false);
        card.setBackground(new Color(255, 255, 255, 55));
        card.setBorder(new EmptyBorder(28, 32, 28, 32));
        card.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.setMaximumSize(new Dimension(420, 420));

        JLabel tempLabel = new JLabel(String.format("%.1f°", lastData.temp()), SwingConstants.CENTER);
        tempLabel.setFont(new Font("SansSerif", Font.PLAIN, 58));
        tempLabel.setForeground(Color.WHITE);
        tempLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel descLabel = new JLabel(weatherDescription(lastData.code()), SwingConstants.CENTER);
        descLabel.setFont(new Font("SansSerif", Font.PLAIN, 16));
        descLabel.setForeground(new Color(255, 255, 255, 230));
        descLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel feelsLabel = new JLabel(String.format("Percepita %.1f°", lastData.feelsLike()), SwingConstants.CENTER);
        feelsLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        feelsLabel.setForeground(new Color(255, 255, 255, 179));
        feelsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JSeparator sep = new JSeparator(SwingConstants.HORIZONTAL);
        sep.setMaximumSize(new Dimension(160, 1));
        sep.setForeground(new Color(255, 255, 255, 50));

        JPanel details = new JPanel(new FlowLayout(FlowLayout.CENTER, 36, 0));
        details.setOpaque(false);
        details.add(detailBox("Umidit\u00e0", lastData.humidity() + "%"));
        details.add(detailBox("Vento", String.format("%.0f km/h", lastData.wind())));

        card.add(tempLabel);
        card.add(Box.createVerticalStrut(4));
        card.add(descLabel);
        card.add(Box.createVerticalStrut(4));
        card.add(feelsLabel);
        card.add(Box.createVerticalStrut(14));
        card.add(sep);
        card.add(Box.createVerticalStrut(14));
        card.add(details);

        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setOpaque(false);
        wrapper.add(card);

        contentPanel.add(wrapper);
        refresh();
    }

    private JPanel backHeader() {
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
        backBtn.addActionListener(e -> showChoiceButtons());

        JLabel cityLabel = new JLabel(lastData.city() + (lastData.country().isEmpty() ? "" : ", " + lastData.country()));
        cityLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        cityLabel.setForeground(new Color(255, 255, 255, 220));

        header.add(backBtn);
        header.add(cityLabel);
        return header;
    }

    private void showReport() {
        contentPanel.removeAll();

        contentPanel.add(backHeader());
        contentPanel.add(Box.createVerticalStrut(4));

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        controls.setOpaque(false);

        JLabel dayLabel = new JLabel("Giorni:");
        dayLabel.setForeground(new Color(255, 255, 255, 204));

        JSpinner daySpinner = new JSpinner(new SpinnerNumberModel(7, 1, 16, 1));
        daySpinner.setPreferredSize(new Dimension(70, 30));

        JButton generateBtn = new StyledButton("Genera", new Color(59, 130, 246));
        JPanel chartContainer = new JPanel(new BorderLayout());
        chartContainer.setOpaque(false);

        controls.add(dayLabel);
        controls.add(daySpinner);
        controls.add(generateBtn);

        contentPanel.add(controls);

        generateBtn.addActionListener(e -> {
            chartContainer.removeAll();
            chartContainer.revalidate();
            chartContainer.repaint();

            String weatherUrl = String.format(
                "https://api.open-meteo.com/v1/forecast?latitude=%f&longitude=%f" +
                "&daily=temperature_2m_max,temperature_2m_min,wind_speed_10m_max" +
                "&timezone=auto&forecast_days=%d", lastLat, lastLon, (int) daySpinner.getValue());

            SwingWorker<List<DailyData>, Void> worker = new SwingWorker<>() {
                @Override
                protected List<DailyData> doInBackground() throws Exception {
                    JsonNode daily = fetchJson(weatherUrl).get("daily");
                    List<DailyData> list = new ArrayList<>();
                    JsonNode dates = daily.get("time");
                    JsonNode tMax = daily.get("temperature_2m_max");
                    JsonNode tMin = daily.get("temperature_2m_min");
                    JsonNode wMax = daily.get("wind_speed_10m_max");
                    for (int i = 0; i < dates.size(); i++) {
                        list.add(new DailyData(
                            dates.get(i).asText(),
                            tMax.get(i).asDouble(),
                            tMin.get(i).asDouble(),
                            wMax.get(i).asDouble()));
                    }
                    return list;
                }

                @Override
                protected void done() {
                    try {
                        List<DailyData> data = get();
                        int days = (int) daySpinner.getValue();
                        chartContainer.add(buildChart(data, days), BorderLayout.CENTER);
                        chartContainer.revalidate();
                        chartContainer.repaint();
                    } catch (Exception ex) {
                        errorLabel.setText(ex.getCause().getMessage());
                        errorLabel.setVisible(true);
                    }
                }
            };
            worker.execute();
        });

        contentPanel.add(chartContainer);
        refresh();
    }

    private JPanel buildChart(List<DailyData> data, int days) {
        double avgTMax = data.stream().mapToDouble(DailyData::tempMax).average().orElse(0);
        double avgTMin = data.stream().mapToDouble(DailyData::tempMin).average().orElse(0);
        double avgWind = data.stream().mapToDouble(DailyData::windMax).average().orElse(0);

        JPanel avgs = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        avgs.setOpaque(false);
        avgs.setBorder(new EmptyBorder(10, 0, 6, 0));
        avgs.add(avgBox("T Max media", String.format("%.1f\u00b0C", avgTMax), new Color(255, 107, 107)));
        avgs.add(avgBox("T Min media", String.format("%.1f\u00b0C", avgTMin), new Color(78, 205, 196)));
        avgs.add(avgBox("Vento medio", String.format("%.0f km/h", avgWind), new Color(255, 230, 109)));

        ChartPanel chart = new ChartPanel(data);

        JPanel legend = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        legend.setOpaque(false);
        legend.setBorder(new EmptyBorder(6, 0, 0, 0));
        Color[] cols = {new Color(255, 107, 107), new Color(78, 205, 196), new Color(255, 230, 109)};
        String[] names = {"T Max", "T Min", "Vento"};
        for (int i = 0; i < 3; i++) {
            final Color ci = cols[i];
            final String ni = names[i];
            JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
            item.setOpaque(false);
            JPanel dot = new JPanel() {
                public Dimension getPreferredSize() { return new Dimension(10, 10); }
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(ci);
                    g2.fillOval(0, 0, 10, 10);
                }
            };
            dot.setOpaque(false);
            JLabel lbl = new JLabel(ni);
            lbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
            lbl.setForeground(new Color(255, 255, 255, 210));
            item.add(dot);
            item.add(lbl);
            legend.add(item);
        }

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(avgs, BorderLayout.NORTH);
        wrapper.add(chart, BorderLayout.CENTER);
        wrapper.add(legend, BorderLayout.SOUTH);
        return wrapper;
    }

    // --- Helpers ---

    private JsonNode geocode(String geoUrl, String city) throws Exception {
        JsonNode geoData = fetchJson(geoUrl);
        JsonNode results = geoData.get("results");
        if (results == null || !results.isArray() || results.isEmpty())
            throw new RuntimeException("Localit\u00e0 \"" + city + "\" non trovata.");
        return results.get(0);
    }

    private JsonNode fetchJson(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200)
            throw new RuntimeException("Errore API: " + resp.statusCode() + " " + resp.body());
        return mapper.readTree(resp.body());
    }

    // --- Custom components ---

    static class StyledButton extends JButton {
        private final Color baseColor;
        private final Color hoverColor;
        private final Color pressColor;
        private boolean hovered, pressed;

        StyledButton(String text, Color base) {
            super(text);
            this.baseColor = base;
            this.hoverColor = base.darker();
            this.pressColor = new Color(
                Math.max(0, base.getRed() - 60),
                Math.max(0, base.getGreen() - 60),
                Math.max(0, base.getBlue() - 60));
            setFont(new Font("SansSerif", Font.BOLD, 14));
            setForeground(Color.WHITE);
            setBorder(BorderFactory.createEmptyBorder(11, 28, 11, 28));
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) { pressed = true; repaint(); }
                public void mouseReleased(MouseEvent e) { pressed = false; repaint(); }
                public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                public void mouseExited(MouseEvent e) { hovered = false; pressed = false; repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight(), r = 24;
            if (pressed) {
                g2.setColor(pressColor);
                g2.fillRoundRect(1, 1, w - 2, h - 2, r, r);
            } else if (hovered) {
                g2.setColor(hoverColor);
                g2.fillRoundRect(0, 0, w, h, r, r);
                g2.setColor(new Color(255, 255, 255, 30));
                g2.fillRoundRect(0, 0, w, h / 2, r, r);
            } else {
                g2.setColor(baseColor);
                g2.fillRoundRect(0, 0, w, h, r, r);
            }
            super.paintComponent(g2);
            g2.dispose();
        }
    }

    private JPanel detailBox(String label, String value) {
        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setOpaque(false);
        JLabel val = new JLabel(value, SwingConstants.CENTER);
        val.setFont(new Font("SansSerif", Font.BOLD, 18));
        val.setForeground(Color.WHITE);
        val.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel lbl = new JLabel(label, SwingConstants.CENTER);
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lbl.setForeground(new Color(255, 255, 255, 179));
        lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        box.add(val);
        box.add(lbl);
        return box;
    }

    private JPanel avgBox(String label, String value, Color color) {
        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setOpaque(false);
        box.setBackground(new Color(255, 255, 255, 30));
        box.setBorder(new EmptyBorder(10, 14, 10, 14));
        JLabel val = new JLabel(value, SwingConstants.CENTER);
        val.setFont(new Font("SansSerif", Font.BOLD, 16));
        val.setForeground(color);
        val.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel lbl = new JLabel(label, SwingConstants.CENTER);
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lbl.setForeground(new Color(255, 255, 255, 179));
        lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        box.add(val);
        box.add(lbl);
        return box;
    }

    private void refresh() {
        contentPanel.revalidate();
        contentPanel.repaint();
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

    static class GradientPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            int w = getWidth(), h = getHeight();
            GradientPaint gp = new GradientPaint(0, 0, new Color(0x0f2027),
                0, h, new Color(0x2c5364));
            g2.setPaint(gp);
            g2.fillRect(0, 0, w, h);
        }
    }

    static class RoundedPanel extends JPanel {
        private final int radius;
        public RoundedPanel(int radius) { this.radius = radius; setOpaque(false); }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            g2.dispose();
        }
    }

    static class ChartPanel extends JPanel {
        private final List<DailyData> data;
        private static final Color[] COLORS = {
            new Color(255, 107, 107),
            new Color(78, 205, 196),
            new Color(255, 230, 109)
        };

        ChartPanel(List<DailyData> data) {
            this.data = data;
            setOpaque(false);
            setPreferredSize(new Dimension(480, 300));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (data.isEmpty()) return;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            int padL = 50, padR = 24, padT = 24, padB = 48;
            int w = getWidth(), h = getHeight();
            int cw = w - padL - padR, ch = h - padT - padB;

            if (cw < 10 || ch < 10) { g2.dispose(); return; }

            double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
            for (DailyData d : data) {
                min = Math.min(min, Math.min(d.tempMin(), Math.min(d.tempMax(), d.windMax())));
                max = Math.max(max, Math.max(d.tempMax(), Math.max(d.tempMin(), d.windMax())));
            }
            double range = max - min;
            if (range < 1) range = 1;

            int n = data.size();
            double[][] vals = new double[3][n];
            for (int i = 0; i < n; i++) {
                vals[0][i] = data.get(i).tempMax();
                vals[1][i] = data.get(i).tempMin();
                vals[2][i] = data.get(i).windMax();
            }

            // Grid lines & axis labels
            g2.setColor(new Color(255, 255, 255, 30));
            int ticks = 4;
            for (int t = 0; t <= ticks; t++) {
                int y = padT + ch * t / ticks;
                g2.drawLine(padL, y, padL + cw, y);
                String lbl = String.format("%.0f", max - range * t / ticks);
                g2.setColor(new Color(255, 255, 255, 150));
                g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
                g2.drawString(lbl, 2, y + 4);
                g2.setColor(new Color(255, 255, 255, 30));
            }

            // X labels
            g2.setColor(new Color(255, 255, 255, 150));
            g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
            for (int i = 0; i < n; i++) {
                int x = padL + cw * i / Math.max(n - 1, 1);
                g2.drawString(data.get(i).date().substring(5), x - 12, h - padB + 16);
            }

            // Series
            for (int s = 0; s < 3; s++) {
                g2.setColor(COLORS[s]);
                g2.setStroke(new BasicStroke(2.5f));
                int[] px = new int[n], py = new int[n];
                for (int i = 0; i < n; i++) {
                    px[i] = padL + cw * i / Math.max(n - 1, 1);
                    py[i] = padT + (int) ((max - vals[s][i]) / range * ch);
                }
                for (int i = 1; i < n; i++) {
                    g2.drawLine(px[i - 1], py[i - 1], px[i], py[i]);
                }
                // Dots
                g2.setStroke(new BasicStroke(1));
                for (int i = 0; i < n; i++) {
                    g2.fillOval(px[i] - 3, py[i] - 3, 6, 6);
                }
            }

            g2.dispose();
        }
    }
}
