***How to use*** Launch the app to see a live temperature dashboard that streams new readings every two seconds. Watch summary stats update, inspect the trend chart, and pause or resume collection with a tap.

***Explanation*** Jetpack Compose observes TemperatureViewModel’s StateFlow, renders cards and a Canvas line chart, and keeps a scrollable log of the latest 20 readings. The view model generates random data on a coroutine and can be paused via state updates.

***AI reflection*** I asked AI for suggestions on combining Compose with StateFlow, drawing simple charts on a Canvas, and structuring a toggled UI.
