package edu.gvsu.cis.dulimarh.checkout;

import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Created by dulimarh on 3/11/15.
 * Based on: https://gist.github.com/jamiechapman/5375115
 */
public class ParseProxyObject implements Serializable {
    private HashMap<String, Object> values = new HashMap<String,
            Object>();

    public ParseProxyObject (ParseObject obj)
    {
        for (String key : obj.keySet()) {
            Class klass = obj.get(key).getClass();
            if (klass == ParseFile.class) {
                ParseFile pf = (ParseFile) obj.get(key);
                try {
                    values.put (key, pf.getData());
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
            else if (klass == ParseObject.class) {
                values.put (key, new ParseProxyObject((ParseObject) obj.get
                        (key)));
            }
            else {
                values.put(key, obj.get(key));
            }
        }
    }




}
