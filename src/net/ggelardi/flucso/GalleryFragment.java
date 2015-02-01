package net.ggelardi.flucso;

import net.ggelardi.flucso.R;
import net.ggelardi.flucso.serv.Commons;
import net.ggelardi.flucso.serv.FFAPI;
import net.ggelardi.flucso.serv.FFAPI.Entry;
import net.ggelardi.flucso.serv.FFAPI.Entry.Attachment;
import net.ggelardi.flucso.serv.FFAPI.Entry.Thumbnail;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.TextView;

public class GalleryFragment extends BaseFragment {
	public static final String FRAGMENT_TAG = "net.ggelardi.flucso.GalleryFragment";
	
	private Entry entry;
	
	private String eid;
	private int position;
	
	private TextView txt;
	private WebView web;
	private MenuItem miPrior;
	private MenuItem miNext;
	private MenuItem miRotL;
	private MenuItem miRotR;
	private MenuItem miRot0;
	private MenuItem miSDir;
	private MenuItem miDwnl;
	
	public static GalleryFragment newInstance(String entry_id, int position) {
		GalleryFragment fragment = new GalleryFragment();
		Bundle args = new Bundle();
		args.putString("eid", entry_id);
		args.putInt("position", position);
		fragment.setArguments(args);
		return fragment;
	}
	
	public GalleryFragment() {
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Bundle args = getArguments();
		eid = args.getString("eid");
		position = args.getInt("position", 0);
		
		setHasOptionsMenu(true);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_gallery, container, false);
		
		txt = (TextView) view.findViewById(R.id.txt_media);
		web = (WebView) view.findViewById(R.id.web_image);
		
		web.setLayerType(WebView.LAYER_TYPE_HARDWARE, null);
		web.setBackgroundColor(Color.TRANSPARENT);
		WebSettings ws = web.getSettings();
		ws.setLoadsImagesAutomatically(true);
		ws.setSupportZoom(true);
		ws.setBuiltInZoomControls(true);
		ws.setUseWideViewPort(true);
		ws.setLoadWithOverviewMode(true);
		
		return view;
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		// Log.v("stack", this.getClass().getName() + ".onResume");
	}
	
	@Override
	public void onPause() {
		super.onPause();
		
		// Log.v("stack", this.getClass().getName() + ".onPause");
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		outState.putInt("position", position);
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.gallery, menu);
		
		miPrior = menu.findItem(R.id.action_previous);
		miNext = menu.findItem(R.id.action_next);
		miRotL = menu.findItem(R.id.action_rotl);
		miRotR = menu.findItem(R.id.action_rotr);
		miRot0 = menu.findItem(R.id.action_rot0);
		miSDir = menu.findItem(R.id.action_sdir);
		miDwnl = menu.findItem(R.id.action_dwnl);
	}
	
	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		checkMenu();
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item == miPrior) {
			setPosition((position + (entry.getFilesCount() - 1)) % entry.getFilesCount());
			return true;
		}
		if (item == miNext) {
			setPosition((position + 1) % entry.getFilesCount());
			return true;
		}
		if (item == miRotL) {
			entry.thumbnails[position - entry.files.length].rotation = -90;
			setPosition(position);
			return true;
		}
		if (item == miRotR) {
			entry.thumbnails[position - entry.files.length].rotation = 90;
			setPosition(position);
			return true;
		}
		if (item == miRot0) {
			entry.thumbnails[position - entry.files.length].rotation = 0;
			setPosition(position);
			return true;
		}
		if (item == miSDir) {
			int n = position - entry.files.length;
			entry.thumbnails[n].landscape = !entry.thumbnails[n].landscape;
			setPosition(position);
			return true;
		}
		if (item == miDwnl) {
			String url;
			String name;
			if (position < entry.files.length) {
				url = entry.files[position].url;
				name = entry.files[position].name;
			} else {
				Thumbnail pic = entry.thumbnails[position - entry.files.length];
				url = pic.isFFMediaPic() || (pic.link.endsWith(".jpg") || pic.link.endsWith(".jpeg") ||
					pic.link.endsWith(".png") || pic.link.endsWith(".gif")) ? pic.link : pic.url;
				name = URLUtil.guessFileName(url, null, null);
				// friendfeed-media.com files don't have extension so URLUtil adds ".bin", but since it's a picture
				// (I'm sure it is) I think it's better to use a more portable extension (jpg).
				if (pic.isFFMediaPic())
					name = name.replace(".bin", ".jpg");
			}
			Log.v(getTag(), "About to download " + url);
			Log.v(getTag(), "With filename: " + name);
			DownloadManager.Request req = new DownloadManager.Request(Uri.parse(url));
			req.setTitle(name);
			req.setDescription("A file from " + entry.from.name);
			req.allowScanningByMediaScanner();
			req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
			req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name);
			DownloadManager manager = (DownloadManager) getActivity().getSystemService(Context.DOWNLOAD_SERVICE);
			manager.enqueue(req);
			return true;
		}
		return false;
	}
	
	@Override
	protected void initFragment() {
		entry = session.cachedEntry;
		if (entry == null || !entry.isIt(eid))
			loadEntry();
		else
			setPosition(position);
	}
	
	private void checkMenu() {
		if (miPrior == null)
			return;
		miPrior.setVisible(entry != null && entry.getFilesCount() > 1);
		miNext.setVisible(miPrior.isVisible());
		// rotation
		Thumbnail pic = entry != null && entry.getFilesCount() > 0 && position >= entry.files.length ?
			entry.thumbnails[position - entry.files.length] : null;
		miRotL.setVisible(pic != null && pic.rotation >= 0);
		miRotR.setVisible(pic != null && pic.rotation <= 0);
		miRot0.setVisible(pic != null && pic.rotation != 0);
		// resize
		miSDir.setVisible(pic != null);
		// download
		miDwnl.setVisible(entry != null);
	}
	
	private void loadEntry() {
		mProgress.setTitle(R.string.waiting_entry);
		mProgress.show();
		Callback<Entry> callback = new Callback<Entry>() {
			@Override
			public void success(Entry result, Response response) {
				mProgress.dismiss();
				entry = result;
				setPosition(position);
			}
			@Override
			public void failure(RetrofitError error) {
				mProgress.dismiss();
				new AlertDialog.Builder(getActivity()).setTitle(R.string.res_rfcall_failed).setMessage(
					Commons.retrofitErrorText(error)).setOnDismissListener(new OnDismissListener() {
						@Override
						public void onDismiss(DialogInterface dialog) {
							getActivity().getFragmentManager().popBackStack();
						}
					}).setPositiveButton(
					R.string.dlg_btn_retry, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							loadEntry();
						}
					}).setIcon(android.R.drawable.ic_dialog_alert).setCancelable(true).create().show();
			}
		};
		FFAPI.client_entry(session).get_entry_async(eid, callback);
	}
	
	private void setPosition(int value) {
		Log.v("gallery", "setPosition: " + Integer.toString(value));
		position = value;
		if (position < entry.files.length)
			showFile(entry.files[position]);
		else
			showThmb(entry.thumbnails[position - entry.files.length]);
		checkMenu();
	}
	
	private void showFile(Attachment att) {
		Log.v("gallery", "showFile: " + att.url);
		/*
		if (att.type.toLowerCase(Locale.getDefault()).startsWith("audio") && att.size > 0) {
			
			killPlayer();
			
			web.setVisibility(View.GONE);
			txt.setVisibility(View.VISIBLE);
			
			txt.setText(att.name);
			
			player = new MediaPlayer();
			try {
				player.setDataSource(getActivity(), Uri.parse(att.url));
				player.prepare();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			control = new MediaController(getActivity());
			
			control.show(0);
			
		} else {
		*/
			txt.setVisibility(View.GONE);
			web.setVisibility(View.VISIBLE);
			web.loadUrl("about:blank");
			String lnk = "<br/><br/><br/><div align='center'><h1><a href='" + att.url + "'><img src='" + att.icon
				+ "' height='50' width='50'/>&nbsp;" + att.name + "</a></h1></div>";
			String html = "<html><body style='background-color:#b0c4de; font-size:200%; padding:10,10,10,10;' >" + lnk
				+ "</body></html>";
			web.loadData(html, "text/html", "UTF-8");
		//}
	}
	
	private void showThmb(Thumbnail pic) {
		//TODO http://www.w3schools.com/jsref/tryit.asp?filename=tryjsref_img_naturalwidth
		
		Log.v("gallery", "showThmb: " + pic.link);
		txt.setVisibility(View.GONE);
		web.setVisibility(View.VISIBLE);
		web.loadUrl("about:blank");
		String rot = pic.rotation == 0 ? "" :
			" -webkit-transform: rotate(@deg); -moz-transform: rotate(@deg);".replace("@", Integer.toString(pic.rotation));
		String css = "style='position: absolute; top:0; bottom:0; margin: auto;" + rot +
			(pic.landscape ? " width: 100%;" : " height: 100%;") + "'";
		String img;
		if (pic.isFFMediaPic() || pic.isSimplePic())
			img = "<img id='pic' " + css + " src='" + pic.link + "'>";
		else if (pic.isYouTube())
			img = "<a href='" + pic.videoUrl + "'><img id='pic' " + css + " src='" + pic.videoPreview() + "'></a>";
		else
			img = "<a href='" + pic.link + "'><img id='pic' " + css + " src='" + pic.url + "'></a>";
		Log.v("gallery", img);
		String html = "<html><head><meta name='viewport' content='width=device-width'></head>" +
			"<body style='margin: 0; padding: 0;'><div style='height: 100vh; position: relative'>" +
			img + "</div></body></html>";
		web.loadData(html, "text/html", "UTF-8");
	}
}