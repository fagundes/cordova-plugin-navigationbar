# cordova-plugin-navigationbar-color

> The `NavigationBar` object provides some functions to control the Android device navigation bar.

## Installation

    cordova plugin add cordova-plugin-navigationbar-color

Preferences
-----------

#### config.xml

-  __NavigationBarBackgroundColor__ (color hex string, default value __#000000__). Color of navigation bar.

```xml
<preference name="NavigationBarBackgroundColor" value="#000000" />
```
        

- __NavigationBarLight__ (boolean, defaults to __false__). Change the color of the buttons in the navigation bar to black, use in light colors of the navigation bar (Android 8.0 or higher).

```xml
<preference name="NavigationBarLight" value="true" />
```

- __NavigationBarTransparent__ (boolean, defaults to __false__). Makes the navigation bar transparent and overlay it with the webview (Android 11.0 or higher).

```xml
<preference name="NavigationBarTransparent" value="true" />
```

Methods
-------
This plugin defines global `NavigationBar` object.

Although in the global scope, it is not available until after the `deviceready` event.

```js
document.addEventListener("deviceready", onDeviceReady, false);

function onDeviceReady()
{
    console.log(NavigationBar);
}
```

#### NavigationBar.backgroundColorByHexString

Set color of navigation bar by hex string.

```js
NavigationBar.backgroundColorByHexString(String colorHex, Boolean lightNavigationBar = false, Boolean transparentNavigationBar = false);
```

-  __colorHex__ Color hex string. Set the color of navigation bar.

-  __lightNavigationBar__ Change the color of the buttons in the navigation bar to black, use in light colors of the navigation bar (Android 8.0 or higher).

-  __transparentNavigationBar__ Makes the navigation bar transparent and overlay it with the webview (Android 11.0 or higher).

#### NavigationBar.backgroundColorByName

Set color of navigation bar by color name.

```js
NavigationBar.backgroundColorByName(String colorName, Boolean lightNavigationBar = false, Boolean transparentNavigationBar = false);
```

-  __colorName__ Color name. Set the color of navigation bar.
- - __Possible values__
- - `black`: Equals #000000
- - `darkGray`: Equals #A9A9A9
- - `lightGray`: Equals #D3D3D3
- - `white`: Equals #FFFFFF
- - `gray`: Equals #808080
- - `red`: Equals #FF0000
- - `green`: Equals #00FF00
- - `blue`: Equals #0000FF
- - `cyan`: Equals #00FFFF
- - `yellow`: Equals #FFFF00
- - `magenta`: Equals #FF00FF
- - `orange`: Equals #FFA500
- - `purple`: Equals #800080
- - `brown`: Equals #A52A2A

-  __lightNavigationBar__ Change the color of the buttons in the navigation bar to black, use in light colors of the navigation bar (Android 8.0 or higher).

-  __transparentNavigationBar__ Makes the navigation bar transparent and overlay it with the webview (Android 11.0 or higher).

#### NavigationBar.size

Get the width, height and position of the navigation bar, these values can change depending on whether the device is in portrait or landscape, you can use `window.addEventListener("resize", yourFunction);` to always have the navigation bar size updated.

```js
NavigationBar.size(function(size) {
    size = {
        width: int,
        height: int,
        widthInPixels: int,
        heightInPixels: int,
        position: string, // bottom, left and right
    };
});
```

#### NavigationBar.hide

Hide the navigation bar.

```js
NavigationBar.hide();
```

#### NavigationBar.show

Shows the navigation bar.

```js
NavigationBar.show();
```