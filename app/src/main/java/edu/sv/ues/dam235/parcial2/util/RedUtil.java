package edu.sv.ues.dam235.parcial2.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

public class RedUtil {
    public static boolean hayInternet(Context ctx) {
        ConnectivityManager cm =
                (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        Network n = cm.getActiveNetwork();
        if (n == null) return false;
        NetworkCapabilities nc = cm.getNetworkCapabilities(n);
        return nc != null && nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }
}