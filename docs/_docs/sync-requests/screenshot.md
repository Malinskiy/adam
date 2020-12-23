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
val adapter = RawImageScreenCaptureAdapter()
val image = adb.execute(
    request = ScreenCaptureRequest(adapter),
    serial = "emulator-5554" 
).toBufferedImage()

if (!ImageIO.write(image, "png", File("/tmp/screen.png"))) {
    throw IOException("Failed to find png writer")
}
```
 
### Image adapter
In order to receive the image you'll have to transform the framebuffer bytes into something meaningful. There are two options here:
`RawImageScreenCaptureAdapter` and `BufferedImageScreenCaptureAdapter`. The `RawImageScreenCaptureAdapter` is a bare minimum to receive the
 necessary metadata as well as the `byte[]` that holds the screenshot. The return type of this adapter is `RawImage` that supports
  retrieving the pixel value by index using `RawImage#getARGB(index: Int)`. You can also transform the image into Java's `BufferedImage`.
  
However, if you intend to capture a lot of screenshots for a particular device, consider using the `BufferedImageScreenCaptureAdapter` that
 will reduce additional allocations of memory when transforming the image.
 
Please note, that all adapter by default will try to reduce the memory consumption and reuse the internal buffers. If you're using the same
 adapter on multiple threads in parallel either set the buffer to `null` all the time or provide an external buffer that is allocated per
  thread.
