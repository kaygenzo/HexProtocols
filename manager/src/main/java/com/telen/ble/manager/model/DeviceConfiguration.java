package com.telen.ble.manager.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class DeviceConfiguration {
    @SerializedName("commands")
    @Expose
    private List<Command> commands;

    @SerializedName("deviceNames")
    @Expose
    private String[] deviceNames;

    public String[] getDeviceNames() {
        return deviceNames;
    }

    public Command getCommand(String commandName) {
        for (Command command:commands) {
            if(commandName.equals(command.getIdentifier()))
                return command;
        }
        return null;
    }
}
