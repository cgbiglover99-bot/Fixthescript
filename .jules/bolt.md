## 2024-05-24 - [Jetpack Compose State Optimization]
**Learning:** High-frequency state updates like text inputs cause full-screen recompositions if not isolated into separate composables, and `mutableStateOf(listOf())` leads to O(n) reallocations on every append.
**Action:** Use `mutableStateListOf` instead of `mutableStateOf(listOf())` for O(1) list appends, and isolate text inputs.
