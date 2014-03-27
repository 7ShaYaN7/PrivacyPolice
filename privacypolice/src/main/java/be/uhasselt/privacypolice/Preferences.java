package be.uhasselt.privacypolice;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.HashSet;
import java.util.Set;

public class Preferences {
    private SharedPreferences prefs;
    private final String ALLOWED_BSSID_PREFIX = "ABSSID//";

    public Preferences(Context ctx) {
        this.prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        try {
            Log.v("PrivacyPolice", "Current preferences are: " + prefs.getAll().toString());
        } catch (NullPointerException npe) {
            Log.v("PrivacyPolice", "No preferences found!");
        }
    }

    public boolean getEnableOnlyAvailableNetworks() {
        return prefs.getBoolean("enableOnlyAvailableNetworks", true);
    }

    public boolean getOnlyConnectToKnownAccessPoints() {
        return prefs.getBoolean("onlyConnectToKnownAccessPoints", true);
    }

    public Set<String> getAllowedBSSIDs(String SSID) {
        return prefs.getStringSet(ALLOWED_BSSID_PREFIX + SSID, new HashSet<String>());
    }

    public Set<String> getBlockedBSSIDs() {
        return prefs.getStringSet("BlockedSSIDs", new HashSet<String>());
    }

    public void addAllowedBSSID(String SSID, String BSSID) {
        Set<String> currentlyInList = getAllowedBSSIDs(SSID);
        if (currentlyInList.contains(BSSID))
            // Already in the list
            return;

        // Create copy of list, because sharedPreferences only checks whether *reference* is the same
        // In order to add elements, we thus need a new object (otherwise nothing changes)
        Set<String> newList = new HashSet<String>(currentlyInList);
        Log.i("PrivacyPolice", "Adding BSSID: " + BSSID + " for " + SSID);
        newList.add(BSSID);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putStringSet(ALLOWED_BSSID_PREFIX + SSID, newList);
        editor.commit();
    }

    public void addBlockedBSSID(String BSSID) {
        Set<String> currentlyInList = getBlockedBSSIDs();
        if (currentlyInList.contains(BSSID))
            // Already in the list
            return;

        Log.i("PrivacyPolice", "Adding blocked BSSID: " + BSSID);
        // Create copy of list, because sharedPreferences only checks whether *reference* is the same
        // In order to add elements, we thus need a new object (otherwise nothing changes)
        Set<String> newList = new HashSet<String>(currentlyInList);
        newList.add(BSSID);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putStringSet("BlockedSSIDs", newList);
        editor.commit();
    }
}