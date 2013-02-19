package mfec.enlightened.portal.locator;

import java.io.File;
import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
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
	private Builder layerBuilder;

	private void loadImageToMap(Uri selectedImg){
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
				moveToLatLong();
				Log.d("maplat", "Got Photo LOC: " + map_lat + " - " + map_long);
			}else{
				Log.d("maplat","Null TAG");
			}
		} catch (IOException e) {
			Toast t = Toast.makeText(getApplicationContext(), "Error Open Image", Toast.LENGTH_SHORT);
			t.show();
		}
		
		// update filename
		selectedFile = new File(selectedUri);
		getActionBar().setTitle(selectedFile.getName());
		/*
		try { // ( API LV14 - ICS )
			//
			// 1 - not work
			// getActionBar().setIcon(getDrawableFromUrl(getResources(), selectedUri));
			//
			// 2 - not work
			// getActionBar().setLogo(getDrawableFromUrl(getResources(), selectedUri));
			//
			// 3 - not work
			// ImageView logo = (ImageView) findViewById(android.R.id.home);
			// logo.setImageDrawable(getDrawableFromUrl(getResources(), selectedUri));
			//
		} catch (Exception e) {
			e.printStackTrace();
		}
		*/
	}

	// response to Gallery Picker
	protected void onActivityResult(int requestCode, int resultCode, Intent data){
		super.onActivityResult(requestCode, resultCode, data);
		
		if(requestCode == SELECT_PHOTO && resultCode == RESULT_OK){
			Uri selectedImg = data.getData();
			loadImageToMap(selectedImg);
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// custom action bar --- must set before setContentView
		requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
		getActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.bg55));
	
		setContentView(R.layout.activity_main);
		
		setUpMapIfNeeded();
		
		// Default Location from coarse location
		LocationManager lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		double[] bestloc = Utility.getLocation(lm);
		if(bestloc != null){
			map_lat_ori  = map_lat  = bestloc[0];
			map_long_ori  = map_long  = bestloc[1];
		}
		
		// setup layer dialog
		layerBuilder = new AlertDialog.Builder(this);
		layerBuilder.setTitle("Pick a layer")
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
		
	    Intent intent = getIntent();
	    Bundle extras = intent.getExtras();
	    String action = intent.getAction();
		if(Intent.ACTION_SEND.equals(action)){ // come from Send to menu
			if(extras.containsKey(Intent.EXTRA_STREAM)){
				Uri uri = (Uri) extras.getParcelable(Intent.EXTRA_STREAM);
				loadImageToMap(uri);
			}
		}
		else { // come from app icon
			Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
			photoPickerIntent.setType("image/jpeg");
			startActivityForResult(photoPickerIntent, SELECT_PHOTO);
		}
	}
	
	private void setUpMapIfNeeded() {
	    // Do a null check to confirm that we have not already instantiated the map.
	    if (map == null) {
	        map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map))
	                            .getMap();
	        // Check if we were successful in obtaining the map.
	        if (map != null) {
	            // The Map is verified. It is now safe to manipulate the map.
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
	
	private Double[] updateOriLatLong(){
		map_lat = getCurrentLat();
		map_long = getCurrentLong();
		map_lat_ori = getCurrentLat();
		map_long_ori = getCurrentLong();
		return new Double[]{ map_lat, map_long };
	}
	
	private void restoreOriLatLong(){
		map_lat = map_lat_ori;
		map_long = map_long_ori;
	}
	
	// ---------------------------------------------------------------------------- //

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
			Double[] ll = updateOriLatLong();
			// save new lat,long to photo
			Double curr_lat = ll[0];
			Double curr_long = ll[1];
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
			} catch (IOException e) {
				Toast.makeText(getApplicationContext(),"Error Saving Location",Toast.LENGTH_LONG).show();
			}
			// toast
			Toast.makeText(getApplicationContext(), "Save to\n"+ map_lat +"\n"+ map_long, Toast.LENGTH_LONG).show();
			break;
			
		case R.id.menu_share:
			Intent share = new Intent(Intent.ACTION_SEND);
			share.setType("image/jpeg");
			// share.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + selectedUri));
			share.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(selectedFile));
			startActivity(Intent.createChooser(share, "Share Image"));
			break;
			
		case R.id.menu_layers:
			layerBuilder.show();
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
