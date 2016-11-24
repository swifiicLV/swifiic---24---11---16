package swifiic.soa;

import android.util.*;

import java.io.UnsupportedEncodingException;
import java.util.*;
import java.net.*;
/**
 * Created by venky() on 21/9/16.
 */
public class Helpers {
    public static String getUrlFromNameValue(ArrayList<Pair<String,String>> params) throws UnsupportedEncodingException {

        StringBuilder result = new StringBuilder();

        boolean emptyparams = true;

        for(int i = 0 ; i<params.size() ; ++i)
        {
            if(emptyparams == true)
                emptyparams = false;
            else
                result.append("&");

            result.append(URLEncoder.encode((params.get(i)).first, "UTF-8"));
            result.append("=");
            if((params.get(i)).second != null)
            result.append(URLEncoder.encode((params.get(i)).second, "UTF-8"));


        }
/*
        String tmpp1 = new String();
        tmpp1 = "";
        for(int i = 0 ; i<params.size() ; ++i)
        {

            if((params.get(i)).first=="" || (params.get(i)).first==null)
            {
                continue;
            }
            result.append(URLEncoder.encode((params.get(i)).first, "UTF-8"));
            result.append("=");
            if((params.get(i)).second==null)
                result.append(URLEncoder.encode("", "UTF-8"));
            else
            result.append(URLEncoder.encode((params.get(i)).second, "UTF-8"));
            if(i!=params.size()-1)
            {
                result.append("&");
            }

        }
*/
        return result.toString();
    }

}
