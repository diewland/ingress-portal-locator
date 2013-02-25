package mfec.enlightened.portal.locator;

import java.io.File;
import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.location.LocationManager;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MainActivity extends Activity {

	private GoogleMap map;
	private Marker marker;
	private Double map_lat;
	private Double map_long;
	private Double map_lat_ori;
	private Double map_long_ori;
	private String selectedUri;
	private File selectedFile;
	private static final int SELECT_PHOTO = 100;
	
	private Context c;
	private LocationManager lm;

	// ########## ON CREATE ##########
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY); // custom
		getActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.bg55)); // custom
		setContentView(R.layout.activity_main);
		
		// set up view
		setUpMapIfNeeded();
		
		// get app variables
		c = getApplicationContext();
		lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
	    Intent intent = getIntent();
	    Bundle extras = intent.getExtras();
	    String action = intent.getAction();
	    
		if(Intent.ACTION_SEND.equals(action)){
			// come from send menu
			if(extras.containsKey(Intent.EXTRA_STREAM)){
				Uri uri = (Uri) extras.getParcelable(Intent.EXTRA_STREAM);
				updateLatLongFromImage(uri);
				moveToLatLong();
			}
		}
		else if(extras == null) {
			// NOT callback from selecting image, go to gallery picker
			Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
			photoPickerIntent.setType("image/jpeg");
			startActivityForResult(photoPickerIntent, SELECT_PHOTO);
		}
	}
	
	// ########## RESPONSE FROM GALLERY PICKER ##########

	protected void onActivityResult(int requestCode, int resultCode, Intent data){
		super.onActivityResult(requestCode, resultCode, data);
		
		if(requestCode == SELECT_PHOTO && resultCode == RESULT_OK){
			Uri selectedImg = data.getData();
			updateLatLongFromImage(selectedImg);
			moveToLatLong();
		}
	}
	
	// ########## MAP FUNCTIONS ##########
	
	private void setUpMapIfNeeded() {
	    // Do a null check to confirm that we have not already instantiated the map.
	    if (map == null) {
	        map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map))
	                            .getMap();
	        // Check if we were successful in obtaining the map.
	        if (map != null) {
	            // The Map is verified. It is now safe to manipulate the map.
	        	map.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
					public void onMarkerDragEnd(Marker arg0) {
						map_lat = getCurrentLat();
						map_long = getCurrentLong();
					}
					public void onMarkerDragStart(Marker arg0) {}
					public void onMarkerDrag(Marker arg0) {}
				});
	        	map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
					public boolean onMarkerClick(Marker m) {
						AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
						builder.setTitle("Enter Lat,Long");
						final EditText input = new EditText(MainActivity.this); 
						input.setText(map_lat + "," + map_long);
						builder.setView(input);
						builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {  
							public void onClick(DialogInterface dialog, int whichButton) {
								try {
									String[] bestloc = input.getText().toString().split(",");
									map_lat_ori  = map_lat  = Double.parseDouble(bestloc[0]);
									map_long_ori = map_long = Double.parseDouble(bestloc[1]);
									moveToLatLong();
								}
								catch(Exception e){
									Utility.toast(c, "Invalid format ex. 13.764912,100.538308", Toast.LENGTH_LONG);
								}
						        return;                  
							}  
					    });  
					    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					        public void onClick(DialogInterface dialog, int which) {
					            return;   
					        }
					    });
						builder.show();
						return false;
					}
				});
	        }
	    }
	}
	
	private void moveToLatLong(){
		LatLng ll = new LatLng(map_lat, map_long);
		if(marker == null){
			marker = map.addMarker(new MarkerOptions()
										.position(ll)
										.draggable(true));
			map.moveCamera(CameraUpdateFactory.newLatLngZoom(ll, 15));
		} else {
			marker.setPosition(ll);
			map.moveCamera(CameraUpdateFactory.newLatLng(ll));
		}
		// marker.setTitle(map_lat + ", " + map_long);
	}
	
	private Double getCurrentLat(){
		return marker.getPosition().latitude;
	}
	
	private Double getCurrentLong(){
		return marker.getPosition().longitude;
	}
	
	private void restoreOriLatLong(){
		map_lat = map_lat_ori;
		map_long = map_long_ori;
	}
	
	// ########## IMAGE FUNCTIONS ##########

	private void updateLatLongFromImage(Uri selectedImg){
		Cursor csr = getContentResolver().query(selectedImg, new String[]{android.provider.MediaStore.Images.ImageColumns.DATA}, null, null, null);
		csr.moveToFirst();
		
		Log.d("imglog",csr.getString(0));
		selectedUri = csr.getString(0);
		try {
			ExifInterface exif = new ExifInterface(selectedUri);
			String dmslat = exif.getAttribute(exif.TAG_GPS_LATITUDE);
			String dmslatref = exif.getAttribute(exif.TAG_GPS_LATITUDE_REF);
			String dmslong = exif.getAttribute(exif.TAG_GPS_LONGITUDE);
			String dmslongref = exif.getAttribute(exif.TAG_GPS_LONGITUDE_REF);
			if(dmslat != null){
				map_lat_ori = Utility.convertToDegree(dmslat, dmslatref);
				map_long_ori = Utility.convertToDegree(dmslong, dmslongref);
				map_lat = map_lat_ori;
				map_long = map_long_ori;
				Log.d("maplat", "Got Photo LOC: " + map_lat + " - " + map_long);
				Utility.toast(c, "Found location\n" + map_lat + "\n" + map_long, Toast.LENGTH_SHORT);
			}else{
				// load current location
				double[] bestloc = Utility.getLocation(lm);
				if(bestloc != null){
					map_lat_ori  = map_lat  = bestloc[0];
					map_long_ori = map_long = bestloc[1];
				}
				Log.d("maplat","Null TAG");
				Utility.toast(c, "Location not found", Toast.LENGTH_SHORT);
			}
		} catch (IOException e) {
			Utility.toast(c, "Error Open Image", Toast.LENGTH_SHORT);
		}
		
		// update filename
		selectedFile = new File(selectedUri);
		getActionBar().setTitle(selectedFile.getName());
	}
	
	private void saveLatLongToImage(){
		// update lat_ori, long_ori
		map_lat_ori = map_lat;
		map_long_ori = map_long;
		// save to file
		try {
			ExifInterface exif = new ExifInterface(selectedUri);
			String str_lat = Utility.convertToDMS(map_lat);
			String lat_ref = map_lat > 0 ? "N" : "E";
			String str_long = Utility.convertToDMS(map_long);
			String long_ref = map_long > 0 ? "E" : "W";
			exif.setAttribute(exif.TAG_GPS_LATITUDE, str_lat);
			exif.setAttribute(exif.TAG_GPS_LATITUDE_REF, lat_ref);
			exif.setAttribute(exif.TAG_GPS_LONGITUDE, str_long);
			exif.setAttribute(exif.TAG_GPS_LONGITUDE_REF, long_ref);
			exif.saveAttributes();
			Utility.toast(c, "Save to\n"+ map_lat +"\n"+ map_long, Toast.LENGTH_SHORT);
		} catch (IOException e) {
			Utility.toast(c, "Error Saving Location", Toast.LENGTH_SHORT);
		}
	}

	// ########## MENU ##########

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		
		case R.id.menu_reset:
			restoreOriLatLong();
			moveToLatLong();
			break;
			
		case R.id.menu_save:
			saveLatLongToImage();
			break;
			
		case R.id.menu_share:
			AlertDialog.Builder sBuilder = new AlertDialog.Builder(this);
			sBuilder.setTitle("Enter portal name");
			final EditText input = new EditText(this); 
			sBuilder.setView(input);
			sBuilder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {  
				public void onClick(DialogInterface dialog, int whichButton) {  
		  			// save before send
					saveLatLongToImage();
					// send mail w/ attachment
					Intent intent = new Intent(Intent.ACTION_SEND);
					intent.setType("message/rfc822");
					intent.putExtra(Intent.EXTRA_EMAIL, new String[] {"super-ops@google.com"});
					intent.putExtra(Intent.EXTRA_SUBJECT, input.getText().toString());
					// intent.putExtra(Intent.EXTRA_TEXT, "body text");
					intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(selectedFile));
					startActivity(Intent.createChooser(intent, "Send mail via"));
			        return;                  
				}  
		    });  
		    sBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
		        public void onClick(DialogInterface dialog, int which) {
		            return;   
		        }
		    });
			sBuilder.show();
			/*
			// NOT WORK #1
			Intent share = new Intent(Intent.ACTION_SEND);
			share.setType("image/jpeg");
			// share.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + selectedUri));
			share.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(selectedFile));
			startActivity(Intent.createChooser(share, "Share Image"));
			//
			// NOT WORK #2
			Intent superOps = new Intent(Intent.ACTION_SEND);
			superOps.setType("image/jpeg");
			superOps.setFlags(0x80001);
			superOps.setComponent(new ComponentName("com.nianticproject.ingress", "com.nianticproject.ingress.share.ShareToSuperOpsActivity"));
			superOps.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(selectedFile));
			startActivity(superOps);
			*/
			break;
			
		case R.id.menu_layers:
			AlertDialog.Builder lBuilder = new AlertDialog.Builder(this);
			lBuilder.setTitle("Pick a layer")
						.setItems(R.array.map_layer, new DialogInterface.OnClickListener(){
							public void onClick(DialogInterface dialog, int which){
								switch(which){ // sync with map_layer xml array
									case 0:
										map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
										break;
									case 1:
										map.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
										break;
									case 2:
										map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
										break;
									case 3:
										map.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
										break;
								}
							}
						});
			lBuilder.show();
			break;
			
		case R.id.menu_browse:
			Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
			photoPickerIntent.setType("image/jpeg");
			startActivityForResult(photoPickerIntent, SELECT_PHOTO);
			break;
		}
		return super.onOptionsItemSelected(item);
	}

}
