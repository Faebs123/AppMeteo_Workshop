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
        root.setBorder(new EmptyBorder(30, 20, 30, 20));

        JLabel title = new JLabel("Meteo App", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 26));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        searchRow.setOpaque(false);

        cityField = new JTextField(18);
        cityField.putClientProperty("JTextField.placeholderText", "Roma, Milano, Tokyo...");
        cityField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        cityField.setPreferredSize(new Dimension(220, 38));

        JButton searchBtn = styledButton("CERCA");
        searchRow.add(cityField);
        searchRow.add(searchBtn);

        loadingLabel = new JLabel("Caricamento...", SwingConstants.CENTER);
        loadingLabel.setFont(new Font("SansSerif", Font.PLAIN, 16));
        loadingLabel.setForeground(new Color(255, 255, 255, 200));
        loadingLabel.setVisible(false);
        loadingLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

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
        root.add(Box.createVerticalStrut(10));
        root.add(searchRow);
        root.add(Box.createVerticalStrut(8));
        root.add(errorLabel);
        root.add(loadingLabel);
        root.add(Box.createVerticalStrut(8));
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

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0));
        btnRow.setOpaque(false);

        JButton currentBtn = styledButton("Condizioni attuali");
        currentBtn.addActionListener(e -> showCurrent());
        JButton reportBtn = styledButton("Report giornaliero");
        reportBtn.addActionListener(e -> showReport());

        btnRow.add(currentBtn);
        btnRow.add(reportBtn);

        contentPanel.add(cityLabel);
        contentPanel.add(Box.createVerticalStrut(6));
        contentPanel.add(btnRow);
        refresh();
    }

    private void showCurrent() {
        showChoiceButtons();

        RoundedPanel card = new RoundedPanel(20);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setOpaque(false);
        card.setBackground(new Color(255, 255, 255, 30));
        card.setBorder(new EmptyBorder(24, 28, 24, 28));
        card.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.setMaximumSize(new Dimension(400, 400));

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

        JPanel details = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 0));
        details.setOpaque(false);
        details.add(detailBox("Umidità", lastData.humidity() + "%"));
        details.add(detailBox("Vento", String.format("%.0f km/h", lastData.wind())));

        card.add(tempLabel);
        card.add(Box.createVerticalStrut(2));
        card.add(descLabel);
        card.add(Box.createVerticalStrut(2));
        card.add(feelsLabel);
        card.add(Box.createVerticalStrut(10));
        card.add(sep);
        card.add(Box.createVerticalStrut(10));
        card.add(details);

        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setOpaque(false);
        wrapper.add(card);

        contentPanel.add(wrapper);
        refresh();
    }

    private void showReport() {
        showChoiceButtons();

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        controls.setOpaque(false);

        JLabel dayLabel = new JLabel("Giorni:");
        dayLabel.setForeground(new Color(255, 255, 255, 204));

        JSpinner daySpinner = new JSpinner(new SpinnerNumberModel(7, 1, 16, 1));
        daySpinner.setPreferredSize(new Dimension(70, 30));

        JButton generateBtn = styledButton("Genera");
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
        avgs.add(avgBox("T Max media", String.format("%.1f°C", avgTMax), new Color(255, 107, 107)));
        avgs.add(avgBox("T Min media", String.format("%.1f°C", avgTMin), new Color(78, 205, 196)));
        avgs.add(avgBox("Vento medio", String.format("%.0f km/h", avgWind), new Color(255, 230, 109)));

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(avgs, BorderLayout.NORTH);
        wrapper.add(new ChartPanel(data), BorderLayout.CENTER);

        return wrapper;
    }

    // --- Helpers ---

    private JsonNode geocode(String geoUrl, String city) throws Exception {
        JsonNode geoData = fetchJson(geoUrl);
        JsonNode results = geoData.get("results");
        if (results == null || !results.isArray() || results.isEmpty())
            throw new RuntimeException("Località \"" + city + "\" non trovata.");
        return results.get(0);
    }

    private JsonNode fetchJson(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200)
            throw new RuntimeException("Errore API: " + resp.statusCode() + " " + resp.body());
        return mapper.readTree(resp.body());
    }

    private JButton styledButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btn.setForeground(Color.WHITE);
        btn.setBackground(new Color(59, 130, 246));
        btn.setBorder(BorderFactory.createEmptyBorder(8, 22, 8, 22));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(new Color(37, 99, 235)); }
            public void mouseExited(MouseEvent e) { btn.setBackground(new Color(59, 130, 246)); }
        });
        return btn;
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
        box.setBackground(new Color(255, 255, 255, 15));
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

    // --- Custom components ---

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
        private static final String[] NAMES = {"T Max", "T Min", "Vento"};

        ChartPanel(List<DailyData> data) {
            this.data = data;
            setOpaque(false);
            setPreferredSize(new Dimension(440, 240));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (data.isEmpty()) return;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            int padL = 50, padR = 20, padT = 20, padB = 40;
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

            // Legend
            int lx = padL + cw - 160, ly = padT + 4;
            g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
            for (int s = 0; s < 3; s++) {
                g2.setColor(new Color(255, 255, 255, 30));
                g2.fillRect(lx, ly + s * 18, 50, 16);
                g2.setColor(COLORS[s]);
                g2.fillRect(lx + 2, ly + s * 18 + 3, 10, 10);
                g2.setColor(new Color(255, 255, 255, 200));
                g2.drawString(NAMES[s], lx + 16, ly + s * 18 + 12);
            }

            g2.dispose();
        }
    }
}
