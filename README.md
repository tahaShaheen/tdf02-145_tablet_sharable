# TDF02-145 Tablet {#mainpage}

## Introduction

This is the repository for Android (initially exported from Unity) app related matters for robot for Autism Spectrum Disorder therapy development funded through HEC TDF. The documentation folder contains all the code for Android remote division of HEC funded project TDF 02-145. There are multiple files contained in this folder. This code will run on most Android phones and Tablets.

## JAVA

This section details the procedure of exporting and running a Unity program in the Android Studio IDE and running it with the JAVA code.

### Exporting to JAVA inside Unity

* Go to File > Build Settings... or press Ctrl + Shift + B
* Select Android and click on Switch Platform
* Enable the Export Project Option. The "Build" option will change to "Export".
* Click on Export.
* Navigate to the TDF02-145 Tablet folder. Click on Select Folder.

### Opening in Android Studio

* This folder can now be opening in the Android Studio IDE
* Select "Use Android Studio's SDK"
* MainActivity.java is named UnityPlayerActivity.java

#### Note

> Exporting for the first time you may see the following comment <br/><b>"// GENERATED BY UNITY. REMOVE THIS COMMENT TO PREVENT OVERWRITING WHEN EXPORTING AGAIN"</b>.<br/> Delete this line. Otherwise the next time you Export it'll overwrite anything you've written in JAVA.<br/><br/> Exporting after already having exported you'll see a UnityPlayerActivity.NEW in addition to the UnityPlayerActivity.java you already edited.<br/> You may delete this NEW file.

#### Author

Taha Shaheen

#### Version

chotuX
