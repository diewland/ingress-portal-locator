package mfec.enlightened.portal.locator;

import java.util.List;

import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

public class Utility {

	public static double convertToDegree(String stringDMS,String ref){
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
	
	public static String convertToDMS(double coord){
		String[] dms = Location.convert(coord, Location.FORMAT_SECONDS).split(":");
		String deg = dms[0];
		String min = dms[1];
		String[] secs = dms[2].split("\\.");
		String sec = secs[0]+secs[1];
		String ret = deg + "/1," + min + "/1," + sec + "/100000";
		return ret;
	}
	
	public static double[] getLocation(LocationManager lm){
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
	
	// http://stackoverflow.com/questions/3375166/android-drawable-images-from-url
	// ...
	
}
