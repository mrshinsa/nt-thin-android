package com.forwiz.nursetree.view;

import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import java.lang.ref.WeakReference;


public abstract class BaseAppCompatActivity extends AppCompatActivity {

    private WeakReference<AppCompatActivity> activityWeakRef;

    @Override
    protected void onResume() {
        super.onResume();
        activityWeakRef = new WeakReference(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            this.finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected boolean isSafeToShowDialog()
    {
        return (activityWeakRef.get() != null && !activityWeakRef.get().isFinishing());
    }
}
