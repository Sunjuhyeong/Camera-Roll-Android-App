[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Travis-ci](https://api.travis-ci.org/kollerlukas/Camera-Roll-Android-App.svg)](https://travis-ci.org/kollerlukas/Camera-Roll-Android-App)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/abf5a5e744c34396b20c1f7ed125ff04)](https://www.codacy.com/app/lukaskoller6/Camera-Roll-Android-App?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=kollerlukas/Camera-Roll-Android-App&amp;utm_campaign=Badge_Grade)

![PREVIEW](https://github.com/kollerlukas/Camera-Roll-Android-App/blob/master/camera_roll_banner.png)

# Camera Roll Android App
Simple Gallery App for Android, with lovely Material Design.<br>

<a href="https://play.google.com/store/apps/details?id=us.koller.cameraroll" target="_blank">
<img src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png" alt="Get it on Google Play" height="80"/></a>

[<img src="https://f-droid.org/badge/get-it-on.png"
      alt="Get it on F-Droid"
      height="80">](https://f-droid.org/app/us.koller.cameraroll)



# Search pictures in a gallery (Added by Sunjuhyeong, Yunyoungjun)
 Add Search function in Gallery.
 
 Searching by keyword, texts and person face is supported.
 
 
 
## Search by keword
 Microsoft Azure's Computer Vision API (describe) attaches relevant tags to each picture.
 Import the package "edmt.dev.edmtdevcognitivevision" for using the API.
 
 
## Search by text
 Microsoft Azure's Computer Vision API (OCR) recognizes and saves the text in each picture.
 Import the package "edmt.dev.edmtdevcognitivevision" for using the API.
 
 
## Search by face
 Microsoft Azure's Face API detect faces in each picture. Using the API, save each person to LargePersonGroup in the API's server, and verify new face in the LargePersonGroup.
 Import the package "com.microsoft.projectoxford.face" for using the API.