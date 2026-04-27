# Safe-Stride

Safe-Stride is an Android application designed to assist visually impaired individuals by acting as a smart mobility aid. Using real-time object detection and spatial analysis, it scans the user's environment to identify obstacles, calculate their distance, and provide immediate auditory and haptic feedback.

## Features
- **Real-Time Object Detection**: Uses a custom TensorFlow Lite model (`detect.tflite`) to accurately detect objects in the camera's field of view in real time.
- **Spatial Awareness**: Determines the position of the object (left, right, or directly ahead) and estimates its distance from the user.
- **Auditory Feedback**: Integrates with Android's Text-to-Speech (TTS) engine to verbally warn the user about obstacles and guide them to navigate safely.
- **Haptic Alerts**: Triggers strong device vibrations when an obstacle is critically close (< 1 meter) to ensure immediate user response.
- **High-Contrast UI**: Provides a clean, dark-themed, and highly readable visual interface, beneficial for partially sighted users or caretakers monitoring the app's status.

## Technologies Used
- **Kotlin**: The primary programming language used for Android development.
- **CameraX**: For handling the camera preview and capturing frames for the image analysis pipeline.
- **TensorFlow Lite**: Powers the on-device, low-latency machine learning inference for object detection.
- **Android Text-To-Speech (TTS)**: Delivers continuous auditory cues and navigation instructions.

## Installation
1. Clone this repository to your local machine.
2. Open the project in **Android Studio**.
3. Let Gradle sync and resolve all dependencies.
4. Connect a physical Android device (emulators may not provide proper camera or hardware sensor support).
5. Build and run the application on your device.

## Usage
1. Open the "Safe-Stride" app on your Android device.
2. Grant the requested **Camera permissions** to allow the app to scan your surroundings.
3. Hold your device with the back camera facing forward as you walk.
4. Listen carefully for auditory cues like `"Path clear"` or `"Warning [Object] very close"`. Follow the spoken instructions (e.g., `"move left"`, `"move right"`) to navigate safely around any detected obstacles.

## License
[MIT License](LICENSE) (or specify the license under which this project is distributed).
