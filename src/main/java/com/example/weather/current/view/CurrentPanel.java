package com.example.weather.current.view;

import com.example.weather.shared.model.WeatherData;
import com.example.weather.shared.ui.Labels;
import com.example.weather.shared.ui.RoundedPanel;
import com.example.weather.shared.model.Theme;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class CurrentPanel extends JPanel {
    public CurrentPanel(WeatherData data, String description, Labels labels, Theme theme) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);
        setAlignmentX(Component.CENTER_ALIGNMENT);

        RoundedPanel card = new RoundedPanel(24);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(new Color(255, 255, 255, 28));
        card.setBorder(BorderFactory.createEmptyBorder(32, 40, 32, 40));
        card.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.setMaximumSize(new Dimension(520, 520));

        JLabel iconLabel = new JLabel(iconForCode(data.code()), SwingConstants.CENTER);
        iconLabel.setFont(new Font("SansSerif", Font.PLAIN, 64));
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel tempLabel = labels.hero(String.format("%.1f\u00b0", data.temp()));
        JLabel descLabel = new JLabel(description, SwingConstants.CENTER);
        descLabel.setFont(new Font("SansSerif", Font.PLAIN, 20));
        descLabel.setForeground(new Color(theme.textPrimary().getRed(), theme.textPrimary().getGreen(), theme.textPrimary().getBlue(), 230));
        descLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel feelsLabel = labels.caption(String.format("Percepita %.1f\u00b0", data.feelsLike()));

        JSeparator sep = new JSeparator(SwingConstants.HORIZONTAL);
        sep.setMaximumSize(new Dimension(200, 1));
        sep.setForeground(new Color(255, 255, 255, 50));

        JPanel details = new JPanel(new FlowLayout(FlowLayout.CENTER, 48, 0));
        details.setOpaque(false);
        details.add(detailBox("Umidit\u00e0", data.humidity() + "%", labels));
        details.add(detailBox("Vento", String.format("%.0f km/h", data.wind()), labels));

        card.add(iconLabel);
        card.add(Box.createVerticalStrut(6));
        card.add(tempLabel);
        card.add(Box.createVerticalStrut(4));
        card.add(descLabel);
        card.add(Box.createVerticalStrut(4));
        card.add(feelsLabel);
        card.add(Box.createVerticalStrut(18));
        card.add(sep);
        card.add(Box.createVerticalStrut(18));
        card.add(details);

        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setOpaque(false);
        wrapper.add(card);

        add(wrapper);
        add(Box.createVerticalStrut(16));
    }

    private JPanel detailBox(String label, String value, Labels labels) {
        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setOpaque(false);
        JLabel val = labels.value(value);
        JLabel lbl = labels.small(label);
        box.add(val);
        box.add(lbl);
        return box;
    }

    private static String iconForCode(int code) {
        if (code == 0) return "\u2600\uFE0F";
        if (code <= 3) return "\u26C5";
        if (code <= 48) return "\uD83C\uDF2B\uFE0F";
        if (code <= 57) return "\uD83C\uDF26";
        if (code <= 67) return "\uD83C\uDF27";
        if (code <= 77) return "\u2744\uFE0F";
        if (code <= 82) return "\uD83C\uDF28";
        if (code <= 86) return "\uD83C\uDF28";
        return "\u26A1";
    }
}
