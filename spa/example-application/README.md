## KTX SPA Example App
This app implements a tiny TODO app using KTX:SPA to show how such applications look.

It is compiled from Kotlin to JS using TeaVM to run client side, WASM would be possible too!

## Try me out!
execute this in the root dir of lifewire!
```
./gradlew :spa:example-application:distWebapp
```
This will generate a full webpage at `build/dist/webapp`