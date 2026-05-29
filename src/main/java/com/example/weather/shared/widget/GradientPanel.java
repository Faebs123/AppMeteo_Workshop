package com.example.weather.shared.widget;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

public class GradientPanel extends JPanel {
    private Color currentTop;
    private Color currentBottom;
    private Timer transitionTimer;

    public GradientPanel(Theme theme) {
        currentTop = theme.gradientTop();
        currentBottom = theme.gradientBottom();
    }

    public void applyTheme(Theme theme) {
        Objects.requireNonNull(theme);
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
            currentTop = ColorUtils.lerp(startTop, targetTop, ease);
            currentBottom = ColorUtils.lerp(startBottom, targetBottom, ease);
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
}
