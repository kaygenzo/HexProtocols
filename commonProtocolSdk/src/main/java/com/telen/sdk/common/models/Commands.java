package com.telen.sdk.common.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Commands {
    @SerializedName("commands")
    @Expose
    private List<Command> commands;

    @SerializedName("deviceNames")
    @Expose
    private String[] deviceNames;
}
