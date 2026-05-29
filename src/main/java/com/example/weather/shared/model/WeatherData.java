package com.example.weather.shared.model;

public record WeatherData(String city, String country, double temp, double feelsLike, int humidity, double wind, int code, boolean day) {}
