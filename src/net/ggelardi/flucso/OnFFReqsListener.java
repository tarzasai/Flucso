package net.ggelardi.flucso;

public interface OnFFReqsListener {
	
	void openInfo(String feed_id);
	
	void openFeed(String name, String feed_id, String query);
	
	void openEntry(String entry_id);
	
	void openGallery(String entry_id, int position);
	
	void openPostNew(String[] dsts, String body, String link, String[] tmbs);
	
	void openPostEdit(String entry_id, String body);
}