---
layout: default
title:  "Screenshot"
parent: Sync requests
nav_order: 5
---

## Capture a screenshot

Capturing screenshots is done using the `ScreenCaptureRequest`. This request will check the remote protocol version and will fail if the
 format is unsupported.

```kotlin
val image = adb.execute(
    request = ScreenCaptureRequest(),
    serial = "emulator-5554" 
).toBufferedImage()

if (!ImageIO.write(image, "png", File("/tmp/screen.png"))) {
    throw IOException("Failed to find png writer")
}
```
