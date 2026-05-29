package com.example.weather.shared.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.HierarchyEvent;

public class SkeletonPanel extends JPanel {
    private float phase;

    public SkeletonPanel(int w, int h, int count) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);
        setAlignmentX(Component.CENTER_ALIGNMENT);
        setMaximumSize(new Dimension(w, h));

        for (int i = 0; i < count; i++) {
            JPanel bar = new JPanel() {
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    int bw = (int)(getWidth() * (0.5 + 0.4 * Math.random()));
                    int bh = getHeight();
                    int bx = (getWidth() - bw) / 2;
                    g2.setColor(new Color(255, 255, 255, 20));
                    g2.fillRoundRect(bx, 0, bw, bh, 8, 8);
                    g2.dispose();
                }
            };
            bar.setOpaque(false);
            bar.setMaximumSize(new Dimension(w, 14));
            bar.setPreferredSize(new Dimension(w, 14));
            add(bar);
            add(Box.createVerticalStrut(8));
        }

        Timer shimmer = new Timer(40, e -> {
            phase = (phase + 0.04f) % 1;
            repaint();
        });
        shimmer.start();

        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                if (isShowing()) shimmer.start();
                else shimmer.stop();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        int w = getWidth(), h = getHeight();
        g2.setColor(new Color(255, 255, 255, 8));
        g2.fillRect(0, 0, w, h);
        int x = (int)(phase * (w + 120)) - 60;
        g2.setColor(new Color(255, 255, 255, 30));
        g2.fillRoundRect(x, 0, 80, h, 40, 40);
        g2.dispose();
    }
}
