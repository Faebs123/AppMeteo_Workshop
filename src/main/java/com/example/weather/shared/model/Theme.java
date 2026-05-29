package com.example.weather.shared.model;

import java.awt.*;

public record Theme(Color gradientTop, Color gradientBottom, Color textPrimary, Color textSecondary, Color textMuted, Color cardBackground, Color separator, Color errorText) {
    public static final Theme NIGHT = new Theme(
        new Color(0x0f2027),
        new Color(0x2c5364),
        Color.WHITE,
        new Color(255, 255, 255, 218),
        new Color(255, 255, 255, 179),
        new Color(255, 255, 255, 55),
        new Color(255, 255, 255, 50),
        new Color(255, 200, 200));

    public static final Theme DAY = new Theme(
        new Color(0x87CEEB),
        new Color(0xE0F7FA),
        new Color(0x2D3436),
        new Color(0x636E72),
        new Color(99, 110, 114, 179),
        new Color(255, 255, 255, 180),
        new Color(99, 110, 114, 40),
        new Color(220, 80, 80));
}
