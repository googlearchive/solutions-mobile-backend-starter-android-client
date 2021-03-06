![status: inactive](https://img.shields.io/badge/status-inactive-red.svg)

This project is no longer actively developed or maintained.  

For new work on this check out [Mobile Shopping Assistant](https://github.com/GoogleCloudPlatform/MobileShoppingAssistant-sample) or [Cloud Endpoints](https://cloud.google.com/endpoints/).

solutions-mobile-backend-starter-android-client
=====================================

## Warning: No Longer Supported

The mobile backend starter sample application is no longer supported.  If you are looking for an easy way to connect your mobile app to Google Cloud Platform, we recommend you check out [Cloud Endpoints](https://cloud.google.com/endpoints/).  If you are already using the mobile backend starter your code will continue to work and no modifications are necessary at this time.

## Description

This project is Android native client sample for Mobile Backend Starter.

Disclaimer: This is not an official Google Product.

## Products
- [App Engine][1]
- [Android][2]

## Language
- [Java][3]

## APIs
- [Google Cloud Endpoints][4]

## Setup Instructions
The instruction below lists just some key steps.
For detailed setup instructions and documentation visit [Google App Engine developer site] (https://developers.google.com/cloud/samples/mbs).

1. Make sure you have Android SDK with Google APIs level 15 or above installed.

2. Import the project into Eclipse.

3. Make sure you have Google APIs selected in your project properties. This option is under Android in Project Build Target.

4. Update the value of `PROJECT_ID` in
   `src/com/google/cloud/backend/android/Consts.java` to the app_id of your
   deployed Mobile Backend [5]. Make sure that your Mobile Backend is configured
   with OPEN mode. Also update your `PROJECT_NUMBER` and `WEB_CLIENT_ID` with the values from your console project.

5. Update the value of `DEFAULT_ROOT_URL` in
   `endpoint-libs/libmobilebackend-v1/mobilebackend/mobilebackend-v1-generated-source/Mobilebackend.java` to your own project.

6. Run the application.

[1]: https://developers.google.com/appengine
[2]: http://developer.android.com/index.html
[3]: http://java.com/en/
[4]: https://developers.google.com/appengine/docs/java/endpoints/
[5]: https://github.com/GoogleCloudPlatform/solutions-mobile-backend-starter-java

