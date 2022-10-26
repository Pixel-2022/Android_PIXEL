package org.tensorflow.demo;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class Forgot_pw_Activity extends AppCompatActivity {
    private Button button;
    private EditText edit;
    private String saveemail;

    public static String BASE_URL = "http://ec2-3-36-61-222.ap-northeast-2.compute.amazonaws.com:3001";
    private Retrofit retrofit;
    private RetrofitInterface retrofitInterface;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.forgot);

        edit = findViewById(R.id.input_pwd);
        edit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                saveemail = edit.getText().toString();
            }
        });

        retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        retrofitInterface = retrofit.create(RetrofitInterface.class);

        button = findViewById(R.id.forgotPwdBtn);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HashMap<String, String> map = new HashMap<>();
                map.put("email", saveemail);
                Call<Void> call = retrofitInterface.findpw(map);
                call.enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.code() == 200) {
                            Toast.makeText(Forgot_pw_Activity.this, "이메일로 비밀번호가 발송되었습니다.", Toast.LENGTH_LONG).show();
                        } else if (response.code() == 404) {
                            Toast.makeText(Forgot_pw_Activity.this, "404 오류", Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Toast.makeText(Forgot_pw_Activity.this, t.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }
}
