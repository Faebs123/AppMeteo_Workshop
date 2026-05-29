package com.example.weather.search.view;

import com.example.weather.shared.ui.Labels;
import com.example.weather.shared.ui.StyledButton;

import javax.swing.*;
import java.awt.*;

public class HomePanel extends JPanel {
    public HomePanel(String cityLabel, Runnable onCurrent, Runnable onReport, Labels labels) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);

        JLabel label = labels.heading(cityLabel);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 18, 0));
        btnRow.setOpaque(false);

        JButton currentBtn = new StyledButton("Condizioni attuali", new Color(5, 150, 105));
        currentBtn.addActionListener(e -> onCurrent.run());
        JButton reportBtn = new StyledButton("Report giornaliero", new Color(217, 119, 6));
        reportBtn.addActionListener(e -> onReport.run());

        btnRow.add(currentBtn);
        btnRow.add(reportBtn);

        add(label);
        add(Box.createVerticalStrut(6));
        add(btnRow);
    }
}
