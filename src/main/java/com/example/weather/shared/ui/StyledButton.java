package com.example.weather.shared.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class StyledButton extends JButton {
    private final Color baseColor;
    private final Color hoverColor;
    private final Color pressColor;
    private boolean hovered, pressed;

    public StyledButton(String text, Color base) {
        super(text);
        this.baseColor = base;
        this.hoverColor = base.darker();
        this.pressColor = new Color(
            Math.max(0, base.getRed() - 60),
            Math.max(0, base.getGreen() - 60),
            Math.max(0, base.getBlue() - 60));
        setFont(new Font("SansSerif", Font.BOLD, 14));
        setForeground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(11, 28, 11, 28));
        setFocusPainted(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) { pressed = true; repaint(); }
            public void mouseReleased(MouseEvent e) { pressed = false; repaint(); }
            public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
            public void mouseExited(MouseEvent e) { hovered = false; pressed = false; repaint(); }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight(), r = 24;
        if (pressed) {
            g2.setColor(pressColor);
            g2.fillRoundRect(1, 1, w - 2, h - 2, r, r);
        } else if (hovered) {
            g2.setColor(hoverColor);
            g2.fillRoundRect(0, 0, w, h, r, r);
            g2.setColor(new Color(255, 255, 255, 30));
            g2.fillRoundRect(0, 0, w, h / 2, r, r);
        } else {
            g2.setColor(baseColor);
            g2.fillRoundRect(0, 0, w, h, r, r);
        }
        super.paintComponent(g2);
        g2.dispose();
    }
}
