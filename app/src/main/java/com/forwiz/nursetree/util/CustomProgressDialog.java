package com.forwiz.nursetree.util;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StyleRes;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;

import com.forwiz.nursetree.R;

/**
 * Created by YunHo on 2017-09-20.
 */
public class CustomProgressDialog extends Dialog {

    private ImageView waitingInImage;
    private ImageView waitingOutImage;

    private static final float ROTATE_FROM = -10.0f * 360.0f;
    private static final float ROTATE_TO = 0.0f;

    private RotateAnimation inAnim = new RotateAnimation(ROTATE_TO, ROTATE_FROM, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
    private RotateAnimation outAnim = new RotateAnimation(ROTATE_FROM, ROTATE_TO, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);


    public CustomProgressDialog(@NonNull Context context) {
        super(context);
        if (this.getWindow() != null) {
            this.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    public CustomProgressDialog(@NonNull Context context, @StyleRes int themeResId) {
        super(context, themeResId);
        if (this.getWindow() != null) {
            this.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_custom_progress);
        setLayout();

        super.onCreate(savedInstanceState);
    }

    /*
     * Layout
     */
    private void setLayout() {
        waitingInImage = findViewById(R.id.waiting_ani_in_image);
        waitingOutImage = findViewById(R.id.waiting_ani_out_image);
        inAnim.setInterpolator(new LinearInterpolator());
        inAnim.setRepeatCount(Animation.INFINITE);
        inAnim.setDuration(6500);

        outAnim.setInterpolator(new LinearInterpolator());
        outAnim.setRepeatCount(Animation.INFINITE);
        outAnim.setDuration(6500);

        // Start animating the image

        startAnimationImage();

    }
    private void startAnimationImage(){
        waitingInImage.setAnimation(inAnim);
        waitingOutImage.setAnimation(outAnim);
        waitingInImage.startAnimation(inAnim);
        waitingOutImage.startAnimation(outAnim);
    }

}
