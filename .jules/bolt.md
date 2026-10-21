## 2023-10-21 - Jetpack Compose Recomposition Optimization
**Learning:** High-frequency state updates like text inputs can cause full-screen recompositions if not isolated. Also, using `mutableStateOf(listOf())` for list appends results in O(n) array copy overhead on every message, while `mutableStateListOf()` offers O(1) appends and better Compose tracking.
**Action:** Always isolate high-frequency state updates (like text inputs) into separate composables. Use `mutableStateListOf()` for dynamic lists instead of `mutableStateOf(listOf())`.
