package net.ggelardi.flucso;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import net.ggelardi.flucso.R;

public class SettingsFragment extends PreferenceFragment {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.preferences);
	}
}