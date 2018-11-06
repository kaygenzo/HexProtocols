package com.telen.sdk.blemanagersample;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class DevicesBLEAdapter extends RecyclerView.Adapter<DevicesBLEAdapter.DevicesViewHolder> {

    private DeviceInfo[] mDataset;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class DevicesViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public DevicesViewHolder(View v) {
            super(v);
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public DevicesBLEAdapter(DeviceInfo[] myDataset) {
        mDataset = myDataset;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public DevicesBLEAdapter.DevicesViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        DeviceInfo deviceInfo = DeviceInfo.values()[viewType];
        View view;
        switch (deviceInfo) {
            case MINGER:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_adapter_minger, parent, false);
                return new DevicesViewHolder(view);
            case RIBBON:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_adapter_ribbon, parent, false);
                return new DevicesViewHolder(view);
        }
        return null;
    }

    @Override
    public int getItemViewType(int position) {
        return mDataset[position].ordinal();
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(DevicesViewHolder holder, int position) {
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset.length;
    }
}
