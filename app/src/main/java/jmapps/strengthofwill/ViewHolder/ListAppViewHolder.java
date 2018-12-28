package jmapps.strengthofwill.ViewHolder;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import jmapps.strengthofwill.R;

public class ListAppViewHolder extends RecyclerView.ViewHolder {

    public final ImageView ivIconApp;
    public final TextView tvNameApp;

    public ListAppViewHolder(@NonNull View listApp) {
        super(listApp);

        ivIconApp = listApp.findViewById(R.id.iv_icon_app);
        tvNameApp = listApp.findViewById(R.id.tv_name_app);
    }
}