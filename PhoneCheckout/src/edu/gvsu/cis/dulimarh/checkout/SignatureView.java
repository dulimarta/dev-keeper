package edu.gvsu.cis.dulimarh.checkout;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

/**
 * 
 * @author Hans Dulimarta <dulimarh@cis.gvsu.edu>
 *
 * This activity captures user signature and saves it to an image file.
 * A user signature may have several strokes.
 * It works by using the following technique:
 * (a) onTouchEvent records a single stroke and at the end of each stroke
 *     (ACTION_UP) the stroke is written to a canvas.
 * (b) onDraw then does two tasks: render the previous strokes recorded
 *     on the canvas (its associated bitmap) and also the current stroke
 *     being drawn by the user
 */
public class SignatureView extends View {
    private final String TAG = getClass().getName();
    private final static int TOLERANCE = 4;
    private final static int WIDTH = 225;
    private final static int HEIGHT = 300;
    private Path sig;
    private Paint sigColor;
    private Bitmap sigBmp;
    private Canvas sigCanvas;
    private float posx, posy;
    private boolean moved;
    
    public SignatureView(Context context) {
        super(context);
    }

    /* This constructor is needed to parse any layout parameters 
     * specific for SignatureView 
     */
    public SignatureView(Context context, AttributeSet attrs) {
        super(context, attrs);
//        final int N = attrs.getAttributeCount();
//        for (int k = 0; k < N; k++) {
//            String name = attrs.getAttributeName(k);
//            String value = attrs.getAttributeValue(k);
//            Log.d(TAG, "Attribute-" + k + " " + name + " => " + value);
//        }
    }

    public void reset()
    {
        sigBmp.eraseColor(Color.GRAY);
        sig.reset();
        invalidate();
        
    }
    
    private void buildView(int width, int height)
    {
        sig = new Path();
        sigColor = new Paint();
        sigColor.setColor(0xFF0000F0);
        sigColor.setStyle(Paint.Style.STROKE);
        sigColor.setStrokeWidth(7);
        sigBmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        sigCanvas = new Canvas(sigBmp);
    }

    /* (non-Javadoc)
     * @see android.view.View#onTouchEvent(android.view.MotionEvent)
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
            sigColor.setStyle(Style.STROKE);
            moved = false;
            sig.reset();
            sig.moveTo(x, y);
            posx = x;
            posy = y;
            break;
        case MotionEvent.ACTION_MOVE:
            float dx = Math.abs(posx - x);
            float dy = Math.abs(posy - y);
            if (dx >= TOLERANCE || dy >= TOLERANCE) {
                moved = true;
                /* use Bezier segments for smoother curves */
                sig.quadTo(posx, posy, (x + posx)/2, (y + posy)/2);
                posx = x;
                posy = y;
            }
            break;
        case MotionEvent.ACTION_UP:
            if (moved) {
                sig.lineTo(x, y);
                sigCanvas.drawPath(sig, sigColor);
            }
            else {
                sigColor.setStyle(Style.FILL);
                sig.addCircle(x, y, 5, Direction.CW);
                sigCanvas.drawPath(sig, sigColor);
            }
        }
        invalidate();
        return true;
    }

    /* (non-Javadoc)
     * @see android.view.View#onDraw(android.graphics.Canvas)
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(0XFFAAAAAA);
        canvas.drawBitmap(sigBmp, 0, 0, sigColor);
        canvas.drawPath(sig, sigColor);
    }

    /* (non-Javadoc)
     * @see android.view.View#onMeasure(int, int)
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(resolveSize(WIDTH, widthMeasureSpec), 
                resolveSize(HEIGHT, heightMeasureSpec));
    }

    /* (non-Javadoc)
     * @see android.view.View#onLayout(boolean, int, int, int, int)
     */
    @Override
    protected void onLayout(boolean changed, int left, int top, int right,
            int bottom) {
        if (changed) {
//            Log.d(TAG, String.format(
//                    "OnLayout: %s: left=%d, top=%d, right=%d, bottom=%d",
//                    changed, left, top, right, bottom));
//            Log.d(TAG, "Width=" + (right - left) + " height=" + (bottom - top));
            buildView(right - left, bottom - top);
        }
    }
    
}
