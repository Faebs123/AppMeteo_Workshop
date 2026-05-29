package com.example.weather.forecast.view;

import com.example.weather.shared.model.DailyData;
import com.example.weather.shared.ui.Labels;
import com.example.weather.shared.ui.RoundedPanel;
import com.example.weather.shared.ui.ToggleButton;
import com.example.weather.shared.model.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;
import java.util.function.Consumer;

public class ForecastPanel extends JPanel {
    private final JPanel chartContainer;
    private final Labels labels;
    private final Theme theme;
    private ChartPanel chart;
    private final Consumer<Integer> onGenerate;
    private int selectedDays = 7;

    public ForecastPanel(Consumer<Integer> onGenerate, Labels labels, Theme theme) {
        this.onGenerate = onGenerate;
        this.labels = labels;
        this.theme = theme;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        controls.setOpaque(false);

        ToggleButton oggiBtn = new ToggleButton("Oggi", new Color(100, 140, 255, 80), new Color(59, 130, 246));
        ToggleButton treBtn = new ToggleButton("3 Giorni", new Color(100, 140, 255, 80), new Color(59, 130, 246));
        ToggleButton setteBtn = new ToggleButton("7 Giorni", new Color(100, 140, 255, 80), new Color(59, 130, 246));
        setteBtn.setSelected(true);

        ButtonGroup group = new ButtonGroup();
        group.add(oggiBtn);
        group.add(treBtn);
        group.add(setteBtn);

        oggiBtn.addActionListener(e -> { selectedDays = 1; onGenerate.accept(1); });
        treBtn.addActionListener(e -> { selectedDays = 3; onGenerate.accept(3); });
        setteBtn.addActionListener(e -> { selectedDays = 7; onGenerate.accept(7); });

        controls.add(oggiBtn);
        controls.add(treBtn);
        controls.add(setteBtn);

        chartContainer = new JPanel(new BorderLayout());
        chartContainer.setOpaque(false);

        add(controls);
        add(Box.createVerticalStrut(8));
        add(chartContainer);
    }

    public void setChart(List<DailyData> data) {
        chartContainer.removeAll();

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
