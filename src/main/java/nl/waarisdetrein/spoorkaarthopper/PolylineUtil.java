package nl.waarisdetrein.spoorkaarthopper;

import com.graphhopper.util.PointList;

public class PolylineUtil {

	 public static String encodePolyline( PointList poly )
	    {
	        StringBuilder sb = new StringBuilder();
	        int size = poly.getSize();
	        int prevLat = 0;
	        int prevLon = 0;
	        for (int i = 0; i < size; i++)
	        {
	            int num = (int) Math.floor(poly.getLatitude(i) * 1e5);
	            encodeNumber(sb, num - prevLat);
	            prevLat = num;
	            num = (int) Math.floor(poly.getLongitude(i) * 1e5);
	            encodeNumber(sb, num - prevLon);
	            prevLon = num;
	        }
	        return sb.toString();
	    }

	    private static void encodeNumber( StringBuilder sb, int num )
	    {
	        num = num << 1;
	        if (num < 0)
	        {
	            num = ~num;
	        }
	        while (num >= 0x20)
	        {
	            int nextValue = (0x20 | (num & 0x1f)) + 63;
	            sb.append((char) (nextValue));
	            num >>= 5;
	        }
	        num += 63;
	        sb.append((char) (num));
	    }
	
}
