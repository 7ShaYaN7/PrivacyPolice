package be.uhasselt.privacypolice;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.List;
import java.util.Set;

public class ScanResultsChecker extends BroadcastReceiver {
    private static long lastCheck = 0;
    private static Preferences prefs = null;
    private WifiManager wifiManager = null;
    private Context ctx = null;
    private NotificationManager notificationManager = null;

    public void onReceive(Context ctx, Intent i){
        this.ctx = ctx;
        // Older devices might try to scan constantly. Allow them some rest by checking max. once every 0.5 seconds
        if (System.currentTimeMillis() - lastCheck < 500)
            return;
        lastCheck = System.currentTimeMillis();

        // WiFi scan performed
        wifiManager =  (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
        prefs = new Preferences(ctx);
        notificationManager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);

        if (!(prefs.getEnableOnlyAvailableNetworks() || prefs.getOnlyConnectToKnownAccessPoints()))
            // Nothing to do
            return;

        // Disable previous notifications
        notificationManager.cancelAll();

        List<ScanResult> scanResults = wifiManager.getScanResults();
        Log.d("WiFiPolice", "Wi-Fi scan performed, results are: " + scanResults.toString());

        List<WifiConfiguration> networkList = wifiManager.getConfiguredNetworks();
        for (WifiConfiguration network : networkList) {
            if (isSafe(network, scanResults)) {
                Log.i("WiFiPolice", "Enabling " + network.SSID);
                wifiManager.enableNetwork(network.networkId, false); // Do not disable other networks, as multiple networks may be available
            } else
                wifiManager.disableNetwork(network.networkId); // Make sure all other networks are disabled
        }
    }

    private boolean isSafe(WifiConfiguration network, List<ScanResult> scanResults) {
        String plainSSID = network.SSID.substring(1, network.SSID.length() - 1); // Strip "s

        for (ScanResult scanResult : scanResults) {
            if (scanResult.SSID.equals(plainSSID)) {
                if (!prefs.getOnlyConnectToKnownAccessPoints()) { // Any MAC address is fair game
                    // Enabling now makes sure that we only want to connect when it is in range
                    return true;
                } else { // Check access point's MAC address
                    Set<String> allowedBSSIDs = prefs.getAllowedBSSIDs(scanResult.SSID);
                    if (allowedBSSIDs.contains(scanResult.BSSID)) {
                        return true;
                    } else {
                        // Not an allowed BSSID
                        if (prefs.getBlockedBSSIDs().contains(scanResult.BSSID))
                            Log.w("WiFiPolice", "Spoofed network for " + scanResult.SSID + " detected! (BSSID is " + scanResult.BSSID + ")");
                        else
                            // Allow the user to add it to the whitelist
                            askNetworkPermission(scanResult.SSID, scanResult.BSSID);
                        // Block temporarily
                        return false;
                    }
                }
            }
        }
        return false; // Network not in range
    }

    public void askNetworkPermission(String SSID, String BSSID) {
        Log.d("WiFiPolice", "Asking permission for " + SSID + " (" + BSSID + ")");
        Intent addIntent = new Intent(ctx, PermissionChangeReceiver.class);
        addIntent.putExtra("SSID", SSID).putExtra("BSSID", BSSID).putExtra("enable", true);
        PendingIntent addPendingIntent = PendingIntent.getBroadcast(ctx, 0, addIntent, PendingIntent.FLAG_ONE_SHOT);

        Intent disableIntent = new Intent(ctx, PermissionChangeReceiver.class);
        disableIntent.putExtra("SSID", SSID).putExtra("BSSID", BSSID).putExtra("enable", false);
        PendingIntent disablePendingIntent = PendingIntent.getBroadcast(ctx, 1, disableIntent, PendingIntent.FLAG_ONE_SHOT);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(ctx)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle('"' + SSID + "\" encountered")
                .setContentText("Are you sure this network should be available right now?")
                .addAction(android.R.drawable.ic_input_add, "Yes", addPendingIntent)
                .addAction(android.R.drawable.ic_delete, "No", disablePendingIntent);
        notificationManager.notify(0, mBuilder.build());
    }
}