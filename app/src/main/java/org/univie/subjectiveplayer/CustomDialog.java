package org.univie.subjectiveplayer;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

public class CustomDialog extends Dialog {
    private final Context mConfigContext;

    protected CustomDialog (Context context) {
        super(context, R.style.AlertDialogTheme);
        // Create a context with current configuration for layout inflation
        // (needed when using android:configChanges to pick correct layout-port vs layout)
        mConfigContext = context.createConfigurationContext(context.getResources().getConfiguration());
    }

    @Override
    public void setContentView(int layoutResID) {
        // Inflate with configuration-aware context to pick correct orientation layout
        View view = LayoutInflater.from(mConfigContext).inflate(layoutResID, null);
        super.setContentView(view);
    }
}
