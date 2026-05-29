package com.example.weather.shared.widget;

import javax.swing.*;
import java.awt.*;

public class Labels {
    private final Theme theme;

    public Labels(Theme theme) {
        this.theme = theme;
    }

    public JLabel hero(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(new Font("SansSerif", Font.PLAIN, 58));
        label.setForeground(theme.textPrimary());
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        return label;
    }

    public JLabel title(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(new Font("SansSerif", Font.BOLD, 26));
        label.setForeground(theme.textPrimary());
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        return label;
    }

    public JLabel heading(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(new Font("SansSerif", Font.BOLD, 15));
        label.setForeground(theme.textSecondary());
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        return label;
    }

    public JLabel subheading(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 14));
        label.setForeground(theme.textSecondary());
        return label;
    }

    public JLabel body(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(new Font("SansSerif", Font.PLAIN, 16));
        label.setForeground(new Color(theme.textPrimary().getRed(), theme.textPrimary().getGreen(), theme.textPrimary().getBlue(), 230));
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        return label;
    }

    public JLabel caption(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(new Font("SansSerif", Font.PLAIN, 13));
        label.setForeground(theme.textMuted());
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        return label;
    }

    public JLabel value(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(new Font("SansSerif", Font.BOLD, 18));
        label.setForeground(theme.textPrimary());
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        return label;
    }

    public JLabel value(String text, Color color) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(new Font("SansSerif", Font.BOLD, 16));
        label.setForeground(color);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        return label;
    }

    public JLabel small(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.PLAIN, 12));
        label.setForeground(theme.textMuted());
        return label;
    }

    public JLabel loading(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(new Font("SansSerif", Font.PLAIN, 16));
        label.setForeground(new Color(theme.textPrimary().getRed(), theme.textPrimary().getGreen(), theme.textPrimary().getBlue(), 200));
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        return label;
    }

    public JLabel error(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(new Font("SansSerif", Font.PLAIN, 13));
        label.setForeground(theme.errorText());
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        return label;
    }
}
