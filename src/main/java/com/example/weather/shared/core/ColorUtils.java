package com.example.weather.shared.core;

import java.awt.*;

public class ColorUtils {
    public static Color lerp(Color a, Color b, double t) {
        return new Color(
            clamp(a.getRed() + (int) Math.round((b.getRed() - a.getRed()) * t)),
            clamp(a.getGreen() + (int) Math.round((b.getGreen() - a.getGreen()) * t)),
            clamp(a.getBlue() + (int) Math.round((b.getBlue() - a.getBlue()) * t)),
            clamp(a.getAlpha() + (int) Math.round((b.getAlpha() - a.getAlpha()) * t)));
    }

    public static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }
}
