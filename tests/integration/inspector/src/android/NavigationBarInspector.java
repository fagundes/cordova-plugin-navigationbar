package com.viniciusfagundes.cordova.plugin.navigationbar.test;

import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class NavigationBarInspector extends CordovaPlugin {
    @Override
    public boolean execute(
            String action, JSONArray args, final CallbackContext callbackContext)
            throws JSONException {
        if (!"getState".equals(action)) {
            return false;
        }

        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Window window = cordova.getActivity().getWindow();
                int uiOptions = window.getDecorView().getSystemUiVisibility();
                JSONObject state = new JSONObject();

                try {
                    state.put("sdk", Build.VERSION.SDK_INT);
                    state.put("navigationBarHidden",
                            (uiOptions & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) != 0);
                    state.put("lightNavigationBar",
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                                    && (uiOptions & View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR) != 0);
                    state.put("statusBarFullscreen",
                            (window.getAttributes().flags
                                    & WindowManager.LayoutParams.FLAG_FULLSCREEN) != 0);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        state.put("navigationBarColor", window.getNavigationBarColor());
                    } else {
                        state.put("navigationBarColor", JSONObject.NULL);
                    }

                    callbackContext.success(state);
                } catch (JSONException exception) {
                    callbackContext.error("Unable to inspect the navigation bar");
                }
            }
        });

        return true;
    }
}
