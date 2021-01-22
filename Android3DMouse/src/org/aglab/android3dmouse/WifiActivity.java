package org.aglab.android3dmouse;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class WifiActivity extends Activity {

	private Button mConnect;
	private EditText mIp;
	private EditText mPort;
	
	private String ipAddress;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.wifi_layout);
		
		mIp = (EditText) findViewById(R.id.ipaddress);
		
		ipAddress = mIp.getText().toString();
		
		MouseSettings.init(this.getApplicationContext());
		
		if (MouseSettings.ip != null) {
			mIp.setText(MouseSettings.ip);
		}
		
		mConnect = (Button) findViewById(R.id.connectButton);
		mConnect.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				onConnectButton();
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.general_menu, menu);
		return true;
	}
	
	private void onConnectButton() {
		// TODO Auto-generated method stub
		
		
			try {
				//
				Intent i = new Intent(this, MouseActivity.class);
				this.startActivity(i);
			} catch (Exception ex) {
				//this.tvError.setText("Invalid IP address");
				//this.tvError.setVisibility(View.VISIBLE);
				Toast.makeText(this, this.getResources().getText(R.string.toast_invalidIP), Toast.LENGTH_LONG).show();
				Log.d("3dMouse","1" +  ex.toString());
			}
		
		
	}
	

}
