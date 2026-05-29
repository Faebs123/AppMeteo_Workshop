package com.example.weather.shared.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class ToggleButton extends JToggleButton {
    private final Color baseColor;
    private final Color selectedColor;
    private boolean hovered;

    public ToggleButton(String text, Color base, Color selected) {
        super(text);
        this.baseColor = base;
        this.selectedColor = selected;
        setFont(new Font("SansSerif", Font.BOLD, 13));
        setForeground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        setFocusPainted(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
            public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight(), r = 20;
        if (isSelected()) {
            g2.setColor(selectedColor);
            g2.fillRoundRect(0, 0, w, h, r, r);
        } else if (hovered) {
            g2.setColor(baseColor);
            g2.fillRoundRect(0, 0, w, h, r, r);
        } else {
            g2.setColor(new Color(255, 255, 255, 40));
            g2.fillRoundRect(0, 0, w, h, r, r);
        }
        super.paintComponent(g2);
        g2.dispose();
    }
}
