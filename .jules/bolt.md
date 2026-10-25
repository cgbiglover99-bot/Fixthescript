
## 2024-05-14 - Jetpack Compose Performance: Isolating High-Frequency State and O(1) List Appends
**Learning:** In the Jetpack Compose architecture of this codebase, using `mutableStateOf(listOf())` for collections causes full recompositions and O(N) list copying on append operations. Furthermore, leaving high-frequency state updates like text inputs (`TextField`) hoisted in large parent composables (like `VesperUI`) causes severe UI jank by triggering full-screen recompositions on every keystroke.
**Action:** Always isolate high-frequency state updates (like text inputs) into separate, targeted composables to prevent full-screen recompositions. Additionally, use `mutableStateListOf` instead of `mutableStateOf(listOf())` for collections to ensure O(1) list appends.
