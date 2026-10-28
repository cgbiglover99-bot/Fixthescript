
## 2024-05-14 - [Jetpack Compose UI Performance Optimization]
**Learning:** High-frequency state updates (like text inputs in `inputText`) should be isolated into separate composables to avoid full-screen recompositions. Additionally, when managing dynamic lists in state like `chatHistory`, `mutableStateListOf()` provides O(1) appends, avoiding the overhead of list copying inherent in `mutableStateOf(listOf())`.
**Action:** Isolate input states into their own nested composables and always prefer `mutableStateListOf()` for dynamically growing state lists in Compose.
