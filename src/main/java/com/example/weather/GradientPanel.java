package com.example.weather;

import javax.swing.*;
import java.awt.*;

public class GradientPanel extends JPanel {
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
