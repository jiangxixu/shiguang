package io.github.jiangxixu.shiguang;

import java.net.URL;
import javax.net.ssl.HttpsURLConnection;
import android.os.SystemClock;

final class XhsRedirects {
    static String resolve(String value){
        final long deadline=SystemClock.elapsedRealtime()+8000;
        return XhsLinks.resolve(value,url->{
            long remaining=deadline-SystemClock.elapsedRealtime();if(remaining<=0)throw new Exception("timeout");
            // XhsLinks validates every hop before this callback; never send account cookies.
            HttpsURLConnection c=(HttpsURLConnection)new URL(url).openConnection();
            c.setInstanceFollowRedirects(false);c.setConnectTimeout((int)Math.min(2000,remaining));c.setReadTimeout((int)Math.min(2000,remaining));
            c.setRequestProperty("User-Agent","Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/130.0 Mobile Safari/537.36");
            try{int status=c.getResponseCode();return status>=300&&status<400?c.getHeaderField("Location"):null;}finally{c.disconnect();}
        });
    }
}
