package org.aglab.android3dmouse;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Button;

public class GeneralMenuActivity extends Activity {

	private Button wifiButton ;
	private Button btButton ;
	private Button CNButton ;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.general_menu);
		
		wifiButton = (Button)findViewById(R.id.wifibutton);
		wifiButton.setOnClickListener(wifiClick);
		
		btButton = (Button)findViewById(R.id.btbutton);
		btButton.setOnClickListener(btClick);
		
		CNButton = (Button)findViewById(R.id.CNbutton);
		CNButton.setOnClickListener(CNClick);
	}
	
	View.OnClickListener wifiClick = new View.OnClickListener() {
	    public void onClick(View v) {
	      // it was the 1st button
	    	Intent i = new Intent(GeneralMenuActivity.this,WifiActivity.class);
	    	startActivity(i);
	    }
	  };
	  
	View.OnClickListener btClick = new View.OnClickListener() {
	    public void onClick(View v) {
	      // it was the 1st button
	    	Intent i = new Intent(GeneralMenuActivity.this,BluetoothActivity.class);
	    	startActivity(i);
	    }
	 };
		  
	 View.OnClickListener CNClick = new View.OnClickListener() {
	    public void onClick(View v) {
	      // it was the 1st button
	    	Intent i = new Intent(GeneralMenuActivity.this,CelullarActivity.class);
	    	startActivity(i);
	   }
	 };

}
