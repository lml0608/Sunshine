/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.sunshine;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.android.sunshine.ForecastAdapter.ForecastAdapterOnClickHandler;
import com.example.android.sunshine.data.SunshinePreferences;
import com.example.android.sunshine.utilities.NetworkUtils;
import com.example.android.sunshine.utilities.OpenWeatherJsonUtils;

import java.net.URL;

import static android.support.v4.app.LoaderManager.*;

public class MainActivity extends AppCompatActivity implements
        ForecastAdapterOnClickHandler, LoaderCallbacks<String[]> {

    private static final String TAG = MainActivity.class.getSimpleName();

    private RecyclerView mRecyclerView;
    private ForecastAdapter mForecastAdapter;

    private TextView mErrorMessageDisplay;

    private ProgressBar mLoadingIndicator;

    private static final int FORECAST_LOADER_ID = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forecast);


        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerview_forecast);


        mErrorMessageDisplay = (TextView) findViewById(R.id.tv_error_message_display);


        LinearLayoutManager layoutManager
                = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);

        mRecyclerView.setLayoutManager(layoutManager);


        mRecyclerView.setHasFixedSize(true);



        mForecastAdapter = new ForecastAdapter(this);


        mRecyclerView.setAdapter(mForecastAdapter);


        mLoadingIndicator = (ProgressBar) findViewById(R.id.pb_loading_indicator);


        int loaderId = FORECAST_LOADER_ID;

        LoaderCallbacks<String[]> callback = MainActivity.this;

        Bundle bundleForLoader = null;

        //第一步初始化任务执行 step1
        getSupportLoaderManager().initLoader(loaderId, bundleForLoader, callback);

    }


    @Override
    public Loader<String[]> onCreateLoader(int id, final Bundle loaderArgs) {
        return new AsyncTaskLoader<String[]>(this) {

            String[] mWeatherData = null;

            //缓存成员变量中的天气数据，并在onStartLoading中传递
            //第二步，自动调用
            @Override
            protected void onStartLoading() {
                if (mWeatherData != null) {
                    deliverResult(mWeatherData);
                }else {
                    //显示进度控件
                    mLoadingIndicator.setVisibility(View.VISIBLE);
                    //开始执行任务
                    forceLoad();
                }
            }

            //这是AsyncTaskLoader的方法，将加载和解析JSON数据
            //第三步，后台执行任务
            @Override
            public String[] loadInBackground() {

                String locationQuery = SunshinePreferences
                        .getPreferredWeatherLocation(MainActivity.this);

                URL weatherRequestUrl = NetworkUtils.buildUrl(locationQuery);

                Log.i(TAG, String.valueOf(weatherRequestUrl));

                try {

                    String jsonWeatherResponse = NetworkUtils.getResponseFromHttpUrl(weatherRequestUrl);

                    String[] simpleJsonWeatherData = OpenWeatherJsonUtils
                            .getSimpleWeatherStringsFromJson(MainActivity.this, jsonWeatherResponse);

                    Log.i(TAG, String.valueOf(simpleJsonWeatherData));

                    //返回天气数据数组
                    return simpleJsonWeatherData;

                }catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }

            }

            //将加载结果发送到注册的侦听器。
            public void deliverResult(String[] data) {

                mWeatherData = data;
                super.deliverResult(data);
            }
        };
    }

    //加载完成后，如果没有数据，则显示数据或错误消息
    //加载程序完成加载时调用。
    //第四步，任务执行完成后，更新UI
    @Override
    public void onLoadFinished(Loader<String[]> loader, String[] data) {
        mLoadingIndicator.setVisibility(View.INVISIBLE);
        mForecastAdapter.setWeatherData(data);
        if (null == data) {
            showErrorMessage();
        }else {
            showWeatherDataView();
        }
    }

    @Override
    public void onLoaderReset(Loader<String[]> loader) {

    }

    private void invalidateData() {

        mForecastAdapter.setWeatherData(null);
    }

    //打开地图软件
    private void openLocationInMap() {
        String addressString = "1600 Ampitheatre Parkway, CA";

        Uri geoLocation = Uri.parse("geo:0,0?q=" + addressString);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(geoLocation);

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
        else {
            Log.d(TAG, "Couldn't call " + geoLocation.toString()
                    + ", no receiving apps installed!");
        }
    }

    //点击事件，打开详情界面
    @Override
    public void onClick(String weatherForDay) {
        Context context = this;


        Intent intent = new Intent(context, DetailActivity.class);

        intent.putExtra(Intent.EXTRA_TEXT, weatherForDay);

        startActivity(intent);
    }


    //正常有数据时，RecyclerView显示，进度条不显示
    private void showWeatherDataView() {
        /* First, make sure the error is invisible */
        mErrorMessageDisplay.setVisibility(View.INVISIBLE);
        /* Then, make sure the weather data is visible */
        mRecyclerView.setVisibility(View.VISIBLE);
    }

    //未加载成功时，RecyclerView不显示，进度条显示
    private void showErrorMessage() {

        mRecyclerView.setVisibility(View.INVISIBLE);

        mErrorMessageDisplay.setVisibility(View.VISIBLE);
    }




    //创建菜单
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();

        inflater.inflate(R.menu.forecast, menu);

        return true;
    }

    //菜单项被选择事件
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_refresh) {
            //重构刷新功能以使用我们的AsyncTaskLoader
            invalidateData();
            getSupportLoaderManager().restartLoader(FORECAST_LOADER_ID, null, this);
            return true;
        }
        if (id == R.id.action_map) {
            openLocationInMap();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}