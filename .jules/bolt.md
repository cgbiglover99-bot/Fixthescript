## 2024-10-23 - Jetpack Compose Recomposition & List Performance
**Learning:** High-frequency state updates like text inputs cause full-screen recompositions when placed inside large UI composables (like `VesperUI`), leading to UI lag. Also, `mutableStateOf(listOf())` recreates the entire list every append, which degrades performance as chat histories grow.
**Action:** Always isolate high-frequency state updates into separate composables to avoid full-screen recompositions. Use `mutableStateListOf` for O(1) list appends instead of recreating the list with `+`.
