package mfec.enlightened.portal.locator;

import java.io.IOException;
import java.util.List;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
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
	private static final int SELECT_PHOTO = 100;
	
	double convertToDegree(String stringDMS,String ref){
		//from http://android-er.blogspot.com/2010/01/convert-exif-gps-info-to-degree-format.html
		Double result = null;
		
		Log.d("maplat", stringDMS);
		String[] DMS = stringDMS.split(",", 3);
		String[] stringD = DMS[0].split("/", 2);
		Double D0 = new Double(stringD[0]);
		Double D1 = new Double(stringD[1]);
		Double FloatD = D0/D1;

		String[] stringM = DMS[1].split("/", 2);
		Double M0 = new Double(stringM[0]);
		Double M1 = new Double(stringM[1]);
		Double FloatM = M0/M1;
		  
		String[] stringS = DMS[2].split("/", 2);
		Double S0 = new Double(stringS[0]);
		Double S1 = new Double(stringS[1]);
		Double FloatS = S0/S1;
		  
		result = new Double(FloatD + (FloatM/60) + (FloatS/3600));
		if(ref.equals("W") || ref.equals("S")){
			result *= -1;
		}
		return result;
	}
	
	String convertToDMS(double coord){
		String[] dms = Location.convert(coord, Location.FORMAT_SECONDS).split(":");
		String deg = dms[0];
		String min = dms[1];
		String[] secs = dms[2].split("\\.");
		String sec = secs[0]+secs[1];
		String ret = deg + "/1," + min + "/1," + sec + "/100000";
		return ret;
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data){
		super.onActivityResult(requestCode, resultCode, data);
		
		if(requestCode == SELECT_PHOTO && resultCode == RESULT_OK){
			Uri selectedImg = data.getData();
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
					map_lat_ori = convertToDegree(dmslat, dmslatref);
					map_long_ori = convertToDegree(dmslong, dmslongref);
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
			String fileName = getFileNameFromUri(selectedImg, getContentResolver());
			((TextView)findViewById(R.id.filename)).setText(fileName);
		}
	}
	
	private double[] getLocation(){
		LocationManager lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		List<String> providers = lm.getAllProviders();
		Location l = null;
		double[] ret = new double[2];
		for(int i = providers.size() -1; i >= 0; i--){
			l = lm.getLastKnownLocation(providers.get(i));
			if(l != null) break;
		}
		if(l != null){
			ret[0] = l.getLatitude();
			ret[1] = l.getLongitude();
			return ret;
		}
		return null;
	}
	
	private String getFileNameFromUri(Uri selectedImg, ContentResolver cr){
		String fileName = null;
		String scheme = selectedImg.getScheme();
		if (scheme.equals("file")) {
		    fileName = selectedImg.getLastPathSegment();
		}
		else if (scheme.equals("content")) {
		    String[] proj = { MediaStore.Images.Media.TITLE };
		    Cursor cursor = getContentResolver().query(selectedImg, proj, null, null, null);
		    if (cursor != null && cursor.getCount() != 0) {
		        int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.TITLE);
		        cursor.moveToFirst();
		        fileName = cursor.getString(columnIndex);
		    }
		}
		return fileName;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		// Default Location from coarse location
		double[] bestloc = getLocation();
		if(bestloc != null){
			map_lat_ori  = map_lat  = bestloc[0];
			map_long_ori  = map_long  = bestloc[1];
		}
		
		Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
		photoPickerIntent.setType("image/jpeg");
		startActivityForResult(photoPickerIntent, SELECT_PHOTO);
		
		// init
		setUpMapIfNeeded();
		
		// move to lat, long
		moveToLatLong();
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
				String str_lat = convertToDMS(map_lat);
				String lat_ref = map_lat > 0 ? "N" : "E";
				String str_long = convertToDMS(map_long);
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
			// TODO
			Toast.makeText(getBaseContext(), "Under Contruction", Toast.LENGTH_SHORT).show();
			break;
			
		case R.id.menu_browse:
			Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
			photoPickerIntent.setType("image/jpeg");
			startActivityForResult(photoPickerIntent, SELECT_PHOTO);
			break;
			
		case R.id.menu_v_normal:
			map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
			break;
			
		case R.id.menu_v_satellite:
			map.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
			break;
		}
		return super.onOptionsItemSelected(item);
	}

}
