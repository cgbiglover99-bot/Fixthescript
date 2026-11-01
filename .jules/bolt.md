## 2026-11-01 - [Jetpack Compose State Optimization]
**Learning:** High-frequency state updates like text input can cause full-screen recompositions if not isolated, and using `mutableStateOf(listOf())` creates O(n) reallocations for chat histories.
**Action:** Always isolate input states into separate composable functions to minimize recomposition blast radius, and use `mutableStateListOf` for O(1) appending in list states.
