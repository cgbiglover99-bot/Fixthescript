## 2024-05-24 - Main Thread I/O Blockers
**Learning:** Initializing the environment (`ScriptManager.setupEnvironment`) performs synchronous file system I/O (directory and file creation). Running this directly in `MainActivity.onCreate` blocks the UI thread, causing potential ANR (Application Not Responding) errors, especially on slower storage.
**Action:** Always wrap heavy I/O operations, such as environment setup and file handling, in `lifecycleScope.launch(Dispatchers.IO) { ... }` or similar background thread constructs during Activity initialization to ensure a responsive UI.
