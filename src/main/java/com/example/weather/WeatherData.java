package com.example.weather;

record WeatherData(String city, String country, double temp, double feelsLike, int humidity, double wind, int code) {}
