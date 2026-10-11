# Look Up Production Studios - Vesper LLM Setup

To make Vesper 100% complete and fully functional offline, you need to set up the local "Brain" inside your Termux environment on your Galaxy S25 Ultra.

We use `llama.cpp` because it runs highly quantized models exceptionally fast on ARM CPUs.

### Step 1: Install Dependencies in Termux
Open Termux and run the following commands to install required build tools:
```bash
pkg update && pkg upgrade -y
pkg install clang wget git cmake make python -y
```

### Step 2: Download and Compile llama.cpp
Clone the repository and compile it for Android:
```bash
git clone https://github.com/ggerganov/llama.cpp
cd llama.cpp
make -j8
```

### Step 3: Download the "Vesper" Model
We recommend a highly compressed Llama-3 8B model (Q4_K_M) for the best balance of speed and intelligence on mobile.
Run this to download the model into a `models` directory:
```bash
mkdir -p models
wget -O models/vesper-llama-3-8b-instruct.Q4_K_M.gguf https://huggingface.co/NousResearch/Meta-Llama-3-8B-Instruct-GGUF/resolve/main/Meta-Llama-3-8B-Instruct-Q4_K_M.gguf
```

### Step 4: Start the Vesper Brain Server
The Android app is hardcoded to look for the LLM API at `http://127.0.0.1:8080`.
Run this command to start the background inference server:
```bash
./server -m models/vesper-llama-3-8b-instruct.Q4_K_M.gguf -c 2048 --host 127.0.0.1 --port 8080 -t 8 &
```

> **Note:** The `&` at the end runs the server in the background. You can now minimize Termux, open your Android app, and chat with Vesper natively!
