package com.sjf.library;

import android.app.Activity;
import android.graphics.Color;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sjf.library.adapter.AreaChooseAdapter;
import com.sjf.library.annotation.AreaLevel;
import com.sjf.library.entity.Area;
import com.sjf.library.entity.City;
import com.sjf.library.entity.County;
import com.sjf.library.entity.Province;
import com.sjf.library.global.Global;
import com.sjf.library.listener.OnChooseCompleteListener;
import com.sjf.library.listener.OnChooseListener;
import com.sjf.library.util.AreaUtil;
import com.sjf.library.util.WindowUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import static com.sjf.library.constant.Constant.COUNTY;
import static com.sjf.library.constant.Constant.CITY;
import static com.sjf.library.constant.Constant.PROVINCE;
import static com.sjf.library.constant.Constant.UNKNOWN_AREA_LEVEL;

/**
 * Function: 区域选择器
 * Author: ShiJingFeng
 * Date: 2019/10/30 18:49
 * Description:
 */
public class AreaChooser {

    private ImageView ivCancel;
    private TextView tvProvinceLabel;
    private TextView tvCityLabel;
    private TextView tvCountyLabel;
    private RecyclerView rvAreaList;

    /** 自定义扩展数据 */
    private final Data mData;
    /** 监听器 */
    private final Listener mListener;
    /** Parent View */
    private final View mParentView;
    /** 区域选择 PopupWindow */
    private PopupWindow mAreaChoicePopupwindow;
    /** 区域选择 适配器 */
    private AreaChooseAdapter mAdapter;
    /** 区域数据 */
    private List<Area> mAreaDataList;
    /** 地区级别 */
    private int mAreaLevel = PROVINCE;

    public AreaChooser(@NonNull Data data, @NonNull Listener listener) {
        this.mData = data;
        this.mListener = listener;
        this.mAreaDataList = data.mAreaDataList;
        this.mParentView = data.mActivity.findViewById(android.R.id.content);
    }

    /**
     * 初始化处理收货地址选择区域点击事件
     */
    private void initAreaAction() {
        //设置为省级数据
        tvProvinceLabel.setOnClickListener(view -> {
            final List<Area> provinceList = mAreaDataList;

            setSelectedStatus(PROVINCE);
            mAdapter.setData(provinceList, mAreaLevel = PROVINCE);
            mAdapter.notifyDataSetChanged();
        });
        //设置为市级数据
        tvCityLabel.setOnClickListener(view -> {
            final int provincePosition = mAdapter.getPosition(PROVINCE);
            final Province province = mAreaDataList.get(provincePosition).getArea();
            final List<Area> cityList = province.getCityList();

            setSelectedStatus(CITY);
            mAdapter.setData(cityList, mAreaLevel = CITY);
            mAdapter.notifyDataSetChanged();
        });
        //设置县级数据
        tvCountyLabel.setOnClickListener(view -> {
            final int provincePosition = mAdapter.getPosition(PROVINCE);
            final int cityPosition = mAdapter.getPosition(CITY);
            final Province province = mAreaDataList.get(provincePosition).getArea();
            final City city = province.getCityList().get(cityPosition).getArea();
            final List<Area> countyList = city.getCountyList();

            setSelectedStatus(COUNTY);
            mAdapter.setData(countyList, mAreaLevel = COUNTY);
            mAdapter.notifyDataSetChanged();
        });
        //取消
        ivCancel.setOnClickListener(view -> {
            mAreaChoicePopupwindow.dismiss();
        });
        //关闭窗口
        mAreaChoicePopupwindow.setOnDismissListener(() -> {
            WindowUtil.setWindowOutsideBackground(mData.mActivity, 1f);
            reset();
        });

        mAdapter.setOnItemEventListener((View view, Object data, int position, int flag) -> {
            switch (mAreaLevel) {
                case PROVINCE:
                    final Province province = (Province) data;
                    final List<Area> cityList = province.getCityList();

                    tvCountyLabel.setText("请选择");
                    tvCountyLabel.setVisibility(View.GONE);
                    tvProvinceLabel.setText(province.getName());

                    if (mData.mLevel == PROVINCE || cityList == null || cityList.size() == 0) {
                        mListener.mOnChooseCompleteListener.onComplete(province, null, null);

                        mAreaChoicePopupwindow.dismiss();
                        return;
                    }

                    tvCityLabel.setText("请选择");
                    setSelectedStatus(CITY);
                    tvCityLabel.setVisibility(View.VISIBLE);

                    //更新数据
                    mAdapter.setData(province.getCityList(), mAreaLevel = CITY);
                    mAdapter.notifyDataSetChanged();
                    break;
                case CITY:
                    final City city = (City) data;
                    final List<Area> countyList = city.getCountyList();

                    tvCityLabel.setText(city.getName());

                    if (mData.mLevel == CITY || countyList == null || countyList.size() == 0) {
                        final int provincePosition = mAdapter.getPosition(PROVINCE);
                        final Province province1 = mAreaDataList.get(provincePosition).getArea();

                        mListener.mOnChooseCompleteListener.onComplete(province1, city, null);

                        mAreaChoicePopupwindow.dismiss();
                        return;
                    }

                    tvCityLabel.setText(city.getName());
                    tvCountyLabel.setText("请选择");
                    setSelectedStatus(COUNTY);
                    tvCountyLabel.setVisibility(View.VISIBLE);

                    //更新数据
                    mAdapter.setData(countyList, mAreaLevel = COUNTY);
                    mAdapter.notifyDataSetChanged();
                    break;
                case COUNTY:
                    final County county = (County) data;

                    tvCountyLabel.setText(county.getName());

                    if (mListener.mOnChooseCompleteListener != null) {
                        final int provincePosition = mAdapter.getPosition(PROVINCE);
                        final int cityPosition = mAdapter.getPosition(CITY);
                        final Province province1 = mAreaDataList.get(provincePosition).getArea();
                        final City city1 = province1.getCityList().get(cityPosition).getArea();

                        mListener.mOnChooseCompleteListener.onComplete(province1, city1, county);
                    }

                    mAreaChoicePopupwindow.dismiss();
                    break;
                default:
                    break;
            }
        });
    }

    /**
     * 设置地区选择显示标签颜色和背景
     *
     * @param areaLevel 区域级别
     */
    private void setSelectedStatus(int areaLevel) {
        switch (areaLevel) {
            case PROVINCE:
                tvProvinceLabel.setTextColor(mData.mActivity.getResources().getColor(R.color.red));
                tvProvinceLabel.setBackground(mData.mActivity.getResources().getDrawable(R.drawable.layer_list_underline_red));

                tvCityLabel.setTextColor(mData.mActivity.getResources().getColor(R.color.black));
                tvCityLabel.setBackground(mData.mActivity.getResources().getDrawable(R.color.white));
                tvCountyLabel.setTextColor(mData.mActivity.getResources().getColor(R.color.black));
                tvCountyLabel.setBackground(mData.mActivity.getResources().getDrawable(R.color.white));
                break;
            case CITY:
                tvProvinceLabel.setTextColor(mData.mActivity.getResources().getColor(R.color.black));
                tvProvinceLabel.setBackground(mData.mActivity.getResources().getDrawable(R.color.white));

                tvCityLabel.setTextColor(mData.mActivity.getResources().getColor(R.color.red));
                tvCityLabel.setBackground(mData.mActivity.getResources().getDrawable(R.drawable.layer_list_underline_red));

                tvCountyLabel.setTextColor(mData.mActivity.getResources().getColor(R.color.black));
                tvCountyLabel.setBackground(mData.mActivity.getResources().getDrawable(R.color.white));
                break;
            case COUNTY:
                tvProvinceLabel.setTextColor(mData.mActivity.getResources().getColor(R.color.black));
                tvProvinceLabel.setBackground(mData.mActivity.getResources().getDrawable(R.color.white));
                tvCityLabel.setTextColor(mData.mActivity.getResources().getColor(R.color.black));
                tvCityLabel.setBackground(mData.mActivity.getResources().getDrawable(R.color.white));

                tvCountyLabel.setTextColor(mData.mActivity.getResources().getColor(R.color.red));
                tvCountyLabel.setBackground(mData.mActivity.getResources().getDrawable(R.drawable.layer_list_underline_red));
                break;
            default:
                break;
        }
    }

    /**
     * 重置
     */
    private void reset() {
        tvCountyLabel.setText("请选择");
        tvCountyLabel.setVisibility(View.GONE);
        tvCityLabel.setText("请选择");
        tvCityLabel.setVisibility(View.GONE);
        tvProvinceLabel.setText("请选择");

        setSelectedStatus(PROVINCE);

        //更新数据
        mAdapter.setData(mAreaDataList, mAreaLevel = PROVINCE);
        mAdapter.notifyDataSetChanged();
        mAdapter.reset();
    }

    /**
     * 显示
     */
    public void show() {
        if (mAreaChoicePopupwindow != null) {
            if (!mAreaChoicePopupwindow.isShowing()) {
                WindowUtil.setWindowOutsideBackground(mData.mActivity, 0.4f);
                mAreaChoicePopupwindow.showAtLocation(mParentView, Gravity.BOTTOM, 0, 0);
            }
            return;
        }

        final LinearLayout llContent = new LinearLayout(mData.mActivity);

        llContent.setOrientation(LinearLayout.VERTICAL);

        //因为layout_area_choice根节点为merge(为了防止PopupWindow布局失效)所以必须绑定到root布局
        final View content = LayoutInflater.from(mData.mActivity).inflate(R.layout.layout_area_choice, llContent, true);

        ivCancel = content.findViewById(R.id.iv_cancel);
        tvProvinceLabel = content.findViewById(R.id.tv_province_label);
        tvCityLabel = content.findViewById(R.id.tv_city_label);
        tvCountyLabel = content.findViewById(R.id.tv_county_label);
        rvAreaList = content.findViewById(R.id.rv_area_list);

        if (mAreaDataList == null) {
            try {
                final InputStream inputStream = mData.mActivity.getAssets().open("json/province_city_county.json");

                mAreaDataList = AreaUtil.getAreaLocalData(inputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        mAdapter = new AreaChooseAdapter(mData.mActivity, mAreaDataList);

        rvAreaList.setLayoutManager(new LinearLayoutManager(mData.mActivity));
        rvAreaList.setAdapter(mAdapter);

        mAreaChoicePopupwindow = new PopupWindow();
        mAreaChoicePopupwindow.setContentView(content);
        mAreaChoicePopupwindow.setWidth(WindowManager.LayoutParams.MATCH_PARENT);
        mAreaChoicePopupwindow.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
        mAreaChoicePopupwindow.setBackgroundDrawable(content.getResources().getDrawable(R.color.white));
        mAreaChoicePopupwindow.setOutsideTouchable(true);
        mAreaChoicePopupwindow.setFocusable(true);
        WindowUtil.setWindowOutsideBackground(mData.mActivity, 0.4f);
        mAreaChoicePopupwindow.showAtLocation(mParentView, Gravity.BOTTOM, 0, 0);

        initAreaAction();

    }

    /**
     * 隐藏
     */
    public void hide() {
        if (mAreaChoicePopupwindow != null) {
            mAreaChoicePopupwindow.dismiss();
        }
    }

    /**
     * 设置省市县（用于初始化显示）
     * @param province 省
     * @param city 市
     * @param county 县
     */
    public AreaChooser setArea(Province province, City city, County county) {
        if (province == null) {
            return this;
        }
        for (int i = 0; i < mAreaDataList.size(); ++i) {
            final Province curProvince = mAreaDataList.get(i).getArea();

            if (curProvince.getCode().equals(province.getCode())) {
                if (city == null) {
                    setPosition(i, -1, -1);
                    return this;
                } else {
                    for (int j = 0; j < curProvince.getCityList().size(); ++j) {
                        final City curCity = curProvince.getCityList().get(j).getArea();

                        if (curCity.getCode().equals(city.getCode())) {
                            if (county == null) {
                                setPosition(i, j, -1);
                                return this;
                            } else {
                                for (int k = 0; k < curCity.getCountyList().size(); ++k) {
                                    final County curCounty = curCity.getCountyList().get(k).getArea();

                                    if (curCounty.getCode().equals(county.getCode())) {
                                        setPosition(i, j, k);
                                        return this;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return this;
    }

    /**
     * 设置邮政编码 (用于初始化显示)
     * @param postalCode 邮政编码
     */
    //TODO 功能待添加
    public AreaChooser setPostalCode(@NonNull String postalCode) {
        this.mData.mPostalCode = postalCode;

        return this;
    }

    /**
     * 设置默认选中的位置
     *
     * @param provincePosition 选中的 省 Position
     * @param cityPosition     选中的 市 Position
     * @param countyPosition   选中的 县 Position
     */
    private void setPosition(int provincePosition, int cityPosition, int countyPosition) {
        int areaLevel = UNKNOWN_AREA_LEVEL;

        if (provincePosition != -1) {
            areaLevel = PROVINCE;
        }
        if (cityPosition != -1) {
            areaLevel = CITY;
        }
        if (countyPosition != -1) {
            areaLevel = COUNTY;
        }

        Province province;
        City city;
        County county;

        final List<Area> areaList;

        switch (areaLevel) {
            case PROVINCE:
                province = mAreaDataList.get(provincePosition).getArea();
                areaList = mAreaDataList;

                tvProvinceLabel.setText(province.getName());
                tvProvinceLabel.setVisibility(View.VISIBLE);

                tvCityLabel.setText("请选择");
                tvCityLabel.setVisibility(View.GONE);

                tvCountyLabel.setText("请选择");
                tvCountyLabel.setVisibility(View.GONE);

                setSelectedStatus(PROVINCE);
                break;
            case CITY:
                province = mAreaDataList.get(provincePosition).getArea();
                city = province.getCityList().get(cityPosition).getArea();
                areaList = province.getCityList();

                tvProvinceLabel.setText(province.getName());
                tvProvinceLabel.setVisibility(View.VISIBLE);

                tvCityLabel.setText(city.getName());
                tvCityLabel.setVisibility(View.VISIBLE);

                tvCountyLabel.setText("请选择");
                tvCountyLabel.setVisibility(View.GONE);

                setSelectedStatus(CITY);
                break;
            case COUNTY:
                province = mAreaDataList.get(provincePosition).getArea();
                city = province.getCityList().get(cityPosition).getArea();
                county = city.getCountyList().get(countyPosition).getArea();
                areaList = city.getCountyList();

                tvProvinceLabel.setText(province.getName());
                tvProvinceLabel.setVisibility(View.VISIBLE);

                tvCityLabel.setText(city.getName());
                tvCityLabel.setVisibility(View.VISIBLE);

                tvCountyLabel.setText(county.getName());
                tvCountyLabel.setVisibility(View.VISIBLE);

                setSelectedStatus(COUNTY);
                break;
            default:
                areaList = null;
                break;
        }
        mAdapter.setPosition(provincePosition, cityPosition, countyPosition);
        mAdapter.setData(areaList, mAreaLevel = areaLevel);
        mAdapter.notifyDataSetChanged();
    }

    /**
     * 获取指定级别的数据
     * @param areaLevel 指定级别
     * @return 数据
     */
    public List<Area> getAreaDataList(int areaLevel) {
        List<Area> areaList = null;

        switch (areaLevel) {
            case PROVINCE:
                areaList = mAreaDataList;
                break;
            case CITY:
                final int provincePosition1 = mAdapter.getPosition(PROVINCE);

                if (provincePosition1 > -1) {
                    final Province province = mAreaDataList.get(provincePosition1).getArea();

                    areaList = province.getCityList();
                }
                break;
            case COUNTY:
                final int provincePosition2 = mAdapter.getPosition(PROVINCE);

                if (provincePosition2 > -1) {
                    final Province province = mAreaDataList.get(provincePosition2).getArea();
                    final int cityPosition = mAdapter.getPosition(CITY);

                    if (cityPosition > -1) {
                        final City city = province.getCityList().get(cityPosition).getArea();

                        areaList = city.getCountyList();
                    }
                }
                break;
            default:
                break;
        }

        return areaList;
    }

    /**
     * 设置选择区域完成监听器
     * @param listener 监听器
     * @return Builder
     */
    public AreaChooser setOnChooseCompleteListener(OnChooseCompleteListener listener) {
        this.mListener.mOnChooseCompleteListener = listener;
        return this;
    }

    /**
     * 选择区域监听器
     * @param listener 监听器
     * @return Builder
     */
    public AreaChooser setOnChooseListener(OnChooseListener listener) {
        this.mListener.mOnChooseListener = listener;
        return this;
    }

    /**
     * 构建器类
     */
    public static class Builder {

        private Data mData = new Data();
        private Listener mListener = new Listener();

        public Builder(@NonNull Activity activity) {
            this.mData.mActivity = activity;
        }

        /**
         * 设置联动级别
         * @param level 级别
         * @return Builder
         */
        public Builder setLevel(@AreaLevel int level) {
            this.mData.mLevel = level;
            return this;
        }

        /**
         * 设置主题颜色
         * @param color 主题颜色
         * @return Builder
         */
        public Builder setThemeColor(@ColorInt int color) {
            this.mData.mColor = color;
            return this;
        }

        /**
         * 设置邮政编码
         * @return Builder
         */
        public Builder setPostalCode(@NonNull String postalCode) {
            this.mData.mPostalCode = postalCode;
            return this;
        }

        /**
         * 省市县数据
         * @param areaDataList 数据
         * @return Builder
         */
        public Builder setAreaData(@NonNull List<Area> areaDataList) {
            this.mData.mAreaDataList = areaDataList;
            return this;
        }

        /**
         * 设置选择区域完成监听器
         * @param listener 监听器
         * @return Builder
         */
        public Builder setOnChooseCompleteListener(OnChooseCompleteListener listener) {
            this.mListener.mOnChooseCompleteListener = listener;
            return this;
        }

        /**
         * 选择区域监听器
         * @param listener 监听器
         * @return Builder
         */
        public Builder setOnChooseListener(OnChooseListener listener) {
            this.mListener.mOnChooseListener = listener;
            return this;
        }

        /**
         * 创建AreaChooser
         * @return AreaChooser
         */
        public AreaChooser create() {
            return new AreaChooser(mData, mListener);
        }

        /**
         * 显示 AreaChooser
         * @return AreaChooser
         */
        public AreaChooser show() {
            final AreaChooser areaChooser = new AreaChooser(mData, mListener);

            areaChooser.show();

            return areaChooser;
        }
    }

    /**
     * 自定义扩展数据类
     */
    private static class Data {

        /** Context */
        private Activity mActivity;
        /** 联动级别 默认3级联动（省市县）*/
        private @AreaLevel int mLevel = COUNTY;
        /** 主题颜色 默认红色 */
        private int mColor = Color.parseColor("#FF0000");
        /** 邮政编码 */
        private String mPostalCode;
        /** 省市县数据 */
        private List<Area> mAreaDataList;

    }

    /**
     * 监听器
     */
    private static class Listener {

        /** 选择区域完成监听器 */
        private OnChooseCompleteListener mOnChooseCompleteListener;
        /** 选择区域监听器 */
        private OnChooseListener mOnChooseListener;

    }

}
