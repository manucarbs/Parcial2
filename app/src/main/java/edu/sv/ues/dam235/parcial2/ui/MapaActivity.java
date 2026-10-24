package edu.sv.ues.dam235.parcial2.ui;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.appbar.MaterialToolbar;
import edu.sv.ues.dam235.parcial2.R;

public class MapaActivity extends AppCompatActivity implements OnMapReadyCallback {

    // Punto predefinido: CAMBIA estas coordenadas y el nombre por los de tu negocio
    private static final double LATITUD = 13.721161;
    private static final double LONGITUD = -89.728718;
    private static final String NOMBRE = "Sabor Tours";
    private static final String DIRECCION = "Sonsonate";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mapa);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        SupportMapFragment mapa = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapa != null) mapa.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        LatLng punto = new LatLng(LATITUD, LONGITUD);
        googleMap.addMarker(new MarkerOptions()
                        .position(punto)
                        .title(NOMBRE)
                        .snippet(DIRECCION))
                .showInfoWindow();
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(punto, 16f));
        googleMap.getUiSettings().setZoomControlsEnabled(true);
    }
}