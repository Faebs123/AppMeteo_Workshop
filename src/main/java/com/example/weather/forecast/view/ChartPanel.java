package com.example.weather.forecast.view;

import com.example.weather.shared.model.DailyData;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.List;

public class ChartPanel extends JPanel {
    private final List<DailyData> data;
    private int hoverIndex = -1;
    private boolean[] visible = {true, true, true};

    private static final Color[] COLORS = {
        new Color(255, 107, 107),
        new Color(78, 205, 196),
        new Color(255, 230, 109)
    };
    private static final String[] NAMES = {"T Max", "T Min", "Vento"};
    private static final String[] UNITS = {"\u00b0C", "\u00b0C", "km/h"};

    public ChartPanel(List<DailyData> data) {
        this.data = data;
        setOpaque(false);
        setPreferredSize(new Dimension(480, 340));

        addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseMoved(MouseEvent e) {
                int padL = 50, padR = 24, cw = getWidth() - padL - padR;
                int n = data.size();
                if (n < 2 || cw < 10) return;
                int x = e.getX();
                int i = (int) Math.round((double)(x - padL) / cw * (n - 1));
                i = Math.max(0, Math.min(n - 1, i));
                if (i != hoverIndex) { hoverIndex = i; repaint(); }
            }
        });
        addMouseListener(new MouseAdapter() {
            public void mouseExited(MouseEvent e) { hoverIndex = -1; repaint(); }
        });
    }

    public void setSeriesVisible(int index, boolean v) {
        if (index >= 0 && index < visible.length) {
            visible[index] = v;
            repaint();
        }
    }

    public boolean isSeriesVisible(int index) {
        return index >= 0 && index < visible.length && visible[index];
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
            if (visible[0] || visible[1]) {
                min = Math.min(min, Math.min(d.tempMin(), d.tempMax()));
                max = Math.max(max, Math.max(d.tempMax(), d.tempMin()));
            }
            if (visible[2]) {
                min = Math.min(min, d.windMax());
                max = Math.max(max, d.windMax());
            }
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

        g2.setColor(new Color(255, 255, 255, 20));
        int ticks = 4;
        for (int t = 0; t <= ticks; t++) {
            int y = padT + ch * t / ticks;
            g2.drawLine(padL, y, padL + cw, y);
            String lbl = String.format("%.0f", max - range * t / ticks);
            g2.setColor(new Color(255, 255, 255, 130));
            g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
            g2.drawString(lbl, 2, y + 4);
            g2.setColor(new Color(255, 255, 255, 20));
        }

        g2.setColor(new Color(255, 255, 255, 130));
        g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
        for (int i = 0; i < n; i++) {
            int x = padL + cw * i / Math.max(n - 1, 1);
            g2.drawString(data.get(i).date().substring(5), x - 12, h - padB + 16);
        }

        for (int s = 0; s < 3; s++) {
            if (!visible[s]) continue;
            float alpha = (hoverIndex >= 0 && hoverIndex < n) ? 0.25f : 1f;
            g2.setColor(new Color(COLORS[s].getRed(), COLORS[s].getGreen(), COLORS[s].getBlue(), (int)(255 * alpha)));
            g2.setStroke(new BasicStroke(2.5f));
            int[] px = new int[n], py = new int[n];
            for (int i = 0; i < n; i++) {
                px[i] = padL + cw * i / Math.max(n - 1, 1);
                py[i] = padT + (int) ((max - vals[s][i]) / range * ch);
            }
            for (int i = 1; i < n; i++)
                g2.drawLine(px[i - 1], py[i - 1], px[i], py[i]);

            float dotAlpha = alpha;
            g2.setColor(COLORS[s]);
            g2.setStroke(new BasicStroke(1));
            for (int i = 0; i < n; i++) {
                float da = (i == hoverIndex) ? 1f : dotAlpha;
                g2.setColor(new Color(COLORS[s].getRed(), COLORS[s].getGreen(), COLORS[s].getBlue(), (int)(255 * da)));
                g2.fillOval(px[i] - 3, py[i] - 3, 6, 6);
            }
        }

        if (hoverIndex >= 0 && hoverIndex < n) {
            DailyData d = data.get(hoverIndex);
            int x = padL + cw * hoverIndex / Math.max(n - 1, 1);
            int y = padT + (int) ((max - vals[0][hoverIndex]) / range * ch);

            g2.setColor(new Color(255, 255, 255, 40));
            g2.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{4}, 0));
            g2.drawLine(x, padT, x, padT + ch);

            StringBuilder tip = new StringBuilder("<html><b>" + d.date() + "</b><br>");
            if (visible[0]) tip.append(NAMES[0]).append(": ").append(String.format("%.1f", vals[0][hoverIndex])).append(UNITS[0]).append("<br>");
            if (visible[1]) tip.append(NAMES[1]).append(": ").append(String.format("%.1f", vals[1][hoverIndex])).append(UNITS[1]).append("<br>");
            if (visible[2]) tip.append(NAMES[2]).append(": ").append(String.format("%.0f", vals[2][hoverIndex])).append(UNITS[2]);
            tip.append("</html>");

            int tx = Math.min(x + 12, w - 160);
            int ty = Math.max(padT, y - 50);
            g2.setColor(new Color(0, 0, 0, 160));
            g2.fillRoundRect(tx, ty, 150, visible[0] && visible[1] && visible[2] ? 78 : 60, 10, 10);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
            String[] lines = tip.toString().replace("<html>", "").replace("</html>", "").split("<br>");
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                if (line.startsWith("<b>")) line = line.replace("<b>", "").replace("</b>", "");
                g2.drawString(line, tx + 12, ty + 18 + i * 16);
            }
        }

        g2.dispose();
    }
}
