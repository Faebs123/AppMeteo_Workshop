package com.example.weather.shared.model;

public record GeocodeResult(String cityName, String country, double lat, double lon, String timezone) {}
