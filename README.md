***How to use*** Launch the app to get “Counter++”: tap +/−/Reset to adjust the value, toggle Auto to let it tick up on its own, and hop into the settings screen to change the auto increment interval.

***Explanation*** A shared CounterViewModel exposes StateFlow values for the count, auto mode, and interval; two Compose screens (counter + settings) collect that state, dispatch CounterActions, and share navigation via a single NavHost.

***AI reflection*** I use AI suggestions for wiring collectAsStateWithLifecycle, structuring a shared ViewModel across destinations, and sketching the auto-increment coroutine and interval picker.
