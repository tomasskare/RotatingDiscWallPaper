package org.nocrew.tomas.discwp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.ListPreference;

public class DiscWallpaperSettings extends PreferenceActivity
    implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

	getPreferenceManager().setSharedPreferencesName("prefs");
	addPreferencesFromResource(R.xml.discwp_settings);
        SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
	prefs.registerOnSharedPreferenceChangeListener(this);
	onSharedPreferenceChanged(prefs, null);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        getPreferenceManager().getSharedPreferences().
	    unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
					  String key) {
	ListPreference list;
	list = (ListPreference)findPreference("pref_disc_type");
        list.setSummary(list.getEntry());
    }
}
