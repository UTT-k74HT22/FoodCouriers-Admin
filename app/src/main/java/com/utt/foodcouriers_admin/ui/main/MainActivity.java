package com.utt.foodcouriers_admin.ui.main;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.utt.foodcouriers_admin.R;
import com.utt.foodcouriers_admin.ui.auth.LoginActivity;
import com.utt.foodcouriers_admin.utils.SessionManager;

public class MainActivity extends AppCompatActivity {

    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        sessionManager = SessionManager.getInstance(this);
        
        if (!sessionManager.isLoggedIn()) {
            finish();
            return;
        }
        
        setContentView(R.layout.activity_main);

        String loginSuccessMessage = getIntent().getStringExtra(LoginActivity.EXTRA_LOGIN_SUCCESS_MESSAGE);
        if (loginSuccessMessage != null && !loginSuccessMessage.isBlank()) {
            Toast.makeText(this, loginSuccessMessage, Toast.LENGTH_LONG).show();
        }
        
        TextView tvWelcome = findViewById(R.id.tvWelcome);
        String userName = sessionManager.getUserName();
        tvWelcome.setText("Welcome, " + (userName != null ? userName : "Admin"));
    }
}
