## 2023-10-27 - [Jetpack Compose State Optimization]
**Learning:** Jetpack Compose `mutableStateOf(listOf())` triggers O(N) reallocation and full-screen recompositions when lists update frequently. Typing in text fields linked to top-level composable state also forces expensive global recompositions.
**Action:** Always extract high-frequency state update components (like text inputs) into their own child composables, and use `mutableStateListOf()` for O(1) appending in stateful lists to avoid memory thrashing and unnecessary re-renders.
