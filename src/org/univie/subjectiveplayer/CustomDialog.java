package org.univie.subjectiveplayer;

import android.app.Dialog;
import android.content.Context;

public class CustomDialog extends Dialog {
    protected CustomDialog (Context context) {
        super(context, R.style.AlertDialogTheme);
    }
}
