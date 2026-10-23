package edu.sv.ues.dam235.parcial2.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;
import edu.sv.ues.dam235.parcial2.R;
import edu.sv.ues.dam235.parcial2.util.SesionManager;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            String uid = new SesionManager(this).getUsuarioActivo();
            Class<?> destino = (uid != null) ? HomeActivity.class : LoginActivity.class;
            startActivity(new Intent(this, destino));
            finish();
        }, 2000);
    }
}