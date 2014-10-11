package net.esorciccio.flucso;

import net.esorciccio.flucso.FFAPI.FeedList.SectionItem;
import net.esorciccio.flucso.SubscrAllAdapter.Scope;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.Toast;

public class SearchActivity extends BaseActivity {
	
	private SubscrAllAdapter feeds;
	private TabHost tabs;
	private EditText edtFWhat;
	private Spinner spFWhere;
	private ListView lvFFeeds;
	private RadioButton rbPScop1;
	private RadioButton rbPScop2;
	private RadioButton rbPScop3;
	private RadioButton rbPScop4;
	private RadioButton rbPScop5;
	private Spinner spPLists;
	private EditText edtPUser;
	private EditText edtPRoom;
	private EditText edtPWhat;
	private EditText edtPTitl;
	private EditText edtPComm;
	private EditText edtPCmBy;
	private EditText edtPLkBy;
	private EditText edtPMinC;
	private EditText edtPMinL;
	
	private OnCheckedChangeListener rbListener;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_search);
		
		feeds = new SubscrAllAdapter(this);
		
		tabs = (TabHost) findViewById(android.R.id.tabhost);
		edtFWhat = (EditText) findViewById(R.id.edt_srcf_what);
		spFWhere = (Spinner) findViewById(R.id.sp_srcf_scope);
		lvFFeeds = (ListView) findViewById(R.id.lv_srcf_feeds);
		rbPScop1 = (RadioButton) findViewById(R.id.rb_srcp_scp1);
		rbPScop2 = (RadioButton) findViewById(R.id.rb_srcp_scp2);
		rbPScop3 = (RadioButton) findViewById(R.id.rb_srcp_scp3);
		rbPScop4 = (RadioButton) findViewById(R.id.rb_srcp_scp4);
		rbPScop5 = (RadioButton) findViewById(R.id.rb_srcp_scp5);
		spPLists = (Spinner) findViewById(R.id.sp_srcp_lists);
		edtPUser = (EditText) findViewById(R.id.edt_srcp_user);
		edtPRoom = (EditText) findViewById(R.id.edt_srcp_room);
		edtPWhat = (EditText) findViewById(R.id.edt_srcp_what);
		edtPTitl = (EditText) findViewById(R.id.edt_srcp_title);
		edtPComm = (EditText) findViewById(R.id.edt_srcp_comm);
		edtPCmBy = (EditText) findViewById(R.id.edt_srcp_comby);
		edtPLkBy = (EditText) findViewById(R.id.edt_srcp_likby);
		edtPMinC = (EditText) findViewById(R.id.edt_srcp_minc);
		edtPMinL = (EditText) findViewById(R.id.edt_srcp_minl);
		
		// setup
		
		tabs.setup();
		tabs.addTab(tabs.newTabSpec("tabF").setContent(R.id.ll_srcf_tab).setIndicator(getString(R.string.search_tabf),
			getResources().getDrawable(R.drawable.ic_action_group)));
		tabs.addTab(tabs.newTabSpec("tabP").setContent(R.id.sv_srcp_tab).setIndicator(getString(R.string.search_tabp),
			getResources().getDrawable(R.drawable.ic_action_storage)));
		
		lvFFeeds.setAdapter(feeds);
		lvFFeeds.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Intent result = new Intent();
				result.putExtra("feed", feeds.getItem(position).id);
				result.putExtra("name", feeds.getItem(position).name);
				setResult(RESULT_OK, result);
				finish();
			}
		});
		
		edtFWhat.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				feeds.getFilter().filter(s);
			}
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}
			@Override
			public void afterTextChanged(Editable s) {
			}
		});
		
		spFWhere.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				feeds.setScope(Scope.fromValue(position)).getFilter().filter(edtFWhat.getText().toString());
			}
			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});
		
		final RadioButton[] scopes = new RadioButton[] { rbPScop1, rbPScop2, rbPScop3, rbPScop4, rbPScop5 };
		rbListener = new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked)
					for (RadioButton rb : scopes)
						if (rb.getId() != buttonView.getId())
							rb.setChecked(false);
			}
		};
		
		for (RadioButton rb : scopes) {
			rb.setOnCheckedChangeListener(rbListener);
		}
		
		if (session.navigation.lists == null || session.navigation.lists.length <= 0) {
			rbPScop3.setEnabled(false);
			spPLists.setVisibility(View.GONE);
		} else {
			ArrayAdapter<String> spData = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
			for (SectionItem si : session.navigation.lists)
				spData.add(si.name);
			spData.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			spPLists.setAdapter(spData);
		}
		
		Button btnprun = (Button) findViewById(R.id.btn_srcp_exec);
		btnprun.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				String query = edtPWhat.getText().toString();
				String tmp = edtPTitl.getText().toString();
				if (!TextUtils.isEmpty(tmp))
					query += "+intitle:" + tmp;
				tmp = edtPComm.getText().toString();
				if (!TextUtils.isEmpty(tmp))
					query += "+incomment:" + tmp;
				tmp = edtPCmBy.getText().toString();
				if (!TextUtils.isEmpty(tmp))
					query += "+comment:" + tmp;
				tmp = edtPLkBy.getText().toString();
				if (!TextUtils.isEmpty(tmp))
					query += "+like:" + tmp;
				tmp = edtPMinC.getText().toString();
				if (!TextUtils.isEmpty(tmp))
					query += "+comments:" + tmp;
				tmp = edtPMinL.getText().toString();
				if (!TextUtils.isEmpty(tmp))
					query += "+likes:" + tmp;
				if (rbPScop2.isChecked())
					query += "+friends:" + session.getUsername();
				else if (rbPScop3.isChecked())
					query += "+list:" + spPLists.getSelectedItem().toString();
				else if (rbPScop4.isChecked()) {
					tmp = edtPUser.getText().toString();
					if (TextUtils.isEmpty(tmp)) {
						Toast.makeText(getApplicationContext(), getString(R.string.search_error), Toast.LENGTH_LONG).show();
						return;
					}
					query += "+from:" + tmp;
				} else if (rbPScop5.isChecked()) {
					tmp = edtPRoom.getText().toString();
					if (TextUtils.isEmpty(tmp)) {
						Toast.makeText(getApplicationContext(), getString(R.string.search_error), Toast.LENGTH_LONG).show();
						return;
					}
					query += "+group:" + tmp;
				}
				Intent result = new Intent();
				result.putExtra("query", query);
				setResult(RESULT_OK, result);
				finish();
			}
		});
	}
	
	@Override
	protected void profileReady() {
		// nothing here
	}
}