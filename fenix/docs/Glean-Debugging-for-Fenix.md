# Glean Debugging for Fenix

**Purpose:** To be able to test that pings are sending data as expected.

**You need:**

* An Android phone (note: this should work the same for the Android simulator)
* A cord to plug the android phone into your computer
* The Firefox app of your choice installed on the phone

## Setup

1. Plug in the Android phone to your computer (or start the Android Simulator).

2. Make sure you have usb debugging enabled on your phone.
	* Check for Settings > Developer Options
	* If you don’t see this:
		* Go to Settings > About Device
		* Tap the Build Number 7 times
		* This turns on Developer Mode
	* Go to Settings> Developer Options
		* Turn on the enable usb debugging option

3. Check that you have adb installed on your computer.
	* In the command line, run: `brew install android-platform-tools`
	* If it has installed properly, you should be able to run: `adb devices`
		* If you have the phone plugged in, you should see a device number under the List of Devices

## Tag Pings

In the command line, run:

```
adb shell am start -n org.mozilla.fenix/mozilla.telemetry.glean.debug.GleanDebugActivity --es debugViewTag label-for-your-pings
```

where "label-for-your-pings" is the string you want to name your pings.
If you want to change the app that you are testing data from,
you can replace `org.mozilla.fenix` with the appropriate string (see table below).
This will tag the pings coming in and allow you to find them in the Glean debugger.
This will only tag pings from the current session, so it is a good idea to run this first.

| String | App Version |
| ------ | ----------- |
| org.mozilla.fenix | Fenix Nightly |
| org.mozilla.firefox | Fenix |
| org.mozilla.firefox.beta | Fenix Beta |


Refer to the [Glean Dictionary](https://dictionary.telemetry.mozilla.org/apps/fenix?itemType=app_ids) for the application mappings.

## Manually Send Pings

Now we want to trigger the ping to manually send data to the Glean debugger (and not wait until the scheduled time).
In the command line run:

```
adb shell am start -n org.mozilla.fenix/mozilla.telemetry.glean.debug.GleanDebugActivity --es sendPing metrics
```

where `metrics` is the ping type (you can substitute `baseline`, `events` etc)
and again you can switch the app name if necessary.

If you want to see only metrics updated from your current session,
you can trigger a ping, then perform the actions you want to test,
then trigger another ping.
The second ping will only have data for those actions,
while the first one sent could include any leftover data that was scheduled to be sent later in the day.

## View pings

Now you can see the data from your pings here:
<https://debug-ping-preview.firebaseapp.com/>

## Reference

[The Glean Documentation on Debugging Android Applications](https://mozilla.github.io/glean/book/user/debugging/android.html)
