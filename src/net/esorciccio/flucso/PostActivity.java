package net.esorciccio.flucso;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import net.esorciccio.flucso.FFAPI.BaseFeed;
import net.esorciccio.flucso.FFAPI.Entry;
import net.esorciccio.flucso.PostFileAdapter.ImageRef;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.mime.MultipartTypedOutput;
import retrofit.mime.TypedFile;
import retrofit.mime.TypedString;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.MimeTypeMap;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class PostActivity extends BaseActivity implements OnClickListener {
	private static final int BODYCHARS = 350;
	private static final int REQ_SELECT_FILE = 1;
	private static final int REQ_SNAP_PHOTO = 10;
	
	private String eid;
	private String link;
	private String[] dsts;
	private String body;
	private String comm;
	private String[] tmbs;
	private Uri[] uris;
	
	private Uri snapFileUri;
	
	private PostDSelAdapter aDsts;
	private PostThmbAdapter aTmbs;
	private PostFileAdapter aImgs;
	
	private ScrollView svMain;
	private LinearLayout lDsts;
	private LinearLayout lAtts;
	private LinearLayout lComm;
	private WebView wScreen;
	private TextView txtLink;
	private TextView txtToNo;
	private TextView txtAtNo;
	private TextView txtChNo;
	private EditText edtBody;
	private EditText edtComm;
	private MenuItem miPost;
	private MenuItem miFile;
	private MenuItem miSnap;
	private MenuItem miHome;
	private MenuItem miWImg;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_post);
		
		aDsts = new PostDSelAdapter(this);
		aTmbs = new PostThmbAdapter(this);
		aImgs = new PostFileAdapter(this);
		
		svMain = (ScrollView) findViewById(R.id.sv_post_main);
		lDsts = (LinearLayout) findViewById(R.id.l_post_sez_dsts);
		lAtts = (LinearLayout) findViewById(R.id.l_post_sez_atts);
		lComm = (LinearLayout) findViewById(R.id.l_post_sez_comm);
		txtLink = (TextView) findViewById(R.id.txt_post_link);
		txtToNo = (TextView) findViewById(R.id.txt_post_dsts_count);
		txtAtNo = (TextView) findViewById(R.id.txt_post_atts_count);
		txtChNo = (TextView) findViewById(R.id.txt_post_body_count);
		edtBody = (EditText) findViewById(R.id.edt_post_body);
		edtComm = (EditText) findViewById(R.id.edt_post_comm);
		wScreen = (WebView) findViewById(R.id.wv_post_screen);
		
		lDsts.setVisibility(View.GONE);
		lAtts.setVisibility(View.GONE);
		lComm.setVisibility(View.GONE);
		txtLink.setVisibility(View.GONE);
		edtComm.setText("");
		
		WebSettings webSettings = wScreen.getSettings();
		webSettings.setAllowContentAccess(false);
		webSettings.setGeolocationEnabled(false);
		webSettings.setJavaScriptEnabled(false);
		webSettings.setSaveFormData(false);
		
		eid = "";
		link = "";
		dsts = new String[] {};
		body = savedInstanceState != null ? savedInstanceState.getString("body", "") : "";
		comm = savedInstanceState != null ? savedInstanceState.getString("comment", "") : "";
		tmbs = new String[] {};
		uris = new Uri[] {};
		
		Intent intent = getIntent();
		String action = intent.getAction();
		if (action.equals(Intent.ACTION_INSERT)) {
			Bundle params = intent.getExtras();
			link = params.getString("link", "");
			body = params.getString("body", body);
			dsts = params.getStringArray("dsts");
			tmbs = params.getStringArray("tmbs");
		} else if (action.equals(Intent.ACTION_EDIT)) {
			Bundle params = intent.getExtras();
			eid = params.getString("eid");
			body = params.getString("body", body);
		} else if (action.equals(Intent.ACTION_SEND)) {
			String typ = intent.getType();
			if (typ.equals("text/plain")) {
				String txt = intent.getStringExtra(Intent.EXTRA_TEXT).trim();
				String sub = intent.getStringExtra(Intent.EXTRA_SUBJECT);
				if (Patterns.WEB_URL.matcher(txt).matches()) {
					link = Commons.convertYoutubeLinks(txt);
					if (!TextUtils.isEmpty(sub))
						body = sub;
				} else {
					String[] chk = txt.split("\\s+");
					for (String s : chk)
						if (Patterns.WEB_URL.matcher(s).matches())
							link = Commons.convertYoutubeLinks(s);
					body = TextUtils.isEmpty(link) ? txt : txt.replace(link, "");
				}
			} else if (typ.startsWith("image/")) {
				uris = new Uri[] { (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM) };
			}
		} else if (action.equals(Intent.ACTION_SEND_MULTIPLE)) {
			ArrayList<Uri> imgs = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
			uris = new Uri[imgs.size()];
			for (int i=0; i<imgs.size(); i++)
				uris[i] = imgs.get(i);
		}
		
		ExpandableHeightGridView gvDsts = (ExpandableHeightGridView) findViewById(R.id.gv_post_dsts);
		gvDsts.setExpanded(true);
		gvDsts.setAdapter(aDsts);
		gvDsts.setOnScrollListener(new OnScrollListener() {
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
			}
			@Override
			public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
				txtToNo.setText(Integer.toString(aDsts.getCount()));
				checkMenu();
				checkDM();
			}
		});
		
		final AutoCompleteTextView macDsts = (AutoCompleteTextView) findViewById(R.id.edt_post_actv);
		macDsts.setThreshold(1);
		macDsts.setAdapter(new PostDSrcAdapter(this));
		macDsts.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				BaseFeed item = (BaseFeed) parent.getAdapter().getItem(position);
				aDsts.append(item);
				macDsts.setText("");
			}
		});
		
		ExpandableHeightGridView gvAtts = (ExpandableHeightGridView) findViewById(R.id.gv_post_atts);
		gvAtts.setExpanded(true);
		gvAtts.setAdapter(aImgs);
		gvAtts.setOnScrollListener(new OnScrollListener() {
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
			}
			@Override
			public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
				txtAtNo.setText(Integer.toString(aImgs.getCount()));
				checkMenu();
			}
		});
		
		edtBody.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}
			@Override
			public void afterTextChanged(Editable s) {
				int n = s.length();
				if (n >= BODYCHARS) {
					String txt = s.toString();
					int before = txt.lastIndexOf(' ', BODYCHARS);
					int after = txt.indexOf(' ', BODYCHARS + 1);
					int middle = before == -1 || after != -1 && BODYCHARS - before >= after - BODYCHARS ? after : before;
					s.delete(middle, s.length());
					if (s.length() <= BODYCHARS - 3)
						s.append("...");
					n = s.length();
					edtComm.append(String.valueOf("... " + txt.substring(middle + 1)));
					edtComm.requestFocus();
				}
				txtChNo.setText(Integer.toString(BODYCHARS - n));
				checkMenu();
			}
		});
		
		// I've moved these here from profileReady, otherwise switching fast to home and back would clear both
		// (the activity steps in the onRestoreInstanceState only if the system destroy it)
		txtChNo.setText(Integer.toString(BODYCHARS));
		edtBody.setText(body);
		edtComm.setText(comm);
	}
	
	@Override
	protected void profileReady() {
		if (!isNew())
			setTitle(R.string.post_title_edit);
		else {
			setTitle(R.string.post_title_new);
			if (!TextUtils.isEmpty(link)) {
				txtLink.setText(link);
				txtLink.setVisibility(View.VISIBLE);
			}
			lDsts.setVisibility(View.VISIBLE);
			BaseFeed to;
			for (String s : dsts)
				if (session.profile.isIt(s))
					aDsts.append(session.profile);
				else {
					to = session.profile.findFeedById(s);
					if (to != null && (to.isGroup() && to.canPost() || to.isUser() && to.canDM()))
						aDsts.append(to);
				}
			if (aDsts.getCount() <= 0)
				aDsts.append(session.profile);
			lComm.setVisibility(View.VISIBLE);
		}
		if (tmbs != null)
			for (String s: tmbs)
				aTmbs.append(s);
		if (uris != null)
			for (Uri u: uris)
				attachImage(u, false);
		txtToNo.setText(Integer.toString(aDsts.getCount()));
		txtAtNo.setText(Integer.toString(0));
		/*
		txtChNo.setText(Integer.toString(BODYCHARS));
		edtBody.setText(body);
		edtComm.setText(comm);
		*/
	}
	
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);

		body = savedInstanceState.getString("body", "");
		comm = savedInstanceState.getString("comment", "");
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		String text = edtBody.getText().toString();
		if (!TextUtils.isEmpty(text))
			outState.putString("body", text);
		
		text = edtComm.getText().toString();
		if (!TextUtils.isEmpty(text))
			outState.putString("comment", text);
		
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.post, menu);
		miPost = menu.findItem(R.id.action_post_send);
		miFile = menu.findItem(R.id.action_post_file);
		miSnap = menu.findItem(R.id.action_post_snap);
		miHome = menu.findItem(R.id.action_post_home);
		miWImg = menu.findItem(R.id.action_post_wimg);
		return true;
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		miSnap.setVisible(getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA));
		miWImg.setVisible(!TextUtils.isEmpty(link));
		checkMenu();
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.equals(miPost)) {
			sendPost();
			return true;
		}
		if (item.equals(miFile)) {
			Intent intent = new Intent();
			intent.setType("image/*");
			intent.setAction(Intent.ACTION_GET_CONTENT);
			intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
			startActivityForResult(Intent.createChooser(intent, "Complete action using"), REQ_SELECT_FILE);
			return true;
		}
		if (item.equals(miSnap)) {
			Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			// Ensure that there's a camera activity to handle the intent
			if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
				File photoFile = createImageFile();
				if (photoFile != null) {
					snapFileUri = Uri.fromFile(photoFile);
					takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, snapFileUri);
					startActivityForResult(takePictureIntent, REQ_SNAP_PHOTO);
				}
			}
			return true;
		}
		if (item.equals(miHome)) {
			aDsts.append(session.profile);
			return true;
		}
		if (item.equals(miWImg)) {
			setProgressBarIndeterminateVisibility(true);
			wScreen.setVisibility(View.VISIBLE);
			wScreen.loadUrl(link);
			wScreen.setWebViewClient(new WebViewClient() {
				@Override
				public void onPageFinished(WebView view, String url) {
					try {
						File screenFile = createImageFile();
						if (screenFile != null) {
							Bitmap screenShot;
							view.setDrawingCacheEnabled(true);
							screenShot = Bitmap.createBitmap(view.getDrawingCache());
							view.setDrawingCacheEnabled(false);
							try {
								FileOutputStream fos = new FileOutputStream(screenFile);
								screenShot.compress(Bitmap.CompressFormat.PNG, 90, fos);
								fos.close();
							} catch (Exception e) {
								Toast.makeText(PostActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
								return;
							}
							Uri screenUri = Uri.fromFile(screenFile);
							attachImage(screenUri, true);
						}
						wScreen.setWebViewClient(null);
						view.loadUrl("about:blank");
					} finally {
						view.setVisibility(View.GONE);
						setProgressBarIndeterminateVisibility(false);
					}
				}
			});
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		
		if (requestCode == REQ_SELECT_FILE && resultCode == RESULT_OK)
			attachImage(intent.getData(), false);
		else if (requestCode == REQ_SNAP_PHOTO && resultCode == RESULT_OK)
			attachImage(snapFileUri, true);
	}
	
	@Override
	public void onClick(View v) {
		int pos;
		try {
			pos = (Integer) v.getTag();
		} catch (Exception err) {
			return; // wtf?
		}
		if (v.getId() == R.id.img_post_dsel_del)
			aDsts.remove(pos);
		else if (v.getId() == R.id.img_post_file_del) {
			aImgs.remove(pos);
			if (aImgs.getCount() <= 0)
				lAtts.setVisibility(View.GONE);
		}
	}
	
	private boolean isNew() {
		return TextUtils.isEmpty(eid);
	}
	
	private void checkMenu() {
		if (miPost != null)
			miPost.setVisible((!isNew() || aDsts.getCount() > 0) && !TextUtils.isEmpty(edtBody.getText().toString()));
		if (miFile != null)
			miFile.setVisible(isNew());
		if (miSnap != null)
			miSnap.setVisible(isNew());
	}
	
	private void checkDM() {
		boolean dm = true;
		boolean me = false;
		BaseFeed f;
		for (int i = 0; i < aDsts.getCount() && dm; i++) {
			f = aDsts.getItem(i);
			if (!f.isUser() || f.isIt(session.getUsername()))
				dm = false;
			if (!me && f.isIt(session.getUsername()))
				me = true;
		}
		if (miHome != null)
			miHome.setVisible(!me);
		if (dm)
			svMain.setBackgroundColor(getResources().getColor(R.color.post_bg_private));
		else
			svMain.setBackgroundColor(getResources().getColor(R.color.post_bg_nonpriv));
	}
	
	private File createImageFile() {
		File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
		String fileName = "SNAP_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
		try {
			return File.createTempFile(fileName, ".jpg", storageDir);
		} catch (IOException ex) {
			new AlertDialog.Builder(PostActivity.this).setTitle(R.string.res_imcrea_failed).setMessage(ex.getMessage()).setIcon(
				android.R.drawable.ic_dialog_alert).create().show();
			return null;
		}
	}
	
	private String getDataColumn(Uri uri, String selection, String[] selectionArgs) {
		Cursor cursor = null;
		final String column = "_data";
		final String[] projection = { column };
		try {
			cursor = getContentResolver().query(uri, projection, selection, selectionArgs, null);
			if (cursor != null && cursor.moveToFirst()) {
				final int column_index = cursor.getColumnIndexOrThrow(column);
				return cursor.getString(column_index);
			}
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return null;
	}
	
	@TargetApi(Build.VERSION_CODES.KITKAT)
	private String getFilePath(final Uri uri) {
		// Make sure we're running on KitKat or higher to use the Storage Access Framework
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			if (DocumentsContract.isDocumentUri(this, uri)) {
				if ("com.android.providers.downloads.documents".equals(uri.getAuthority())) {
					// DownloadsProvider
					String id = DocumentsContract.getDocumentId(uri);
					Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"),
						Long.valueOf(id));
					return getDataColumn(contentUri, null, null);
				}
				if ("com.android.providers.media.documents".equals(uri.getAuthority())) {
					// MediaProvider
					String docId = DocumentsContract.getDocumentId(uri);
					String[] split = docId.split(":");
					String type = split[0];
					Uri contentUri = null;
					if ("image".equals(type))
						contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
					else if ("video".equals(type))
						contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
					else if ("audio".equals(type))
						contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
					String selection = "_id=?";
					String[] selectionArgs = new String[] { split[1] };
					return getDataColumn(contentUri, selection, selectionArgs);
				}
				return null; // probably stored in some cloud service...
			}
		}
		if ("file".equalsIgnoreCase(uri.getScheme()))
			return uri.getPath();
		// Older apps (gallery, photo, whatever)
		String[] filePathColumn = { MediaStore.Images.Media.DATA };
		Cursor cursor = getContentResolver().query(uri, filePathColumn, null, null, null);
		cursor.moveToFirst();
		int columnIndex = cursor.getColumnIndexOrThrow(filePathColumn[0]);
		return cursor.getString(columnIndex);
	}
	
	private static String getMimeType(String url) {
		String ext = MimeTypeMap.getFileExtensionFromUrl(url).toLowerCase(Locale.getDefault());
		if (TextUtils.isEmpty(ext) && url.lastIndexOf(".") > 0)
			ext = url.substring((url.lastIndexOf(".") + 1), url.length());
		if (ext != null) {
			MimeTypeMap mime = MimeTypeMap.getSingleton();
			return mime.getMimeTypeFromExtension(ext);
		}
		return null;
	}
	
	private void attachImage(Uri uri, boolean created) {
		ImageRef ir = new ImageRef();
		ir.uri = uri;
		ir.path = getFilePath(uri);
		if (ir.path == null) {
			Log.v("attachImage", ir.uri.toString());
			new AlertDialog.Builder(PostActivity.this).setMessage(R.string.res_impath_failed).setIcon(
				android.R.drawable.ic_dialog_alert).create().show();
			return;
		}
		if (created) {
			Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
			mediaScanIntent.setData(uri);
			sendBroadcast(mediaScanIntent);
		}
		ir.mime = getMimeType(ir.path);
		if (TextUtils.isEmpty(ir.mime)) {
			Log.v("attachImage", ir.path);
			new AlertDialog.Builder(PostActivity.this).setMessage(R.string.res_immime_failed).setIcon(
				android.R.drawable.ic_dialog_alert).create().show();
			return;
		}
		Log.v("attachImage", ir.mime.toString() + " -- " + ir.path.toString());
		if (!aImgs.append(ir)) {
			Toast.makeText(this, R.string.res_imdupe_failed, Toast.LENGTH_LONG).show();
			return;
		}
		lAtts.setVisibility(View.VISIBLE);
	}
	
	private void sendPost() {
		showWaitingBox(R.string.title_posting);
		Callback<Entry> callback = new Callback<Entry>() {
			@Override
			public void success(Entry result, Response response) {
				hideWaitingBox();
				session.cachedEntry = result;
				Intent intent = new Intent();
				intent.putExtra("eid", result.id);
				setResult(RESULT_OK, intent);
				finish();
			}
			@Override
			public void failure(RetrofitError error) {
				hideWaitingBox();
				new AlertDialog.Builder(PostActivity.this).setTitle(R.string.res_rfcall_failed).setMessage(
					Commons.retrofitErrorText(error)).setPositiveButton(R.string.dlg_btn_retry,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							sendPost();
						}
					}).setIcon(android.R.drawable.ic_dialog_alert).setCancelable(true).create().show();
			}
		};
		MultipartTypedOutput mto = new MultipartTypedOutput();
		mto.addPart("body", new TypedString(edtBody.getText().toString()));
		if (isNew()) {
			if (!TextUtils.isEmpty(link)) {
				String chk = link.toLowerCase(Locale.getDefault()).trim();
				if (chk.endsWith(".gif") || chk.endsWith(".jpg") || chk.endsWith(".jpeg") || chk.endsWith(".png"))
					mto.addPart("image_url", new TypedString(link));
				else
					mto.addPart("link", new TypedString(link));
			}
			for (String s : aDsts.getIDs())
				mto.addPart("to", new TypedString(session.profile.isIt(s) ? "me" : s));
			if (!TextUtils.isEmpty(edtComm.getText().toString()))
				mto.addPart("comment", new TypedString(edtComm.getText().toString()));
			for (int i = 0; i < aTmbs.getCount(); i++)
				mto.addPart("image_url", new TypedString(aTmbs.getItem(i)));
			ImageRef ir;
			for (int i = 0; i < aImgs.getCount(); i++) {
				ir = aImgs.getItem(i);
				mto.addPart("file", new TypedFile(ir.mime, new File(ir.path)));
			}
		} else
			mto.addPart("id", new TypedString(eid));
		FFAPI.client_write(session).ins_entry(mto, callback);
	}
}