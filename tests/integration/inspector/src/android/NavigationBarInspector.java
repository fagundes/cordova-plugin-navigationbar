package com.viniciusfagundes.cordova.plugin.navigationbar.test;

import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
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
                View decorView = window.getDecorView();
                int uiOptions = decorView.getSystemUiVisibility();
                JSONObject state = new JSONObject();

                try {
                    boolean navigationBarHidden =
                            (uiOptions & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) != 0;
                    boolean lightNavigationBar = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                            && (uiOptions & View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR) != 0;

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        WindowInsets insets = decorView.getRootWindowInsets();
                        if (insets != null) {
                            navigationBarHidden = !insets.isVisible(
                                    WindowInsets.Type.navigationBars());
                        }

                        WindowInsetsController controller = window.getInsetsController();
                        if (controller != null) {
                            lightNavigationBar = (controller.getSystemBarsAppearance()
                                    & WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS) != 0;
                        }
                    }

                    state.put("sdk", Build.VERSION.SDK_INT);
                    state.put("navigationBarHidden", navigationBarHidden);
                    state.put("lightNavigationBar", lightNavigationBar);
                    state.put("statusBarFullscreen",
                            (window.getAttributes().flags
                                    & WindowManager.LayoutParams.FLAG_FULLSCREEN) != 0);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        state.put("navigationBarColor", window.getNavigationBarColor());
                    } else {
                        state.put("navigationBarColor", JSONObject.NULL);
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        state.put("navigationBarContrastEnforced",
                                window.isNavigationBarContrastEnforced());
                    } else {
                        state.put("navigationBarContrastEnforced", JSONObject.NULL);
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
