package com.example.weather.shared.widget;

import javax.swing.*;
import java.awt.*;

public class GradientPanel extends JPanel {
    private Color currentTop;
    private Color currentBottom;
    private Timer transitionTimer;

    public GradientPanel(Theme theme) {
        currentTop = theme.gradientTop();
        currentBottom = theme.gradientBottom();
    }

    public void applyTheme(Theme theme) {
        Color targetTop = theme.gradientTop();
        Color targetBottom = theme.gradientBottom();

        if (currentTop.equals(targetTop) && currentBottom.equals(targetBottom)) return;

        if (transitionTimer != null && transitionTimer.isRunning())
            transitionTimer.stop();

        Color startTop = currentTop;
        Color startBottom = currentBottom;
        long startTime = System.currentTimeMillis();
        long duration = 1200;

        transitionTimer = new Timer(16, e -> {
            long elapsed = System.currentTimeMillis() - startTime;
            double t = Math.min(1.0, (double) elapsed / duration);
            double ease = t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2;
            currentTop = lerp(startTop, targetTop, ease);
            currentBottom = lerp(startBottom, targetBottom, ease);
            repaint();
            if (t >= 1.0) ((Timer) e.getSource()).stop();
        });
        transitionTimer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        int w = getWidth(), h = getHeight();
        g2.setPaint(new GradientPaint(0, 0, currentTop, 0, h, currentBottom));
        g2.fillRect(0, 0, w, h);
    }

    private static Color lerp(Color a, Color b, double t) {
        return new Color(
            clamp(a.getRed() + (int) Math.round((b.getRed() - a.getRed()) * t)),
            clamp(a.getGreen() + (int) Math.round((b.getGreen() - a.getGreen()) * t)),
            clamp(a.getBlue() + (int) Math.round((b.getBlue() - a.getBlue()) * t)),
            clamp(a.getAlpha() + (int) Math.round((b.getAlpha() - a.getAlpha()) * t)));
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }
}
