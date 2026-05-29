package com.example.weather.shared.service;

import com.example.weather.shared.core.ColorUtils;
import com.example.weather.shared.model.Theme;

import java.awt.*;

public class ThemeEngine {

    private record KeyFrame(int hour, Color gradientTop, Color gradientBottom,
                            Color textPrimary, Color textSecondary, Color textMuted,
                            Color cardBackground, Color separator, Color errorText) {}

    private static final KeyFrame[] KEYFRAMES = {

        new KeyFrame(0,
            color(0x0f2027), color(0x2c5364),
            Color.WHITE, alpha(255,255,255,218), alpha(255,255,255,179),
            alpha(255,255,255,55), alpha(255,255,255,50), color(0xFFC8C8)),

        new KeyFrame(4,
            color(0x0f2027), color(0x2c5364),
            Color.WHITE, alpha(255,255,255,218), alpha(255,255,255,179),
            alpha(255,255,255,55), alpha(255,255,255,50), color(0xFFC8C8)),

        new KeyFrame(5,
            color(0x1a1a4e), color(0xFF6B35),
            alpha(255,255,255,200), alpha(255,255,255,150), alpha(255,255,255,100),
            alpha(255,255,255,70), alpha(255,255,255,40), color(0xFFB4B4)),

        new KeyFrame(6,
            color(0xFF6B35), color(0xF7C948),
            color(0x2D3436), color(0x636E72), alpha(99,110,114,179),
            alpha(255,255,255,180), alpha(99,110,114,40), color(0xDC5050)),

        new KeyFrame(7,
            color(0xFFA726), color(0xFFCC80),
            color(0x2D3436), color(0x636E72), alpha(99,110,114,179),
            alpha(255,255,255,180), alpha(99,110,114,40), color(0xDC5050)),

        new KeyFrame(10,
            color(0x4FC3F7), color(0xE0F7FA),
            color(0x2D3436), color(0x636E72), alpha(99,110,114,179),
            alpha(255,255,255,180), alpha(99,110,114,40), color(0xDC5050)),

        new KeyFrame(14,
            color(0x81D4FA), color(0xFFF9C4),
            color(0x2D3436), color(0x636E72), alpha(99,110,114,179),
            alpha(255,255,255,180), alpha(99,110,114,40), color(0xDC5050)),

        new KeyFrame(16,
            color(0xB39DDB), color(0xFFE082),
            color(0x2D3436), color(0x636E72), alpha(99,110,114,179),
            alpha(255,255,255,180), alpha(99,110,114,40), color(0xDC5050)),

        new KeyFrame(17,
            color(0xFF8A65), color(0xFFAB91),
            color(0x2D3436), color(0x636E72), alpha(99,110,114,179),
            alpha(255,255,255,180), alpha(99,110,114,40), color(0xDC5050)),

        new KeyFrame(18,
            color(0xFF6B35), color(0xFF4433),
            Color.WHITE, alpha(255,255,255,218), alpha(255,255,255,179),
            alpha(255,255,255,55), alpha(255,255,255,50), color(0xFFC8C8)),

        new KeyFrame(20,
            color(0xD84315), color(0x880E4F),
            Color.WHITE, alpha(255,255,255,210), alpha(255,255,255,170),
            alpha(255,255,255,50), alpha(255,255,255,40), color(0xFFC8C8)),

        new KeyFrame(22,
            color(0x0f2027), color(0x2c5364),
            Color.WHITE, alpha(255,255,255,218), alpha(255,255,255,179),
            alpha(255,255,255,55), alpha(255,255,255,50), color(0xFFC8C8)),

        new KeyFrame(24,
            color(0x0f2027), color(0x2c5364),
            Color.WHITE, alpha(255,255,255,218), alpha(255,255,255,179),
            alpha(255,255,255,55), alpha(255,255,255,50), color(0xFFC8C8)),
    };

    public static Theme forHour(int hour) {
        hour = Math.max(0, Math.min(23, hour));
        for (int i = 0; i < KEYFRAMES.length - 1; i++) {
            if (hour >= KEYFRAMES[i].hour() && hour < KEYFRAMES[i + 1].hour()) {
                double t = KEYFRAMES[i].hour() == KEYFRAMES[i + 1].hour() ? 0
                    : (double) (hour - KEYFRAMES[i].hour()) / (KEYFRAMES[i + 1].hour() - KEYFRAMES[i].hour());
                return interpolate(KEYFRAMES[i], KEYFRAMES[i + 1], t);
            }
        }
        return new Theme(
            KEYFRAMES[KEYFRAMES.length - 1].gradientTop(),
            KEYFRAMES[KEYFRAMES.length - 1].gradientBottom(),
            KEYFRAMES[KEYFRAMES.length - 1].textPrimary(),
            KEYFRAMES[KEYFRAMES.length - 1].textSecondary(),
            KEYFRAMES[KEYFRAMES.length - 1].textMuted(),
            KEYFRAMES[KEYFRAMES.length - 1].cardBackground(),
            KEYFRAMES[KEYFRAMES.length - 1].separator(),
            KEYFRAMES[KEYFRAMES.length - 1].errorText());
    }

    private static Theme interpolate(KeyFrame a, KeyFrame b, double t) {
        return new Theme(
            ColorUtils.lerp(a.gradientTop(), b.gradientTop(), t),
            ColorUtils.lerp(a.gradientBottom(), b.gradientBottom(), t),
            ColorUtils.lerp(a.textPrimary(), b.textPrimary(), t),
            ColorUtils.lerp(a.textSecondary(), b.textSecondary(), t),
            ColorUtils.lerp(a.textMuted(), b.textMuted(), t),
            ColorUtils.lerp(a.cardBackground(), b.cardBackground(), t),
            ColorUtils.lerp(a.separator(), b.separator(), t),
            ColorUtils.lerp(a.errorText(), b.errorText(), t));
    }

    private static Color color(int hex) {
        return new Color(hex);
    }

    private static Color alpha(int r, int g, int b, int a) {
        return new Color(r, g, b, a);
    }
}
