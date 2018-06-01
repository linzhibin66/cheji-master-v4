package com.dgcheshang.cheji.Adapter;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.dgcheshang.cheji.Activity.Lukao.LukaoListActivity;
import com.dgcheshang.cheji.Activity.Lukao.LukaoListDetailActivity;
import com.dgcheshang.cheji.Bean.LukaoBean;
import com.dgcheshang.cheji.R;
import com.dgcheshang.cheji.Tools.IsMediaPlayer;

import java.util.List;

/**
 *路考adapter
 */

public class LukaoAdapter extends RecyclerView.Adapter<LukaoAdapter.ViewHolder> {
    MyItemClickListener myItemClickListener;
    MyItemLongClickListener myItemLongClickListener;
    Context mContent;
    String[] list;
    int[] imagelist;
    String who;
    CheckBox checkbox;
    boolean ischeckbox;
    SharedPreferences coachsp;

    public LukaoAdapter(LukaoListActivity lukaoListActivity, String[] list, int[] image, String biaoji, CheckBox checkbox, SharedPreferences coachsp) {
        this.mContent=lukaoListActivity;
        this.list=list;
        this.imagelist=image;
        this.who=biaoji;
        this.checkbox=checkbox;
        this.coachsp=coachsp;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContent).inflate(R.layout.lukao_list_item, parent, false);
        ViewHolder holder = new ViewHolder(view,myItemClickListener,myItemLongClickListener);
        return holder;
    }
    /**
     * 操作
     * */
    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        if(who.equals("denguang")){
            holder.tv_list_name.setText(list[position]);
        }else {
            holder.tv_list_name.setText(list[position+12]);
        }
        //选择框点击
        checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                ischeckbox=isChecked;
                SharedPreferences.Editor edit = coachsp.edit();
                edit.putBoolean("isshowdetail",isChecked);
                edit.commit();


            }
        });

        holder.icon.setBackgroundResource(imagelist[position]);
        holder.layout_list.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(ischeckbox){
                    Intent intent = new Intent();
                    intent.setClass(mContent, LukaoListDetailActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                intent.putExtra("position",position);
                    if(who.equals("denguang")){
                        intent.putExtra("position",position);
                        intent.putExtra("title",list[position]);
                    }else {
                        intent.putExtra("position",position+12);
                        intent.putExtra("title",list[position+12]);
                    }
                    intent.putExtra("who",who);//richang,denguang
                    mContent.startActivity(intent);
                }else {
                    IsMediaPlayer.isRelease();
                    String url="";
                    if(who.equals("denguang")) {
                        url="/mnt/sdcard/chejidoal/lukao"+(position+1)+ ".ogg";
                    }else {
                        url="/mnt/sdcard/chejidoal/lukao"+(position+13)+ ".ogg";
                    }
                    IsMediaPlayer.isplay(url);
                }

            }
        });

    }

    /**
     * 获取控件
     * */
    @Override
    public int getItemCount() {
        return imagelist.length;

    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener,View.OnLongClickListener{
        ImageView icon;
        TextView tv_list_name;
        View layout_list;
        MyItemClickListener myItemClick;
        MyItemLongClickListener myItemLongClick;
        public ViewHolder(View view ,MyItemClickListener myItemClickListener, MyItemLongClickListener myItemLongClickListener) {
            super(view);
            icon = (ImageView) view.findViewById(R.id.image_list_icon);//图标
            tv_list_name = (TextView) view.findViewById(R.id.tv_list_name);//名称
            layout_list = view.findViewById(R.id.layout_list);
            this.myItemClick = myItemClickListener;
            this.myItemLongClick = myItemLongClickListener;
            view.setOnClickListener(this);
            view.setOnLongClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (myItemClick != null) {
                myItemClick.onItemClick(v, getPosition());
            }
        }

        @Override
        public boolean onLongClick(View v) {
            if (myItemLongClick != null) {
                myItemLongClick.onItemLongClick(v, getPosition());
            }
            return false;
        }
    }

    public interface MyItemClickListener{
        void onItemClick(View V,int Position);
    }

    public interface MyItemLongClickListener{
        void onItemLongClick(View V,int Position);
    }

    public void setMyItemClickListener(MyItemClickListener myItemClickListener) {
        this.myItemClickListener = myItemClickListener;
    }

    public void setMyItemLongClickListener(MyItemLongClickListener myItemLongClickListener) {
        this.myItemLongClickListener = myItemLongClickListener;
    }

}
