# MyPulseAudio
Pulseaudio demo app for android.

Building
========
copy the sysroot directory without the include and share directories under built using the pulseaudio-android-ndk to the app/src/main/assets/ directory.

```
cp -r <pulseaudio-android-ndk>/ndk-x86_64/sysroot <MyPulseAudio>/app/src/main/assets
rm -rf <MyPulseAudio>/app/src/main/assets/sysroot/usr/{include,share}
 ```


Running
=======
fireup android studio and hit play and choose the x86_64 android image.
