package com.telen.ble.blemanagersample.pending;

import java.util.Map;

import io.reactivex.Single;
import retrofit2.http.Body;
import retrofit2.http.HeaderMap;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface MagicHueService {

    String CDPID = "ZG001";

    @Headers({
            "accept-encoding: gzip",
            "connection: Keep-Alive",
            "content-length: 243",
            "content-type: application/json; charset=utf-8",
            "host: wifi.magichue.net",
            "user-agent: okhttp/3.3.1"
    })
    @POST("AB004/PostRequestCommandBatch")
    Single<CommandResponse> lightOn(@HeaderMap Map<String, String> headers, @Query("cdpid") String cdpid, @Body DeviceCommand deviceCommand);

    @Headers({
            "accept-encoding: gzip",
            "connection: Keep-Alive",
            "content-length: 243",
            "content-type: application/json; charset=utf-8",
            "host: wifi.magichue.net",
            "user-agent: okhttp/3.3.1"
    })
    @POST("AB004/PostRequestCommandBatch")
    Single<CommandResponse> lightOff(@HeaderMap Map<String, String> headers, @Query("cdpid") String cdpid, @Body DeviceCommand deviceCommand);

    @Headers({
            "accept-encoding: gzip",
            "connection: Keep-Alive",
            "content-length: 243",
            "content-type: application/json; charset=utf-8",
            "host: wifi.magichue.net",
            "user-agent: okhttp/3.3.1"
    })
    @POST("AB004/PostRequestCommandBatch")
    Single<CommandResponse> changeColor(@HeaderMap Map<String, String> headers, @Query("cdpid") String cdpid, @Body DeviceCommand deviceCommand);
}
