package net.esorciccio.flucso;

import net.esorciccio.flucso.FFAPI.Entry.Attachment;
import net.esorciccio.flucso.FFAPI.Entry.Thumbnail;
import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class EntryFilesAdapter extends EntryBaseAdapter {
	
	public EntryFilesAdapter(Context context, OnClickListener clickListener) {
		super(context, clickListener);
	}
	
	@Override
	public int getCount() {
		return entry != null ? (entry.thumbnails.length + entry.files.length) : 0;
	}
	
	@Override
	public Object getItem(int position) {
		return position < entry.files.length ? entry.files[position] : entry.thumbnails[position - entry.files.length];
	}
	
	@Override
	public long getItemId(int position) {
		return position;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder vh;
		View view = convertView;
		if (view == null) {
			view = inflater.inflate(R.layout.item_entry_file, parent, false);
			vh = new ViewHolder();
			vh.img = (ImageView) view.findViewById(R.id.img_file);
			vh.llf = (LinearLayout) view.findViewById(R.id.ll_file);
			vh.ico = (ImageView) view.findViewById(R.id.img_icon);
			vh.txt = (TextView) view.findViewById(R.id.txt_file);
			view.setTag(vh);
		} else {
			vh = (ViewHolder) view.getTag();
		}
		final Object itm = getItem(position);
		if (itm instanceof Attachment) {
			Attachment att = (Attachment) itm;
			vh.img.setVisibility(View.GONE);
			vh.llf.setVisibility(View.VISIBLE);
			vh.txt.setText(att.name);
			Commons.picasso(context).load(att.icon).placeholder(R.drawable.ic_action_attachment).into(vh.ico);
		} else {
			Thumbnail tmb = (Thumbnail) itm;
			vh.img.setVisibility(View.VISIBLE);
			vh.llf.setVisibility(View.GONE);
			Commons.picasso(context).load(tmb.isYouTube() ? tmb.videoPreview() : tmb.url).placeholder(
				R.drawable.ic_action_picture).into(vh.img);
		}
		return view;
	}
	
	public static class ViewHolder {
		public ImageView img;
		public LinearLayout llf;
		public ImageView ico;
		public TextView txt;
	}

	@Override
	public int getIcon() {
		return R.drawable.attachment;
	}
}