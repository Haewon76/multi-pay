package com.mallowlink.payment.config;

import feign.Feign;
import feign.Logger;
import feign.okhttp.OkHttpClient;


public class FeignClientConfig {

    public static Feign.Builder FeignBuilder() {
        // okhttp3.OkHttpClient okHttpClient = new okhttp3.OkHttpClient.Builder()
        //         .connectTimeout(10, TimeUnit.SECONDS)
        //         .readTimeout(30, TimeUnit.SECONDS)
        //         .writeTimeout(10, TimeUnit.SECONDS)
        //         .build();

        // FETCH를 위해 OkHttpClient를 사용
        return Feign.builder()
                .logLevel(Logger.Level.FULL)
                .client(new OkHttpClient());
    }

}
