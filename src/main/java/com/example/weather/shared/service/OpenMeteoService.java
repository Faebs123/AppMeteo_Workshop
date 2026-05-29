package com.example.weather.shared.service;

import com.example.weather.shared.model.DailyData;
import com.example.weather.shared.model.GeocodeResult;
import com.example.weather.shared.model.WeatherData;
import com.example.weather.shared.service.WeatherService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class OpenMeteoService extends WeatherService {
    private final HttpClient client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2).build();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public GeocodeResult geocode(String city) throws Exception {
        String encoded = URLEncoder.encode(city, StandardCharsets.UTF_8);
        String url = "https://geocoding-api.open-meteo.com/v1/search?name=" + encoded
                + "&count=1&language=it&format=json";
        JsonNode data = fetchJson(url);
        JsonNode results = data.get("results");
        if (results == null || !results.isArray() || results.isEmpty())
            throw new RuntimeException("Localit\u00e0 \"" + city + "\" non trovata.");
        JsonNode first = results.get(0);
        String tz = first.has("timezone") ? first.get("timezone").asText() : "UTC";
        return new GeocodeResult(
            first.get("name").asText(),
            first.has("country_code") ? first.get("country_code").asText().toUpperCase() : "",
            first.get("latitude").asDouble(),
            first.get("longitude").asDouble(),
            tz);
    }

    @Override
    public List<String[]> suggestCities(String query) throws Exception {
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = "https://geocoding-api.open-meteo.com/v1/search?name=" + encoded
                + "&count=5&language=it&format=json";
        JsonNode data = fetchJson(url);
        JsonNode results = data.get("results");
        List<String[]> list = new ArrayList<>();
        if (results == null || !results.isArray()) return list;
        for (JsonNode r : results) {
            String name = r.has("name") ? r.get("name").asText() : "";
            String country = r.has("country_code") ? r.get("country_code").asText().toUpperCase() : "";
            list.add(new String[]{name, country});
        }
        return list;
    }

    @Override
    public WeatherData fetchCurrent(GeocodeResult loc) throws Exception {
        String url = String.format(
            "https://api.open-meteo.com/v1/forecast?latitude=%f&longitude=%f" +
            "&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m,is_day",
            loc.lat(), loc.lon());
        JsonNode w = fetchJson(url).at("/current");
        return new WeatherData(loc.cityName(), loc.country(),
            w.get("temperature_2m").asDouble(),
            w.get("apparent_temperature").asDouble(),
            w.get("relative_humidity_2m").asInt(),
            w.get("wind_speed_10m").asDouble(),
            w.get("weather_code").asInt(),
            w.get("is_day").asInt() == 1);
    }

    @Override
    public List<DailyData> fetchForecast(double lat, double lon, String startDate, String endDate) throws Exception {
        String url = String.format(
            "https://api.open-meteo.com/v1/forecast?latitude=%f&longitude=%f" +
            "&daily=temperature_2m_max,temperature_2m_min,wind_speed_10m_max" +
            "&timezone=auto&start_date=%s&end_date=%s", lat, lon, startDate, endDate);
        JsonNode daily = fetchJson(url).get("daily");
        List<DailyData> list = new ArrayList<>();
        JsonNode dates = daily.get("time");
        JsonNode tMax = daily.get("temperature_2m_max");
        JsonNode tMin = daily.get("temperature_2m_min");
        JsonNode wMax = daily.get("wind_speed_10m_max");
        for (int i = 0; i < dates.size(); i++) {
            list.add(new DailyData(
                dates.get(i).asText(),
                tMax.get(i).asDouble(),
                tMin.get(i).asDouble(),
                wMax.get(i).asDouble()));
        }
        return list;
    }

    private JsonNode fetchJson(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200)
            throw new RuntimeException("Errore API: " + resp.statusCode() + " " + resp.body());
        return mapper.readTree(resp.body());
    }
}
