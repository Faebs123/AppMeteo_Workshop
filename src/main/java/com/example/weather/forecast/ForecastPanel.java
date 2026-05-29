package com.example.weather.forecast;

import com.example.weather.shared.model.DailyData;
import com.example.weather.shared.widget.Labels;
import com.example.weather.shared.widget.StyledButton;
import com.example.weather.shared.widget.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;
import java.util.function.Consumer;

public class ForecastPanel extends JPanel {
    private final JSpinner daySpinner;
    private final JPanel chartContainer;
    private final Labels labels;
    private final Theme theme;

    public ForecastPanel(Consumer<Integer> onGenerate, Labels labels, Theme theme) {
        this.labels = labels;
        this.theme = theme;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        controls.setOpaque(false);
        JLabel dayLabel = labels.small("Giorni:");
        daySpinner = new JSpinner(new SpinnerNumberModel(7, 1, 16, 1));
        daySpinner.setPreferredSize(new Dimension(70, 30));

        JButton generateBtn = new StyledButton("Genera", new Color(59, 130, 246));
        generateBtn.addActionListener(e -> onGenerate.accept((Integer) daySpinner.getValue()));

        controls.add(dayLabel);
        controls.add(daySpinner);
        controls.add(generateBtn);

        chartContainer = new JPanel(new BorderLayout());
        chartContainer.setOpaque(false);

        add(controls);
        add(chartContainer);
    }

    public void setChart(List<DailyData> data) {
        chartContainer.removeAll();

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
            JLabel lbl = labels.small(ni);
            item.add(dot);
            item.add(lbl);
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

    private JPanel avgBox(String label, String value, Color color) {
        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setOpaque(false);
        box.setBackground(new Color(255, 255, 255, 30));
        box.setBorder(new EmptyBorder(10, 14, 10, 14));
        JLabel val = labels.value(value, color);
        JLabel lbl = labels.small(label);
        box.add(val);
        box.add(lbl);
        return box;
    }
}
