## 2024-05-18 - Jetpack Compose State Hoisting Gotcha
**Learning:** Hoisting a high-frequency state variable (like a `TextField`'s text) to the very top level of a complex Compose UI (like `VesperUI` which includes a heavy `LazyColumn`) causes the entire screen to recompose on every single keystroke. This is a severe performance bottleneck.
**Action:** Always isolate text input states by extracting the input UI into its own composable (e.g., `ChatInputArea`). Pass callbacks up instead of hoisting the state down.

## 2024-05-18 - Immutable Lists vs `mutableStateListOf`
**Learning:** Using `var history by remember { mutableStateOf(listOf<String>()) }` and updating it via `history = history + newMsg` creates a full deep copy of the array (O(N) operation). In a chat interface, this becomes increasingly expensive.
**Action:** Use `val history = remember { mutableStateListOf<String>() }`. It integrates perfectly with Compose's reactivity but allows O(1) in-place mutations (`history.add(newMsg)`).
