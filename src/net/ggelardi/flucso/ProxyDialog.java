package net.ggelardi.flucso;

import net.ggelardi.flucso.serv.Commons.PK;
import net.ggelardi.flucso.serv.FFSession;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.DialogPreference;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

public class ProxyDialog extends DialogPreference {
	
	private FFSession session;
	private CheckBox mUsed;
	private EditText mHost;
	private EditText mPort;
	
	public ProxyDialog(Context context, AttributeSet attrs) {
		super(context, attrs);
		setDialogLayoutResource(R.layout.dialog_proxy);
		session = FFSession.getInstance(context);
	}
	
	@Override
	protected View onCreateDialogView() {
		View view = super.onCreateDialogView();
		
		mUsed = (CheckBox) view.findViewById(R.id.chk_proxy_used);
		mHost = (EditText) view.findViewById(R.id.txt_proxy_host);
		mPort = (EditText) view.findViewById(R.id.txt_proxy_port);
		
		return view;
	}
	
	@Override
	protected void onBindDialogView(View view) {
		super.onBindDialogView(view);

		mUsed.setChecked(session.getPrefs().getBoolean(PK.PROXY_USED, false));
		mHost.setText(session.getPrefs().getString(PK.PROXY_HOST, ""));
		mPort.setText(session.getPrefs().getString(PK.PROXY_PORT, ""));
	}
	
	@Override
	protected void onDialogClosed(boolean positiveResult) {
		super.onDialogClosed(positiveResult);
		
		if (positiveResult) {
			String host = mHost.getText().toString();
			String port = mPort.getText().toString();
			boolean check = !(TextUtils.isEmpty(host) || TextUtils.isEmpty(port));
			SharedPreferences.Editor editor = session.getPrefs().edit();
			editor.putString(PK.PROXY_HOST, host);
			editor.putString(PK.PROXY_PORT, port);
			editor.putBoolean(PK.PROXY_USED, mUsed.isChecked() && check);
			editor.commit();
			if (mUsed.isChecked() && !check)
				Toast.makeText(getContext(), R.string.wrong_proxy, Toast.LENGTH_SHORT).show();
		}
	}
}