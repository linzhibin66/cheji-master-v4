package com.dgcheshang.cheji.Adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import com.dgcheshang.cheji.Database.DbHandle;
import com.dgcheshang.cheji.R;
import com.dgcheshang.cheji.netty.po.Line;

import java.util.ArrayList;

/**
 *路考_路线管理adapter
 */

public class LukaoChangeAdapter extends RecyclerView.Adapter<LukaoChangeAdapter.ViewHolder> {
    MyItemClickListener myItemClickListener;
    MyItemLongClickListener myItemLongClickListener;
    Context mContent;
    ArrayList<Line> list;
    String[] namelist;

    public LukaoChangeAdapter(Context context, ArrayList<Line> list, String[] namelist) {
        this.mContent=context;
        this.list=list;
        this.namelist=namelist;
    }


    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContent).inflate(R.layout.lukao_line_list_item, parent, false);
        ViewHolder holder = new ViewHolder(view,myItemClickListener,myItemLongClickListener);
        return holder;
    }
    /**
     * 操作
     * */
    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        ArrayList arlist = new ArrayList();
        Line line = list.get(position);
        holder.tv_linenumber.setText("线路"+(position+1)+"：");
        final String mc = line.getMc();
        holder.tv_name.setText(mc);
        String xlzb = line.getXlzb();
        String[] s = xlzb.split(";");
        for(int i=0;i<s.length;i++){
            String s1 = s[i];
            arlist.add(s1);
        }

        holder.bt_delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteDialog(mc,list,position);
            }
        });

    }

    /**
     * 获取控件
     * */
    @Override
    public int getItemCount() {
        if(list!=null){
            return list.size();
        }else {
            return 0;
        }

    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener,View.OnLongClickListener{
        TextView tv_name;
        Button bt_delete;
        TextView tv_linenumber;
        MyItemClickListener myItemClick;
        MyItemLongClickListener myItemLongClick;
        public ViewHolder(View view ,MyItemClickListener myItemClickListener, MyItemLongClickListener myItemLongClickListener) {
            super(view);
            tv_name = (TextView) view.findViewById(R.id.tv_name);//路线名称
            bt_delete = (Button) view.findViewById(R.id.bt_delete);//删除按钮
            tv_linenumber = (TextView) view.findViewById(R.id.tv_linenumber);//线路编号
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
        void onItemClick(View V, int position);
    }

    public interface MyItemLongClickListener{
        void onItemLongClick(View V, int position);
    }

    public void setMyItemClickListener(MyItemClickListener myItemClickListener) {
        this.myItemClickListener = myItemClickListener;
    }

    public void setMyItemLongClickListener(MyItemLongClickListener myItemLongClickListener) {
        this.myItemLongClickListener = myItemLongClickListener;
    }

    /**
     * 删除路线Dialog
     *
     * @param mc
     * @param list
     * @param position*/
    private void deleteDialog(final String mc, final ArrayList<Line> list, final int position) {
        final AlertDialog.Builder dialog = new AlertDialog.Builder(mContent);
        dialog.setTitle("提示");
        dialog.setMessage("确定要删除本条线路吗?");
        dialog.setPositiveButton("确定",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        String[] params={mc};
                        DbHandle.deleteData("line","mc=?",params);
                        list.remove(position);
                        notifyDataSetChanged();
                    }
                });
        dialog.setNegativeButton("取消",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();

                    }
                });
        // 显示
        dialog.show();

    }


}
