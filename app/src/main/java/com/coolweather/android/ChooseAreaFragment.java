package com.coolweather.android;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.coolweather.android.db.City;
import com.coolweather.android.db.County;
import com.coolweather.android.db.Province;
import com.coolweather.android.util.HttpUtil;
import com.coolweather.android.util.Utility;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.net.PortUnreachableException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ChooseAreaFragment extends Fragment {
    public static final int LEVEL_PROVINCE = 0;
    public static final int LEVEL_CITY = 1;
    public static final int LEVEL_COUNTY = 2;
    private ProgressDialog progressDialog;//?
    private TextView titleText;
    private Button backButton;
    private ListView listView;
    private ArrayAdapter<String> adapter;//什么用？
    private List<String> dataList = new ArrayList<>();
    /*省列表*/
    private List<Province> provinceList;
    /*市列表*/
    private List<City> cityList;
    /*县列表*/
    private List<County> countyList;
    /*选中的省份*/
    private Province selectedProvince;
    /*选中的城市*/
    private City selectedCity;
    /*当前选中的级别*/
    private int currentLevel;

    @Nullable
    @Override
    /*获取控件的实列，初始化ArrayAdapter，设置为ListView的适配器*/
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.choose_area, container, false);
        titleText = (TextView) view.findViewById(R.id.title_text);
        backButton = (Button) view.findViewById(R.id.back_button);
        listView = (ListView) view.findViewById(R.id.list_view);
        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, dataList);
        listView.setAdapter(adapter);
        Log.d("ChooseAreaFragment","控件初始化完成");
        return view;
    }

    /*给Button和ListView设置点击事件*/
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (currentLevel == LEVEL_PROVINCE) {
                    selectedProvince = provinceList.get(position);
                    Log.d("ChooseAreaFragment","开始加载城市数据。。。。。。。。。");
                    queryCities();
                    Log.d("ChooseAreaFragment","加载城市数据完成。。。。。。。。。");
                } else if (currentLevel == LEVEL_CITY) {
                    selectedCity = cityList.get(position);
                    Log.d("ChooseAreaFragment","开始加载县级数据。。。。。。。。。");
                    queryCounties();
                    Log.d("ChooseAreaFragment","加载县级数据完成。。。。。。。。。");
                }else if(currentLevel==LEVEL_COUNTY) {
                    String weatherId = countyList.get(position).getWeatherId();
                    if (getActivity() instanceof MainActivity) {
                        //通过当前的活动打开weatherActivity活动
                        Intent intent = new Intent(getActivity(), WeatherActivity.class);
                        intent.putExtra("weather_id", weatherId);
                        startActivity(intent);
                        getActivity().finish();
                    } else if (getActivity() instanceof WeatherActivity) {
                        WeatherActivity activity=(WeatherActivity)getActivity();
                        activity.drawerLayout.closeDrawers();
                        activity.switchRefresh.setRefreshing(true);
                        activity.requestWeather(weatherId);
                    }
                }
            }
        });
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentLevel == LEVEL_COUNTY) {
                    queryCities();
                } else if (currentLevel == LEVEL_CITY) {
                    queryProvinces();
                }
            }
        });
        Log.d("ChooseAreaFragment","开始加载省级数据。。。。。。。。。");
        queryProvinces();//？
        Log.d("ChooseAreaFragment","加载省级数据完成。。。。。。。。。");
    }

    /*查询全国所有的省，优先从数据库查询，如果没有查询到再去服务器上查询*/
    private void queryProvinces() {
        titleText.setText("中国");
        backButton.setVisibility(View.GONE);
        provinceList = DataSupport.findAll(Province.class);//从数据库查询省份数据
        if (provinceList.size() > 0) {
            dataList.clear();//清除缓存数据的dataList
            for (Province province : provinceList) {
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();//刷新数据到ListView显示界面
            listView.setSelection(0);
            currentLevel = LEVEL_PROVINCE;
        } else {
            Log.d("ChooseAreaFragment","从服务器加载省级数据。。。。。。。。。");
            String address = "http://guolin.tech/api/china";
            queryFromServer(address, "province");
            Log.d("ChooseAreaFragment","从服务器加载省级数据完成。。。。。。。。。");
        }
    }

    private void queryCounties() {
        /*查询选中市内所有的县，优先从数据库查询，如果没有查询到再去服务器上查询*/
        titleText.setText(selectedCity.getCityName());
        backButton.setVisibility(View.VISIBLE);
        countyList = DataSupport.where("cityid=?", String.valueOf(selectedCity.getId())).find(County.class);
        if (countyList.size() > 0) {
            dataList.clear();
            for (County county : countyList) {
                dataList.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_COUNTY;
        } else {
            int provinceCode = selectedProvince.getProvinceCode();
            int cityCode = selectedCity.getCityCode();
            String address = "http://guolin.tech/api/china/" +provinceCode+"/" +cityCode;
            Log.d("ChooseAreaFragment","从服务器加载县级数据。。。。。。。。。");
            queryFromServer(address, "county");
            Log.d("ChooseAreaFragment","从服务器加载县级数据完成。。。。。。。。。");
        }

    }

    /*查询选中省内所有的市，优先从数据库查询，如果没有查询到再去服务器上查询*/
    private void queryCities() {
        titleText.setText(selectedProvince.getProvinceName());
        backButton.setVisibility(View.VISIBLE);
        cityList = DataSupport.where("provinceid=?", String.valueOf(selectedProvince.getId())).find(City.class);
        if (cityList.size() > 0) {
            dataList.clear();
            for (City city : cityList) {
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_CITY;
        } else {
            int provinceCode = selectedProvince.getProvinceCode();
            Log.d("ChooseAreaFragment","从服务器加载城市数据。。。。。。。。。");
            String address = "http://guolin.tech/api/china/" + provinceCode;
            queryFromServer(address, "city");
            Log.d("ChooseAreaFragment","从服务器加载城市数据完成。。。。。。。。。");
        }

    }
    /*根据传入的地址和字符串从服务器上查询数据*/
    private void queryFromServer(String address, final String type) {
        showProgressDialog();
        //通过回调接口，实现服务器的数据的查询
        Log.d("ChooseAreaFragment","开始从服务器加载省级数据。。。。。。。。。");
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
            //通过runOnUiThread方法回到主线程执行处理逻辑
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(getContext(),"加载失败",Toast.LENGTH_SHORT).show();
                    }
                });
            }

            /*向服务器发送请求后的响应数据会回调到onResponse方法中*/
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.d("ChooseAreaFragment","返回由服务器加载的数据。。。。。。。。。");
                String responseText=response.body().string();
                boolean result=false;
                if("province".equals(type)){
                    Log.d("ChooseAreaFragment","解析服务器加载的省级数据。。。。。。。。。");
                    result= Utility.handleProvinceResponse(responseText);//解析服务器传回的json数据，并保存到数据库
                }else if("city".equals(type)){
                    Log.d("ChooseAreaFragment","解析服务器加载的城市数据。。。。。。。。。");
                    result=Utility.handleCityResponse(responseText,selectedProvince.getId());
                }else if("county".equals(type)){
                    Log.d("ChooseAreaFragment","解析服务器加载的县级数据。。。。。。。。。");
                    result=Utility.handleCountyResponse(responseText,selectedCity.getId());
                }
                /*从服务器解析和处理完数据后，在主线程中实现将数据展示到界面*/
                if(result){
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            if ("province".equals(type)) {
                                queryProvinces();
                            } else if ("city".equals(type)) {
                                queryCities();
                            } else if ("county".equals(type)) {
                                queryCounties();
                            }
                        }
                    });
                }
            }
        });
    }
    /*显示进度对话框*/
    private void showProgressDialog(){
        if(progressDialog==null){
            progressDialog=new ProgressDialog(getActivity());
            progressDialog.setMessage("正在加载。。。");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }
    /*关闭进度对话框*/
    private void closeProgressDialog(){
        if(progressDialog!=null){
            progressDialog.dismiss();
        }
    }
}
