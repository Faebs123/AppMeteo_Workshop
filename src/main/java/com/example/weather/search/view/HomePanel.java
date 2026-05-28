package com.example.weather.search.view;

import com.example.weather.shared.widget.StyledButton;

import javax.swing.*;
import java.awt.*;

public class HomePanel extends JPanel {
    public HomePanel(String cityLabel, Runnable onCurrent, Runnable onReport) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);

        JLabel label = new JLabel(cityLabel, SwingConstants.CENTER);
        label.setFont(new Font("SansSerif", Font.BOLD, 15));
        label.setForeground(new Color(255, 255, 255, 218));
        label.setAlignmentX(Component.CENTER_ALIGNMENT);

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
