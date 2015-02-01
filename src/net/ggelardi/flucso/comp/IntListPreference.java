package net.ggelardi.flucso.comp;

import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;

public class IntListPreference extends ListPreference {

	public IntListPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public IntListPreference(Context context) {
		super(context);
	}
	
	@Override
	protected boolean persistString(String value) {
		return value != null && persistInt(Integer.valueOf(value));
	}
	
	@Override
	protected String getPersistedString(String defaultReturnValue) {
		if (getSharedPreferences().contains(getKey()))
			return String.valueOf(getPersistedInt(0));
		return defaultReturnValue;
	}
}