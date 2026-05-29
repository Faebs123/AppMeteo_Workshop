package com.example.weather.forecast.view;

import com.example.weather.shared.model.DailyData;
import com.example.weather.shared.ui.Labels;
import com.example.weather.shared.ui.RoundedPanel;
import com.example.weather.shared.ui.StyledButton;
import com.example.weather.shared.model.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class ForecastPanel extends JPanel {
    private final JPanel chartContainer;
    private final Labels labels;
    private final Theme theme;
    private ChartPanel chart;
    private final Consumer<String[]> onGenerate;
    private final JSpinner startSpinner;
    private final JSpinner endSpinner;

    public ForecastPanel(Consumer<String[]> onGenerate, Labels labels, Theme theme) {
        this.onGenerate = onGenerate;
        this.labels = labels;
        this.theme = theme;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        controls.setOpaque(false);

        Date today = new Date();
        Date weekLater = new Date(today.getTime() + TimeUnit.DAYS.toMillis(6));

        SpinnerDateModel startModel = new SpinnerDateModel(today, null, null, java.util.Calendar.DAY_OF_MONTH);
        SpinnerDateModel endModel = new SpinnerDateModel(weekLater, null, null, java.util.Calendar.DAY_OF_MONTH);

        startSpinner = new JSpinner(startModel);
        endSpinner = new JSpinner(endModel);

        SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yyyy");
        JSpinner.DateEditor startEditor = new JSpinner.DateEditor(startSpinner, "dd/MM/yyyy");
        JSpinner.DateEditor endEditor = new JSpinner.DateEditor(endSpinner, "dd/MM/yyyy");
        startSpinner.setEditor(startEditor);
        endSpinner.setEditor(endEditor);

        startSpinner.setPreferredSize(new Dimension(110, 32));
        endSpinner.setPreferredSize(new Dimension(110, 32));
        startSpinner.setFont(new Font("SansSerif", Font.PLAIN, 13));
        endSpinner.setFont(new Font("SansSerif", Font.PLAIN, 13));

        JButton generateBtn = new StyledButton("Analizza", new Color(59, 130, 246));
        generateBtn.addActionListener(e -> generate());

        JLabel daLabel = labels.small("Da:");
        JLabel aLabel = labels.small("A:");

        controls.add(daLabel);
        controls.add(startSpinner);
        controls.add(Box.createHorizontalStrut(4));
        controls.add(aLabel);
        controls.add(endSpinner);
        controls.add(Box.createHorizontalStrut(8));
        controls.add(generateBtn);

        chartContainer = new JPanel(new BorderLayout());
        chartContainer.setOpaque(false);

        add(controls);
        add(Box.createVerticalStrut(8));
        add(chartContainer);
    }

    private void generate() {
        Date start = (Date) startSpinner.getValue();
        Date end = (Date) endSpinner.getValue();
        if (end.before(start)) {
            Date tmp = start;
            start = end;
            end = tmp;
            startSpinner.setValue(start);
            endSpinner.setValue(end);
        }
        SimpleDateFormat isoFmt = new SimpleDateFormat("yyyy-MM-dd");
        String startIso = isoFmt.format(start);
        String endIso = isoFmt.format(end);
        onGenerate.accept(new String[]{startIso, endIso});
    }

    public void setChart(List<DailyData> data) {
        chartContainer.removeAll();

        if (data.size() == 1) {
            DailyData d = data.get(0);
            String formatted = d.date();

            JPanel single = new JPanel();
            single.setLayout(new BoxLayout(single, BoxLayout.Y_AXIS));
            single.setOpaque(false);
            single.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel dateLabel = new JLabel(formatted, SwingConstants.CENTER);
            dateLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
            dateLabel.setForeground(theme.textSecondary());
            dateLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            RoundedPanel card = new RoundedPanel(20);
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            card.setBackground(new Color(255, 255, 255, 25));
            card.setBorder(BorderFactory.createEmptyBorder(28, 36, 28, 36));
            card.setAlignmentX(Component.CENTER_ALIGNMENT);
            card.setMaximumSize(new Dimension(380, 260));

            JLabel tMax = new JLabel(String.format("T Max: %.1f\u00b0C", d.tempMax()), SwingConstants.CENTER);
            tMax.setFont(new Font("SansSerif", Font.BOLD, 28));
            tMax.setForeground(new Color(255, 107, 107));
            tMax.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel tMin = new JLabel(String.format("T Min: %.1f\u00b0C", d.tempMin()), SwingConstants.CENTER);
            tMin.setFont(new Font("SansSerif", Font.BOLD, 28));
            tMin.setForeground(new Color(78, 205, 196));
            tMin.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel wind = new JLabel(String.format("Vento: %.0f km/h", d.windMax()), SwingConstants.CENTER);
            wind.setFont(new Font("SansSerif", Font.BOLD, 20));
            wind.setForeground(new Color(255, 230, 109));
            wind.setAlignmentX(Component.CENTER_ALIGNMENT);

            card.add(tMax);
            card.add(Box.createVerticalStrut(8));
            card.add(tMin);
            card.add(Box.createVerticalStrut(8));
            card.add(wind);

            JPanel wrapper = new JPanel(new GridBagLayout());
            wrapper.setOpaque(false);
            wrapper.add(card);

            single.add(dateLabel);
            single.add(Box.createVerticalStrut(14));
            single.add(wrapper);

            chartContainer.add(single, BorderLayout.CENTER);
            chartContainer.revalidate();
            chartContainer.repaint();
            return;
        }

        double avgTMax = data.stream().mapToDouble(DailyData::tempMax).average().orElse(0);
        double avgTMin = data.stream().mapToDouble(DailyData::tempMin).average().orElse(0);
        double avgWind = data.stream().mapToDouble(DailyData::windMax).average().orElse(0);

        JPanel avgs = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        avgs.setOpaque(false);
        avgs.setBorder(new EmptyBorder(10, 0, 8, 0));
        avgs.add(avgCard("T Max media", String.format("%.1f\u00b0C", avgTMax), new Color(255, 107, 107)));
        avgs.add(avgCard("T Min media", String.format("%.1f\u00b0C", avgTMin), new Color(78, 205, 196)));
        avgs.add(avgCard("Vento medio", String.format("%.0f km/h", avgWind), new Color(255, 230, 109)));

        chart = new ChartPanel(data);

        JPanel legend = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        legend.setOpaque(false);
        legend.setBorder(new EmptyBorder(6, 0, 0, 0));
        Color[] cols = {new Color(255, 107, 107), new Color(78, 205, 196), new Color(255, 230, 109)};
        String[] names = {"T Max", "T Min", "Vento"};
        for (int i = 0; i < 3; i++) {
            final int idx = i;
            JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
            item.setOpaque(false);
            item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            JPanel dot = new JPanel() {
                public Dimension getPreferredSize() { return new Dimension(12, 12); }
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    if (chart.isSeriesVisible(idx)) {
                        g2.setColor(cols[idx]);
                        g2.fillOval(0, 0, 12, 12);
                    } else {
                        g2.setColor(new Color(255, 255, 255, 40));
                        g2.drawOval(0, 0, 12, 12);
                    }
                }
            };
            dot.setOpaque(false);
            JLabel lbl = labels.small(names[i]);
            if (!chart.isSeriesVisible(idx)) lbl.setForeground(new Color(255, 255, 255, 60));
            item.add(dot);
            item.add(lbl);
            item.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    boolean now = !chart.isSeriesVisible(idx);
                    chart.setSeriesVisible(idx, now);
                    lbl.setForeground(now ? theme.textMuted() : new Color(255, 255, 255, 60));
                    dot.repaint();
                }
            });
            legend.add(item);
        }

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(avgs, BorderLayout.NORTH);
        wrapper.add(chart, BorderLayout.CENTER);
        wrapper.add(legend, BorderLayout.SOUTH);

        chartContainer.add(wrapper, BorderLayout.CENTER);
        chartContainer.revalidate();
        chartContainer.repaint();
    }

    private JPanel avgCard(String label, String value, Color color) {
        RoundedPanel card = new RoundedPanel(14);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(new Color(255, 255, 255, 25));
        card.setBorder(new EmptyBorder(10, 16, 10, 16));
        card.setAlignmentY(Component.CENTER_ALIGNMENT);
        JLabel val = labels.value(value, color);
        JLabel lbl = labels.small(label);
        card.add(val);
        card.add(lbl);
        return card;
    }
}
