package uk.co.synatronics.Datafusion;

import android.view.GestureDetector;
import android.view.MotionEvent;
import static java.lang.Math.*;

public class SwipeDetector extends GestureDetector.SimpleOnGestureListener {

    protected static final int DISTANCE_THRESHOLD = 100;
    protected static final int VELOCITY_THRESHOLD = 100;
    protected final OnSwipeListener callback;

    public SwipeDetector(OnSwipeListener listener) {
        this.callback = listener;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if (callback != null) {
            float distanceX = e2.getX()-e1.getX();
            float distanceY = e2.getY()-e1.getY();
            float absDistX = abs(distanceX);
            float absDistY = abs(distanceY);
            float absVelocX = abs(velocityX);
            float absVelocY = abs(velocityY);

            if (absDistX > absDistY && absDistX > DISTANCE_THRESHOLD && absVelocX > VELOCITY_THRESHOLD) {
                return distanceX < 0 ?
                        callback.onSwipeLeft(absDistX, velocityX)
                        : callback.onSwipeRight(absDistX, velocityX);
            }
            else if (absDistY > absDistX && absDistY > DISTANCE_THRESHOLD && absVelocY > VELOCITY_THRESHOLD) {
                return distanceY < 0 ?
                        callback.onSwipeDown(absDistY, velocityY)
                        : callback.onSwipeUp(absDistY, velocityY);
            }
        }
        return super.onFling(e1, e2, velocityX, velocityY);
    }

    public interface OnSwipeListener {

        boolean onSwipeLeft(float distance, float velocity);

        boolean onSwipeRight(float distance, float velocity);

        boolean onSwipeDown(float distance, float velocity);

        boolean onSwipeUp(float distance, float velocity);
    }
}
