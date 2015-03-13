package edu.gvsu.cis.dulimarh.checkout;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by dulimarh on 3/12/15.
 */
public class ImageStore {
    private static Map<String,Drawable> images;

    public static void newInstance(Context ctx) {
        images = new HashMap<String, Drawable>();
        images.put("DEFAULT-IMAGE", ctx.getResources().getDrawable(R
                .drawable
                .male_user_icon));
    }

    public static void put (String key, Drawable val)
    {
        Log.d("HANS", "Place drawable for key " + key);
        images.put(key, val);
    }

    public static Drawable get (String key) {
        Log.d("HANS", "Lookup drawable for key " + key);
        Drawable d;
            d = images.get(key);
//        if (d != null)
            return d;
//        else
//            return images.get("DEFAULT-IMAGE");
    }

    public static void extractFrom (ParseObject p) {
        try {
            if (p.has("user_photo")) {
                ParseFile upic = p.getParseFile
                        ("user_photo");
                ByteArrayInputStream bis = new
                        ByteArrayInputStream(upic.getData());
               String uid = p.getObjectId();

                images.put(uid,
                        Drawable.createFromStream(bis, ""));
            }
            if (p.has("signature")) {
                ParseFile sig = p.getParseFile("signature");
                ByteArrayInputStream bis = new
                        ByteArrayInputStream(sig.getData());

                images.put(sig.getUrl(),
                        Drawable.createFromStream(bis, ""));
            }
        } catch (ParseException e1) {
            e1.printStackTrace();
        }

    }
}
