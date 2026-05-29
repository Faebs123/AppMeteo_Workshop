package com.example.weather.shared.widget;

import javax.swing.*;
import java.awt.*;

public class GradientPanel extends JPanel {
    private final Color top;
    private final Color bottom;

    public GradientPanel(Theme theme) {
        this.top = theme.gradientTop();
        this.bottom = theme.gradientBottom();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        int w = getWidth(), h = getHeight();
        GradientPaint gp = new GradientPaint(0, 0, top,
            0, h, bottom);
        g2.setPaint(gp);
        g2.fillRect(0, 0, w, h);
    }
}
