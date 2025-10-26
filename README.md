***How to use*** Run the app to watch lifecycle events stream in: every transition is logged in real time, highlighted by color, and you can toggle toast-like notifications or open the settings dialog from the top bar.
***Explanation*** Jetpack Compose observes LifeTrackerViewModel’s LiveData, renders the latest state, lists events in reverse chronological order, and uses a LifecycleEventObserver to record transitions with timestamps.
***AI reflection*** I use AI for wiring LiveData into Compose, mapping lifecycle callbacks to colors, and showing a snackbar whenever a new event arrives.
