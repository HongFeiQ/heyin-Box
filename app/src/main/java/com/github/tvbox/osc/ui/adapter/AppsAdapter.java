package com.github.tvbox.osc.ui.adapter;

import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.bean.AppInfo;
import com.github.tvbox.osc.util.HawkConfig;

import java.util.ArrayList;

public class AppsAdapter extends BaseQuickAdapter<AppInfo, BaseViewHolder> {
    public AppsAdapter() {
        super(R.layout.item_apps, new ArrayList<>());
    }

    @Override
    protected void convert(BaseViewHolder helper, AppInfo item) {
        // takagen99: Add Delete Mode
        FrameLayout tvDel = helper.getView(R.id.delFrameLayout);
        if (HawkConfig.hotVodDelete) {
            tvDel.setVisibility(View.VISIBLE);
        } else {
            tvDel.setVisibility(View.GONE);
        }

        helper.setText(R.id.appName, item.getName());
        ImageView ivApps = helper.getView(R.id.ivApps);
        ivApps.setImageDrawable(item.getIcon());
    }
}