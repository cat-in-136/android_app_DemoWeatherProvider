package cat_in_136.github.io.demoweatherprovider;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import cyanogenmod.providers.WeatherContract;
import cyanogenmod.weather.RequestInfo;
import cyanogenmod.weather.WeatherInfo;
import cyanogenmod.weather.WeatherLocation;
import cyanogenmod.weatherservice.ServiceRequest;
import cyanogenmod.weatherservice.ServiceRequestResult;
import cyanogenmod.weatherservice.WeatherProviderService;

public class DemoWeatherProviderService extends WeatherProviderService {
    private static final String TAG = DemoWeatherProviderService.class.getSimpleName();
    private static final int SERVICE_REQUEST_CANCELLED = -1;
    private static final int SERVICE_REQUEST_SUBMITTED = 0;

    private SharedPreferences sharedPreferences;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate()");

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    }

    @Override
    protected void onRequestSubmitted(ServiceRequest serviceRequest) {
        RequestInfo requestInfo = serviceRequest.getRequestInfo();
        int requestType = requestInfo.getRequestType();
        Log.d(TAG, "Received request type " + requestType);

        switch (requestType) {
            case RequestInfo.TYPE_WEATHER_BY_GEO_LOCATION_REQ:
            case RequestInfo.TYPE_WEATHER_BY_WEATHER_LOCATION_REQ:
                handleWeatherRequest(serviceRequest);
                break;
            case RequestInfo.TYPE_LOOKUP_CITY_NAME_REQ:
                handleLookupRequest(serviceRequest);
                break;
            default:
                serviceRequest.fail();
                break;
        }
    }

    @Override
    protected void onRequestCancelled(ServiceRequest serviceRequest) {

    }

    private void handleWeatherRequest(final ServiceRequest serviceRequest) {
        final RequestInfo requestInfo = serviceRequest.getRequestInfo();
        Log.d(TAG, "Received weather request info: " + requestInfo.toString());

        String cityName = getCityNameOfService(requestInfo);
        int current_temperature = Integer.parseInt(sharedPreferences.getString("current_temperature", "0"), 10);
        int temperature_unit = sharedPreferences.getString("temperature_unit", "C").equalsIgnoreCase("K")?
                WeatherContract.WeatherColumns.TempUnit.FAHRENHEIT : WeatherContract.WeatherColumns.TempUnit.CELSIUS;
        int wind_speed_unit = sharedPreferences.getString("wind_speed_unit", "kph").equalsIgnoreCase("mph")?
                WeatherContract.WeatherColumns.WindSpeedUnit.MPH : WeatherContract.WeatherColumns.WindSpeedUnit.KPH;
        float current_wind_speed = Float.parseFloat(sharedPreferences.getString("current_wind_speed", "0"));
        float current_wind_direction = Float.parseFloat(sharedPreferences.getString("current_wind_direction", "0"));

        // Setup dummy weather status
        WeatherInfo.Builder weatherInfoBuilder = new WeatherInfo.Builder(cityName, current_temperature, temperature_unit);
        String current_weather_status = sharedPreferences.getString("current_weather_status", String.valueOf(WeatherContract.WeatherColumns.WeatherCode.NOT_AVAILABLE));
        weatherInfoBuilder.setWeatherCondition(Integer.parseInt(current_weather_status, 10));
        weatherInfoBuilder.setHumidity(Integer.parseInt(sharedPreferences.getString("current_humidity", "0"), 10));
        weatherInfoBuilder.setWind(current_wind_speed, current_wind_direction, wind_speed_unit);
        weatherInfoBuilder.setTodaysHigh(Integer.parseInt(sharedPreferences.getString("today_high", "0"), 10));
        weatherInfoBuilder.setTodaysLow(Integer.parseInt(sharedPreferences.getString("today_low", "0"), 10));

        List<WeatherInfo.DayForecast> forecast = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            String forecast_weather_status = sharedPreferences.getString("forecast_" + i + "_weather_status", String.valueOf(WeatherContract.WeatherColumns.WeatherCode.NOT_AVAILABLE));

            WeatherInfo.DayForecast.Builder dayForecastBuilder = new WeatherInfo.DayForecast.Builder(Integer.parseInt(forecast_weather_status));
            dayForecastBuilder.setHigh(Integer.parseInt(sharedPreferences.getString("forecast_" + i + "_high", "0"), 10));
            dayForecastBuilder.setLow(Integer.parseInt(sharedPreferences.getString("forecast_" + i + "_low", "0"), 10));
            forecast.add(dayForecastBuilder.build());
        }
        weatherInfoBuilder.setForecast(forecast);

        ServiceRequestResult serviceRequestResult = new ServiceRequestResult.Builder(weatherInfoBuilder.build()).build();
        serviceRequest.complete(serviceRequestResult);
    }

    private void handleLookupRequest(final ServiceRequest serviceRequest) {
        final RequestInfo requestInfo = serviceRequest.getRequestInfo();
        Log.d(TAG, "Received lookup request info: " + requestInfo.toString());

        String cityName = getCityNameOfService(requestInfo);

        // Setup dummy location
        List<WeatherLocation> weatherLocation = new ArrayList<>();
        WeatherLocation.Builder weatherLocationBuilder;
        weatherLocationBuilder = new WeatherLocation.Builder(cityName, cityName);
        weatherLocationBuilder.setState(cityName);
        weatherLocationBuilder.setPostalCode(cityName);
        weatherLocationBuilder.setCountry(cityName);
        weatherLocationBuilder.setCountryId(cityName);
        weatherLocation.add(weatherLocationBuilder.build());

        ServiceRequestResult serviceRequestResult = new ServiceRequestResult.Builder(weatherLocation).build();
        serviceRequest.complete(serviceRequestResult);
    }

    private String getCityNameOfService(final RequestInfo requestInfo) {
        String cityName = sharedPreferences.getString("default_city_name", "?");
        if (requestInfo.getCityName() != null) {
            cityName = requestInfo.getCityName();
        } else if (requestInfo.getWeatherLocation() != null && requestInfo.getWeatherLocation().getCity() != null) {
            cityName = requestInfo.getWeatherLocation().getCity();
        }

        return cityName;
    }
}
