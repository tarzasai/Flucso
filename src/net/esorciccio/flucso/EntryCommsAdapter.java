package net.esorciccio.flucso;

import net.esorciccio.flucso.Commons.PK;
import net.esorciccio.flucso.FFAPI.Entry.Comment;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.Html;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Callback;

public class EntryCommsAdapter extends EntryBaseAdapter {
	
	public EntryCommsAdapter(Context context, OnClickListener clickListener) {
		super(context, clickListener);
	}

	@Override
	public int getCount() {
		return entry != null ? entry.comments.size() : 0;
	}
	
	@Override
	public Comment getItem(int position) {
		return entry.comments.get(position);
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
			view = inflater.inflate(R.layout.item_entry_comm, parent, false);
			vh = new ViewHolder();
			vh.imgFrom = (ImageView) view.findViewById(R.id.img_comm_from);
			vh.txtFrom = (TextView) view.findViewById(R.id.txt_comm_from);
			vh.txtTime = (TextView) view.findViewById(R.id.txt_comm_time);
			vh.txtBody = (TextView) view.findViewById(R.id.txt_comm_body);
			vh.imgPict = (ImageView) view.findViewById(R.id.img_comm_media);
			vh.imgMenu = (ImageView) view.findViewById(R.id.img_comm_popup);
			vh.imgMenu.setOnClickListener(listener);
			view.setTag(vh);
		} else {
			vh = (ViewHolder) view.getTag();
		}
		vh.imgMenu.setTag(Integer.valueOf(position));
		Comment comm = getItem(position);
		vh.txtBody.setText(Html.fromHtml(comm.body));
		vh.imgMenu.setVisibility(View.GONE);
		vh.imgPict.setVisibility(View.GONE);
		if (comm.placeholder) {
			vh.imgFrom.setVisibility(View.GONE);
			vh.txtFrom.setVisibility(View.GONE);
			vh.txtTime.setVisibility(View.GONE);
		} else {
			vh.imgFrom.setVisibility(View.VISIBLE);
			vh.txtFrom.setVisibility(View.VISIBLE);
			vh.txtTime.setVisibility(View.VISIBLE);
			Commons.picasso(context).load(comm.from.getAvatarUrl()).placeholder(R.drawable.nomugshot).into(vh.imgFrom);
			vh.txtFrom.setCompoundDrawablesWithIntrinsicBounds(comm.from.locked ? R.drawable.entry_private : 0, 0, 0, 0);
			vh.txtFrom.setText(comm.from.getName());
			String tl = comm.getFuzzyTime();
			if (comm.via != null && !TextUtils.isEmpty(comm.via.name.trim()))
				tl += new StringBuilder().append(" ").append(context.getString(R.string.source_prefix)).append(
					" ").append(comm.via.name.trim()).toString();
			vh.txtTime.setText(tl);
			if (comm.canEdit() || comm.canDelete())
				vh.imgMenu.setVisibility(View.VISIBLE);
			// picture in comment
			int imco = session.getPrefs().getInt(PK.ENTR_IMCO, 1);
			if (imco == 2 || (imco == 1 && Commons.isOnWIFI(context))) {
				final String img = comm.getFirstImage();
				if (!TextUtils.isEmpty(img)) {
					final ImageView iref = vh.imgPict;
					Commons.picasso(context).load(img).placeholder(R.drawable.ic_action_picture).error(
						android.R.drawable.ic_dialog_alert).into(vh.imgPict, new Callback() {
							@Override
							public void onError() {
								iref.setVisibility(View.GONE);
							}
							@Override
							public void onSuccess() {
								iref.setVisibility(View.VISIBLE);
								iref.setOnClickListener(new OnClickListener() {
									@Override
									public void onClick(View v) {
										Intent i = new Intent(Intent.ACTION_VIEW);
										i.setData(Uri.parse(img));
										context.startActivity(i);
									}
								});
							}
						});
				}
			}
		}
		return view;
	}
	
	public static class ViewHolder {
		public ImageView imgFrom;
		public TextView txtFrom;
		public TextView txtTime;
		public TextView txtBody;
		public ImageView imgMenu;
		public ImageView imgPict;
	}

	@Override
	public int getIcon() {
		return R.drawable.entry_comment;
	}
}