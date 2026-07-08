// LoadingDialogHelper.java
package com.example.secureme;

import android.app.ProgressDialog;
import android.content.Context;

public class LoadingDialogHelper {

    private static ProgressDialog dialog;

    /** Show a spinner dialog with the given message. */
    public static void show(Context ctx, String message) {
        hide(); // in case already showing
        dialog = new ProgressDialog(ctx);
        dialog.setMessage(message);
        dialog.setCancelable(false);
        dialog.setIndeterminate(true);
        dialog.show();
    }

    /** Hide the spinner if it’s visible. */
    public static void hide() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
        dialog = null;
    }
}
