package com.telen.ble.blemanagersample.pending;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.telen.ble.sdk.utils.BytesUtils;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

public class DeviceCommand {
    @SerializedName("AppSys")
    @Expose
    private String appSys;
    @SerializedName("Timestamp")
    @Expose
    private long timestamp;
    @SerializedName("AppVer")
    @Expose
    private String appVer;
    @SerializedName("CheckCode")
    @Expose
    private String checkCode;
    @SerializedName("DevicesCMDs")
    @Expose
    private List<DevicesCMD> devicesCMDs = null;

    public String getAppSys() {
        return appSys;
    }

    public void setAppSys(String appSys) {
        this.appSys = appSys;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getAppVer() {
        return appVer;
    }

    public void setAppVer(String appVer) {
        this.appVer = appVer;
    }

    public String getCheckCode() {
        return checkCode;
    }

    public void setCheckCode(String checkCode) {
        this.checkCode = checkCode;
    }

    public List<DevicesCMD> getDevicesCMDs() {
        return devicesCMDs;
    }

    public void setDevicesCMDs(List<DevicesCMD> devicesCMDs) {
        this.devicesCMDs = devicesCMDs;
    }

    public static class Builder {
        private DeviceCommand instance;

        public Builder() {
            instance = new DeviceCommand();
        }

        public Builder setAppSys(String appSys) {
            this.instance.setAppSys(appSys);
            return this;
        }

        public Builder setTimestamp(long timestamp) {
            this.instance.setTimestamp(timestamp);
            return this;
        }

        public Builder setAppVer(String appVer) {
            this.instance.setAppVer(appVer);
            return this;
        }

        public Builder setDevicesCMDs(List<DevicesCMD> devicesCMDs) {
            this.instance.setDevicesCMDs(devicesCMDs);
            return this;
        }

        public DeviceCommand build() throws IllegalArgumentException {
            if(instance.getTimestamp()<=0)
                throw new IllegalArgumentException("Timestamp cannot be less or equal to 0");
            String checkCode = getCheckCode();
            instance.setCheckCode(checkCode);
            return instance;
        }

        private String getCheckCode() {
            StringBuilder builder = new StringBuilder();

            builder.append(MagicHueService.CDPID);
            builder.append(instance.getTimestamp());
            String data = builder.toString();

            byte[] secretKeyBytes = "0FC154F9C01DFA9656524A0EFABC994F".getBytes();
            Key localKey = new SecretKeySpec(secretKeyBytes, "AES");

            Security.addProvider(new BouncyCastleProvider());
            try {
                Cipher cypher = Cipher.getInstance("AES/ECB/PKCS7Padding", BouncyCastleProvider.PROVIDER_NAME);
                cypher.init(Cipher.ENCRYPT_MODE, localKey);
                byte[] finalResult = cypher.doFinal(data.getBytes());
                return BytesUtils.toString(finalResult);
            } catch (NoSuchAlgorithmException | NoSuchProviderException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
                e.printStackTrace();
                return "";
            }
        }
    }
}
