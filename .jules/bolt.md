## 2024-05-24 - Jetpack Compose List Recomposition
**Learning:** For Jetpack Compose performance in this codebase, using `mutableStateOf(listOf())` for the chat history resulted in O(N) list copying overhead for every message append.
**Action:** Use `mutableStateListOf` for lists that are frequently appended to, achieving O(1) appends and better recomposition performance, while keeping the UI responsive.
