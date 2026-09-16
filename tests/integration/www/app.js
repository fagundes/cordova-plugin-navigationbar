"use strict";

(function () {
    var baselineStatusBarFullscreen;
    var lifecycleChecksEnabled = false;
    var pauseObserved = false;
    var rotationCheckPending = false;
    var rotationConfirmed = false;
    var visibilityCycles = 0;

    function finish(message) {
        document.getElementById("result").textContent = message;
        console.log(message);
    }

    function fail(message) {
        finish("NAVIGATIONBAR_TEST_FAIL: " + message);
    }

    function assert(condition, message) {
        if (!condition) {
            throw new Error(message);
        }
    }

    function inspect(success) {
        NavigationBarInspector.getState(success, function (message) {
            fail("inspector error: " + message);
        });
    }

    function afterUiChange(callback) {
        window.setTimeout(callback, 300);
    }

    function assertStatusBarUnchanged(state) {
        assert(state.statusBarFullscreen === baselineStatusBarFullscreen,
            "status bar fullscreen state changed");
    }

    function confirmRotation() {
        var orientation = Number(window.orientation);
        var isLandscape = Math.abs(orientation) === 90
            || window.screen.width > window.screen.height;

        if (rotationConfirmed || rotationCheckPending || !isLandscape) {
            return;
        }

        rotationCheckPending = true;
        inspect(function (state) {
            try {
                assertStatusBarUnchanged(state);
                rotationConfirmed = true;
                finish("NAVIGATIONBAR_TEST_ROTATED");
            } catch (error) {
                fail(error.message);
            }
        });
    }

    function pollForRotation() {
        if (!lifecycleChecksEnabled || rotationConfirmed) {
            return;
        }

        confirmRotation();
        window.setTimeout(pollForRotation, 250);
    }

    document.addEventListener("pause", function () {
        if (lifecycleChecksEnabled) {
            pauseObserved = true;
            console.log("NAVIGATIONBAR_TEST_PAUSED");
        }
    }, false);

    document.addEventListener("resume", function () {
        if (lifecycleChecksEnabled && pauseObserved) {
            afterUiChange(function () {
                inspect(function (state) {
                    try {
                        assert(rotationConfirmed,
                            "application resumed before rotation was confirmed");
                        assertStatusBarUnchanged(state);
                        finish("NAVIGATIONBAR_TEST_PASS");
                    } catch (error) {
                        fail(error.message);
                    }
                });
            });
        }
    }, false);

    window.addEventListener("orientationchange", function () {
        if (lifecycleChecksEnabled) {
            afterUiChange(function () {
                confirmRotation();
            });
        }
    }, false);

    window.addEventListener("resize", function () {
        if (lifecycleChecksEnabled) {
            confirmRotation();
        }
    }, false);

    function testShow(state) {
        assert(state.navigationBarHidden === true, "hide() did not hide navigation bar");
        assertStatusBarUnchanged(state);

        NavigationBar.show();
        afterUiChange(function () {
            inspect(function (shownState) {
                try {
                    assert(shownState.navigationBarHidden === false,
                        "show() did not show navigation bar");
                    assertStatusBarUnchanged(shownState);

                    visibilityCycles += 1;
                    if (visibilityCycles < 2) {
                        NavigationBar.hide();
                        afterUiChange(function () {
                            inspect(testShow);
                        });
                        return;
                    }

                    document.getElementById("keyboard-target").focus();
                    afterUiChange(function () {
                        inspect(function (keyboardState) {
                            try {
                                assertStatusBarUnchanged(keyboardState);
                                lifecycleChecksEnabled = true;
                                finish("NAVIGATIONBAR_TEST_READY");
                                pollForRotation();
                            } catch (error) {
                                fail(error.message);
                            }
                        });
                    });
                } catch (error) {
                    fail(error.message);
                }
            });
        });
    }

    function testHide(state) {
        try {
            assertStatusBarUnchanged(state);
            if (state.sdk >= 26) {
                assert(state.lightNavigationBar === true,
                    "light navigation bar flag was not set");
            }

            NavigationBar.hide();
            afterUiChange(function () {
                inspect(testShow);
            });
        } catch (error) {
            fail(error.message);
        }
    }

    function testColor(state) {
        try {
            assertStatusBarUnchanged(state);
            if (state.sdk >= 21) {
                assert((state.navigationBarColor >>> 0) === 0xff112233,
                    "navigation bar color was not applied");
            } else {
                assert(state.navigationBarColor === null,
                    "navigation bar color should be unavailable before API 21");
            }

            NavigationBar.backgroundColorByHexString("#fff", true);
            afterUiChange(function () {
                inspect(testHide);
            });
        } catch (error) {
            fail(error.message);
        }
    }

    document.addEventListener("deviceready", function () {
        inspect(function (initialState) {
            try {
                baselineStatusBarFullscreen = initialState.statusBarFullscreen;
                assert(initialState.navigationBarHidden === false,
                    "navigation bar was unexpectedly hidden initially");
                if (initialState.sdk >= 21) {
                    assert((initialState.navigationBarColor >>> 0) === 0xff123456,
                        "initial NavigationBarBackgroundColor was not applied");
                }
                if (initialState.sdk >= 26) {
                    assert(initialState.lightNavigationBar === false,
                        "initial dark icon preference was not applied");
                }

                NavigationBar.backgroundColorByHexString("#123", false);
                NavigationBar.backgroundColorByHexString("invalid", false);
                NavigationBar.backgroundColorByName("not-a-color", false);
                afterUiChange(function () {
                    inspect(testColor);
                });
            } catch (error) {
                fail(error.message);
            }
        });
    }, false);
}());
