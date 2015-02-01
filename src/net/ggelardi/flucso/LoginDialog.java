package net.ggelardi.flucso;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.preference.DialogPreference;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import net.ggelardi.flucso.R;
import net.ggelardi.flucso.serv.FFSession;

public class LoginDialog extends DialogPreference {
	
	private FFSession session;
	private EditText mUsername;
	private EditText mPassword;
	
	public LoginDialog(Context context, AttributeSet attrs) {
		super(context, attrs);
		setDialogLayoutResource(R.layout.dialog_login);
		session = FFSession.getInstance(context);
	}
	
	@Override
	protected View onCreateDialogView() {
		View view = super.onCreateDialogView();
		
		mUsername = (EditText) view.findViewById(R.id.txt_login_username);
		mPassword = (EditText) view.findViewById(R.id.txt_login_remotekey);
		
		CheckBox mShowRKey = (CheckBox) view.findViewById(R.id.chk_login_showremkey);
		mShowRKey.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					mPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
				} else {
					mPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
				}
			}
		});
		
		Button mGoToRKey = (Button) view.findViewById(R.id.btn_login_gotoremkey);
		mGoToRKey.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent browse = new Intent(Intent.ACTION_VIEW, Uri.parse("http://friendfeed.com/remotekey"));
				v.getContext().startActivity(browse);
			}
		});
		
		return view;
	}
	
	@Override
	protected void onBindDialogView(View view) {
		super.onBindDialogView(view);
		
		mUsername.setText(session.getUsername());
		mPassword.setText(session.getRemoteKey());
	}
	
	@Override
	protected void onDialogClosed(boolean positiveResult) {
		super.onDialogClosed(positiveResult);
		
		if (positiveResult)
			session.saveAccount(mUsername.getText().toString(), mPassword.getText().toString());
	}
}