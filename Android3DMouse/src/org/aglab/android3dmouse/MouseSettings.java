package org.aglab.android3dmouse;

import java.util.LinkedList;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.KeyCharacterMap;

public class MouseSettings {

	public static String ip;
	
	private static SharedPreferences prefs;
	
	private static final String PREFS_IPKEY = "remoteip";
	
	private static final String PREFS_FILENAME = "Android3DMouse";
	
	public MouseSettings() {
		// TODO Auto-generated constructor stub
	}
	
	public static void init(Context con) {
		if (prefs == null) {
			prefs = con.getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE);
			
			ip = prefs.getString(PREFS_IPKEY, "192.168.50.72");

		}
	}
	
	
	public static void setIp(String ip) throws Exception {
		SharedPreferences.Editor edit = prefs.edit();
		edit.putString(MouseSettings.PREFS_IPKEY, ip);
		edit.commit();
		MouseSettings.ip = ip;
	}

}
