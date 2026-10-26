
## 2026-10-26 - [Jetpack Compose Recomposition & List Opts]
**Learning:** High-frequency state updates like typing in Jetpack Compose cause full-screen recompositions if not isolated. Also, appending to a state list using `+` operator in `mutableStateOf(listOf())` is O(N) and triggers full state reallocations, which scales poorly.
**Action:** Always isolate high-frequency input state into separate composables to localize recomposition. Use `mutableStateListOf()` for O(1) in-place list appends instead of generating new lists on every append.
