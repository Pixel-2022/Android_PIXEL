package org.tensorflow.demo;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
public class RetrofitClient {
    private Retrofit retrofit;
    public void generateClient(){

        final OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .readTimeout(6000, TimeUnit.SECONDS)
                .connectTimeout(6000, TimeUnit.SECONDS)
                .build();
        retrofit = new Retrofit.Builder()
//                ⚠[주의!] 자신의 ip 주소로 변경할 것!
                .baseUrl("http://ec2-15-165-160-135.ap-northeast-2.compute.amazonaws.com:5000/") //Make sure to include your local machine or server ip/url
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build();
    }
    public RetrofitInterface getApi(){
        return retrofit.create(RetrofitInterface.class);
    }
}