package net.esorciccio.flucso;

import net.esorciccio.flucso.FFAPI.Entry;
import net.esorciccio.flucso.FFAPI.Entry.Attachment;
import net.esorciccio.flucso.FFAPI.Entry.Thumbnail;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.TextView;

public class GalleryFragment extends BaseFragment {
	public static final String FRAGMENT_TAG = "net.esorciccio.flucso.GalleryFragment";
	
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
		Log.v("gallery", "showThmb: " + pic.link);
		txt.setVisibility(View.GONE);
		web.setVisibility(View.VISIBLE);
		web.loadUrl("about:blank");
		String rot = pic.rotation == 0 ? "" :
			" -webkit-transform: rotate(@deg); -moz-transform: rotate(@deg);".replace("@", Integer.toString(pic.rotation));
		String css = "style='position: absolute; top:0; bottom:0; margin: auto;" + rot + "'";
		String img;
		if (pic.link.indexOf("/m.friendfeed-media.com/") > 0
			|| (pic.link.endsWith(".jpg") || pic.link.endsWith(".jpeg") || pic.link.endsWith(".png") || pic.link.endsWith(".gif")))
			img = "<img " + css + " width='100%' src='" + pic.link + "'>";
		else {
			String lnk = "<a href='" + pic.link + "'>";
			if (!TextUtils.isEmpty(pic.player)) {
				Document doc = Jsoup.parseBodyFragment(pic.player);
				Elements emb = doc.getElementsByTag("embed");
				if (emb != null && emb.size() > 0 && emb.get(0).hasAttr("src")) {
					String src = emb.get(0).attr("src");
					// http://www.youtube.com/v/0QgYc3dfduA&amp;autoplay=1&amp;showsearch=0&amp;ap=%2526fmt%3D18&amp;fs=1
					if (src.indexOf("youtube") > 0 && src.indexOf("&") > 0)
						src = src.substring(0, src.indexOf("&")); // otherwise we'd get an error 400.
					lnk = "<a href='" + src + "'>";
				}
			}
			img = lnk + "<img " + css + " width='100%' src='" + pic.url + "'></a>";
		}
		Log.v("gallery", img);
		String html = "<html><body style='margin: 0; padding: 0;' ><div style='height: 100vh; position: relative'>"
			+ img + "</div></body></html>";
		web.loadData(html, "text/html", "UTF-8");
	}
}