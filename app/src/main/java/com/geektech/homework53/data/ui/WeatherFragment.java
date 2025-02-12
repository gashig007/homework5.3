package com.geektech.homework53.data.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.provider.Settings;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.geektech.homework53.R;
import com.geektech.homework53.base.BaseFragment;
import com.geektech.homework53.common.OnItemClick;
import com.geektech.homework53.common.Resource;
import com.geektech.homework53.data.local.WeatherDao;
import com.geektech.homework53.data.model.MainResponse;
import com.geektech.homework53.data.model.System;
import com.geektech.homework53.data.model.Weather;
import com.geektech.homework53.data.model.WeatherApp;
import com.geektech.homework53.data.model.Wind;
import com.geektech.homework53.databinding.FragmentWeatherBinding;
import com.google.zxing.client.android.Intents;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanIntentResult;
import com.journeyapps.barcodescanner.ScanOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class WeatherFragment extends BaseFragment<FragmentWeatherBinding> implements OnItemClick<Integer> {
    private WeatherFragmentArgs args;
    private MainResponse main;
    private System sys;
    private ArrayList<Weather> weatherList = new ArrayList<>();
    private WeatherApp weather;
    private Wind wind;

    private String cityName;

    private WeatherViewModel weatherViewModel;

    @Inject
    WeatherDao dao;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        weatherViewModel = new ViewModelProvider(requireActivity()).get(WeatherViewModel.class);
        cityName = "Bishkek";
        try {
            args = WeatherFragmentArgs.fromBundle(getArguments());
        } catch (Exception e) {
            Log.e("Error:", e.getLocalizedMessage());
        }
        //  weatherViewModel.getWeatherByCityName(args.getCity());
    }

    @Override
    protected FragmentWeatherBinding bind() {
        return FragmentWeatherBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void setupViews() {

    }

    @Override
    protected void callRequests() {
        weatherViewModel.getWeatherByMap(args.getLatitude(), args.getLongitude());
        //   Log.e("args", args.getCity());
//
//        if (args != null) {
//            args = WeatherFragmentArgs.fromBundle(getArguments());
//            /*Log.e("args", args.getCity());
//            cityName = args.getCity();*/
//        } else {
//            Log.e("args", "args is empty");
//            cityName = "Bishkek";
//        }
//        weatherViewModel.getWeatherByCityName(cityName);
    }

    @Override
    protected void setupListeners() {
        binding.rectangle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navController.navigate(R.id.action_weatherFragment_to_weatherDetailFragment);
            }
        });

    }

    protected void setupObservers() {
        weatherViewModel.liveData.observe(getViewLifecycleOwner(), new Observer<Resource<WeatherApp>>() {
            @Override
            public void onChanged(Resource<WeatherApp> resource) {
                switch (resource.status) {
                    case LOADING: {
                        binding.cardView.setVisibility(View.GONE);
                        binding.progressBar.setVisibility(View.VISIBLE);
                        binding.imageIv.setVisibility(View.GONE);
                        break;
                    }
                    case SUCCESS: {
                        binding.cardView.setVisibility(View.VISIBLE);
                        binding.progressBar.setVisibility(View.GONE);
                        binding.imageIv.setVisibility(View.VISIBLE);
                        wind = resource.data.getWind();
                        weather = resource.data;
                        main = resource.data.getMain();
                        sys = resource.data.getSys();
                        weatherList = (ArrayList<Weather>) resource.data.getWeather();
                        binding.progressBar.setVisibility(View.GONE);
                        setCurrentWeather();
                        break;
                    }
                    case ERROR: {
                        Toast.makeText(requireContext(), resource.msg, Toast.LENGTH_SHORT).show();
                        wind = dao.getWeather().get(dao.getWeather().size() - 1).getWind();
                        weather = dao.getWeather().get(dao.getWeather().size() - 1);
                        main = dao.getWeather().get(dao.getWeather().size() - 1).getMain();
                        sys = dao.getWeather().get(dao.getWeather().size() - 1).getSys();
                        binding.progressBar.setVisibility(View.GONE);
                        setCurrentWeatherRoom(dao.getWeather());
                        binding.cardView.setVisibility(View.VISIBLE);
                        binding.imageIv.setVisibility(View.VISIBLE);
                        break;
                    }
                }
            }

            @SuppressLint("SetTextI18n")
            private void setCurrentWeather() {

                binding.locationTv.setText(cityName);
                binding.dateTv.setText(getDate(java.lang.System.currentTimeMillis()));

                //Setting weather status
                binding.weatherStatus.setText(weatherList.get(0).getMain());
                Glide.with(requireContext())
                        .load("https://openweathermap.org/img/wn/" + weatherList.get(0).getIcon() + ".png")
                        .override(100, 100)
                        .into(binding.weatherStatusImg);

                //Setting temperature
                binding.tempTv.setText(Math.round(main.getTemp() - 273.15) + "");
                binding.minTempTv.setText(Math.round(main.getTempMin() - 273.15) + "°C");
                binding.maxTempTv.setText(Math.round(main.getTempMax() - 273.15) + "°C");

                //Setting extras
                binding.humidityTv.setText(main.getHumidity() + "%");
                binding.pressureTv.setText(main.getPressure() + "mBar");
                binding.windTv.setText(wind.getSpeed() + "km/h");

                //Setting sunset and sunrise
                binding.sunriseTv.setText(getTime(requireContext(), Long.valueOf(sys.getSunrise())));
                binding.sunsetTv.setText(getTime(requireContext(), Long.valueOf(sys.getSunset())));

                //Setting daytime
                int daytime = sys.getSunset() - sys.getSunrise();
                binding.daytimeTv.setText(getHours(daytime));
            }

            private void setCurrentWeatherRoom(List<WeatherApp> weatherRoom) {

                binding.locationTv.setText(cityName);
                binding.dateTv.setText(getDate(java.lang.System.currentTimeMillis()));

                //Setting weather status
                binding.weatherStatus.setText(weatherList.get(0).getMain());
//        Glide.with(requireContext())
//                .load("https://openweathermap.org/img/wn/" + weatherList.get(0).getIcon() + ".png")
//                .override(100, 100)
//                .into(binding.weatherStatusImg);

                //Setting temperature
                binding.tempTv.setText(Math.round(weatherRoom.get(weatherRoom.size() - 1).getMain().getTemp() - 273.15) + "");
                binding.minTempTv.setText(Math.round(weatherRoom.get(weatherRoom.size() - 1).getMain().getTempMin() - 273.15) + "°C");
                binding.maxTempTv.setText(Math.round(weatherRoom.get(weatherRoom.size() - 1).getMain().getTempMax() - 273.15) + "°C");

                //Setting extras
                binding.humidityTv.setText(weatherRoom.get(weatherRoom.size() - 1).getMain().getHumidity() + "%");
                binding.pressureTv.setText(weatherRoom.get(weatherRoom.size() - 1).getMain().getPressure() + "mBar");
                binding.windTv.setText(weatherRoom.get(weatherRoom.size() - 1).getWind().getSpeed() + "km/h");

                //Setting sunset and sunrise
                binding.sunriseTv.setText(getTime(requireContext(), Long.valueOf(sys.getSunrise())));
                binding.sunsetTv.setText(getTime(requireContext(), Long.valueOf(sys.getSunset())));

                //Setting daytime
                int daytime = sys.getSunset() - sys.getSunrise();
                binding.daytimeTv.setText(getHours(daytime));
            }

            @SuppressLint("SimpleDateFormat")
            private String getDate(Long date) {
                return new SimpleDateFormat("EEE, d MMMM yyyy | h:mm a").format(date);
            }

            private String getTime(Context context, Long time) {
                return DateUtils.formatDateTime(context, time * 1000, DateUtils.FORMAT_SHOW_TIME);
            }

            private String getHours(int time) {

                int hours = (int) TimeUnit.SECONDS.toHours(time);
                int minutes = (int) ((int) TimeUnit.SECONDS.toMinutes(time) -
                        (TimeUnit.SECONDS.toHours(time) * 60));

                return hours + "h " + minutes + "m";
            }


        });
    }

    @Override
    public void onItemClick(Integer data) {

    }
}