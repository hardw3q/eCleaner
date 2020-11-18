package com.kikimore.ecleaner.adapter;

import java.io.File;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.kikimore.ecleaner.R;
import com.kikimore.ecleaner.model.ChildItem;
import com.kikimore.ecleaner.model.GroupItem;
import com.kikimore.ecleaner.utils.Utils;


/**
 * Copyright © 2016 AsianTech inc.
 * Created by HuongNV on 13/02/2016.
 */

public class CleanAdapter extends AnimatedExpandableListView.AnimatedExpandableListAdapter {
    private LayoutInflater inflater;
    private Context mContext;
    private List<GroupItem> items;
    private OnGroupClickListener mOnGroupClickListener;

    public CleanAdapter(Context context, List<GroupItem> items,
                        OnGroupClickListener onGroupClickListener) {
        inflater = LayoutInflater.from(context);
        mContext = context;
        this.items = items;
        mOnGroupClickListener = onGroupClickListener;
    }

    @Override
    public ChildItem getChild(int groupPosition, int childPosition) {
        return items.get(groupPosition).getItems().get(childPosition);
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public View getRealChildView(final int groupPosition, final int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        final ChildHolder holder;
        final ChildItem item = getChild(groupPosition, childPosition);
        if (convertView == null) {
            holder = new ChildHolder();
            convertView = inflater.inflate(R.layout.item, parent, false);
            holder.tvName = (TextView) convertView.findViewById(R.id.tvName);
            holder.tvName.setSelected(true);
            holder.tvSize = (TextView) convertView.findViewById(R.id.tvSize);
            holder.imgIconApp = (ImageView) convertView.findViewById(R.id.imgIconApp);
            holder.viewIconFile = (RelativeLayout) convertView.findViewById(R.id.viewIconFile);
            holder.imgFileApk = (ImageView) convertView.findViewById(R.id.imgFileApk);
            holder.ckItem = (CheckBox) convertView.findViewById(R.id.ckItem);
            convertView.setTag(holder);
        } else {
            holder = (ChildHolder) convertView.getTag();
        }

        holder.tvName.setText(item.getApplicationName());
        holder.tvSize.setText(Utils.formatSize(item.getCacheSize()));
        holder.ckItem.setChecked(item.isCheck());
        holder.ckItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mOnGroupClickListener.onSelectItem(groupPosition, childPosition, holder.ckItem.isChecked());
            }
        });
        switch (item.getType()) {
            case ChildItem.TYPE_APKS:
                holder.viewIconFile.setVisibility(View.VISIBLE);
                holder.imgIconApp.setVisibility(View.GONE);
                holder.imgFileApk.setImageDrawable(ContextCompat.getDrawable(mContext,
                        R.drawable.ic_android_white_24dp));
                holder.ckItem.setVisibility(View.VISIBLE);
                break;
            case ChildItem.TYPE_DOWNLOAD_FILE:
                holder.viewIconFile.setVisibility(View.VISIBLE);
                holder.imgIconApp.setVisibility(View.GONE);
                holder.imgFileApk.setImageDrawable(ContextCompat.getDrawable(mContext,
                        R.drawable.ic_file_download_white_24dp));
                holder.ckItem.setVisibility(View.VISIBLE);
                break;
            case ChildItem.TYPE_LARGE_FILES:
                holder.viewIconFile.setVisibility(View.VISIBLE);
                holder.imgIconApp.setVisibility(View.GONE);
                holder.imgFileApk.setImageDrawable(ContextCompat.getDrawable(mContext,
                        R.drawable.ic_description_white_24dp));
                if (!item.getPath().endsWith(".m3g")) {
                    File file = new File(item.getPath());
                    final Uri uri = FileProvider.getUriForFile(mContext, mContext.getApplicationContext().getPackageName() + ".provider", file);
                    holder.baseView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            mContext.startActivity(intent);
                        }
                    });
                }
                holder.ckItem.setVisibility(View.VISIBLE);
                break;
            case ChildItem.TYPE_CACHE:
                holder.viewIconFile.setVisibility(View.GONE);
                holder.imgIconApp.setVisibility(View.VISIBLE);
                holder.imgIconApp.setImageDrawable(item.getApplicationIcon());

                holder.ckItem.setVisibility(View.GONE);
                break;
            case GroupItem.TYPE_APP_DATA:
                holder.imgIconApp.setVisibility(View.VISIBLE);
                holder.viewIconFile.setVisibility(View.GONE);
                holder.imgIconApp.setImageDrawable(item.getApplicationIcon());
                break;
            case GroupItem.TYPE_APP_CACHE:
                holder.imgIconApp.setVisibility(View.VISIBLE);
                holder.viewIconFile.setVisibility(View.GONE);
                holder.imgIconApp.setImageDrawable(item.getApplicationIcon());
                break;
            case GroupItem.TYPE_RESIDUAL_FILES:
                holder.imgIconApp.setVisibility(View.GONE);
                holder.viewIconFile.setVisibility(View.VISIBLE);
            default:
                break;
        }
        return convertView;
    }

    @Override
    public int getRealChildrenCount(int groupPosition) {
        return items.get(groupPosition).getItems().size();
    }

    @Override
    public GroupItem getGroup(int groupPosition) {
        return items.get(groupPosition);
    }

    @Override
    public int getGroupCount() {
        return items.size();
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public View getGroupView(final int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        final GroupHolder holder;
        GroupItem item = getGroup(groupPosition);
        if (convertView == null) {
            holder = new GroupHolder();
            convertView = inflater.inflate(R.layout.item_header, parent, false);
            holder.tvHeaderSize = (TextView) convertView.findViewById(R.id.tvHeaderSize);
            holder.tvName = (TextView) convertView.findViewById(R.id.item_header_name);
            holder.ckHeader = (CheckBox) convertView.findViewById(R.id.ckHeader);
            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mOnGroupClickListener.onGroupClick(groupPosition);
                }
            });
            holder.ckHeader.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mOnGroupClickListener.onSelectItemHeader(groupPosition, holder.ckHeader.isChecked());
                }
            });
            convertView.setTag(holder);
        } else {
            holder = (GroupHolder) convertView.getTag();
        }

        holder.tvName.setText(item.getTitle());
        holder.tvHeaderSize.setText(Utils.formatSize(item.getTotal()));
        holder.ckHeader.setChecked(item.isCheck());

        return convertView;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    public interface OnGroupClickListener {
        void onGroupClick(int groupPosition);

        void onSelectItemHeader(int position, boolean isCheck);

        void onSelectItem(int groupPosition, int childPosition, boolean isCheck);
    }

    @Override
    public boolean isChildSelectable(int arg0, int arg1) {
        return true;
    }

    private static class ChildHolder {
        private TextView tvName;
        private TextView tvSize;
        private ImageView imgIconApp;
        private RelativeLayout viewIconFile;
        private ImageView imgFileApk;
        private CheckBox ckItem;
        private FrameLayout baseView;
    }

    private static class GroupHolder {
        private TextView tvName;
        private TextView tvHeaderSize;
        private CheckBox ckHeader;
    }

}
