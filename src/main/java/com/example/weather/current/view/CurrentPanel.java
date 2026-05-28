package com.example.weather.current.view;

import com.example.weather.shared.model.WeatherData;
import com.example.weather.shared.widget.Labels;
import com.example.weather.shared.widget.RoundedPanel;

import javax.swing.*;
import java.awt.*;

public class CurrentPanel extends JPanel {
    public CurrentPanel(WeatherData data, String description) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);

        RoundedPanel card = new RoundedPanel(20);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(new Color(255, 255, 255, 55));
        card.setBorder(BorderFactory.createEmptyBorder(28, 32, 28, 32));
        card.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.setMaximumSize(new Dimension(420, 420));

        JLabel tempLabel = Labels.hero(String.format("%.1f\u00b0", data.temp()));

        JLabel descLabel = Labels.body(description);

        JLabel feelsLabel = Labels.caption(String.format("Percepita %.1f\u00b0", data.feelsLike()));

        JSeparator sep = new JSeparator(SwingConstants.HORIZONTAL);
        sep.setMaximumSize(new Dimension(160, 1));
        sep.setForeground(new Color(255, 255, 255, 50));

        JPanel details = new JPanel(new FlowLayout(FlowLayout.CENTER, 36, 0));
        details.setOpaque(false);
        details.add(detailBox("Umidit\u00e0", data.humidity() + "%"));
        details.add(detailBox("Vento", String.format("%.0f km/h", data.wind())));

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

        add(wrapper);
    }

    private JPanel detailBox(String label, String value) {
        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setOpaque(false);
        box.add(Labels.value(value));
        box.add(Labels.caption(label));
        return box;
    }
}
