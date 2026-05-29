package com.example.weather.shared.service;

import com.example.weather.shared.model.DailyData;
import com.example.weather.shared.model.GeocodeResult;
import com.example.weather.shared.model.WeatherData;

import java.util.List;

public abstract class WeatherService {
    public abstract GeocodeResult geocode(String city) throws Exception;
    public abstract WeatherData fetchCurrent(GeocodeResult location) throws Exception;
    public abstract List<DailyData> fetchForecast(double lat, double lon, String startDate, String endDate) throws Exception;
}
