package com.example.weather;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class ChartPanel extends JPanel {
    private final List<DailyData> data;
    private static final Color[] COLORS = {
        new Color(255, 107, 107),
        new Color(78, 205, 196),
        new Color(255, 230, 109)
    };

    public ChartPanel(List<DailyData> data) {
        this.data = data;
        setOpaque(false);
        setPreferredSize(new Dimension(480, 300));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (data.isEmpty()) return;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        int padL = 50, padR = 24, padT = 24, padB = 48;
        int w = getWidth(), h = getHeight();
        int cw = w - padL - padR, ch = h - padT - padB;

        if (cw < 10 || ch < 10) { g2.dispose(); return; }

        double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
        for (DailyData d : data) {
            min = Math.min(min, Math.min(d.tempMin(), Math.min(d.tempMax(), d.windMax())));
            max = Math.max(max, Math.max(d.tempMax(), Math.max(d.tempMin(), d.windMax())));
        }
        double range = max - min;
        if (range < 1) range = 1;

        int n = data.size();
        double[][] vals = new double[3][n];
        for (int i = 0; i < n; i++) {
            vals[0][i] = data.get(i).tempMax();
            vals[1][i] = data.get(i).tempMin();
            vals[2][i] = data.get(i).windMax();
        }

        g2.setColor(new Color(255, 255, 255, 30));
        int ticks = 4;
        for (int t = 0; t <= ticks; t++) {
            int y = padT + ch * t / ticks;
            g2.drawLine(padL, y, padL + cw, y);
            String lbl = String.format("%.0f", max - range * t / ticks);
            g2.setColor(new Color(255, 255, 255, 150));
            g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
            g2.drawString(lbl, 2, y + 4);
            g2.setColor(new Color(255, 255, 255, 30));
        }

        g2.setColor(new Color(255, 255, 255, 150));
        g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
        for (int i = 0; i < n; i++) {
            int x = padL + cw * i / Math.max(n - 1, 1);
            g2.drawString(data.get(i).date().substring(5), x - 12, h - padB + 16);
        }

        for (int s = 0; s < 3; s++) {
            g2.setColor(COLORS[s]);
            g2.setStroke(new BasicStroke(2.5f));
            int[] px = new int[n], py = new int[n];
            for (int i = 0; i < n; i++) {
                px[i] = padL + cw * i / Math.max(n - 1, 1);
                py[i] = padT + (int) ((max - vals[s][i]) / range * ch);
            }
            for (int i = 1; i < n; i++) {
                g2.drawLine(px[i - 1], py[i - 1], px[i], py[i]);
            }
            g2.setStroke(new BasicStroke(1));
            for (int i = 0; i < n; i++) {
                g2.fillOval(px[i] - 3, py[i] - 3, 6, 6);
            }
        }

        g2.dispose();
    }
}
