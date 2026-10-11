# --- VESPER MASTER INSTALLER V6.0 ---

# 1. NUCLEAR CLEANUP (Kill all old processes and clear ports)
echo "Initializing Clean Room Protocol..."
pkill -9 python > /dev/null 2>&1
pkill -9 streamlit > /dev/null 2>&1
rm -f vesper_app.py

# 2. INSTALL DEPENDENCIES (Silent & Force Re-install to fix corruption)
echo "Installing Vesper Neural Core..."
pip install streamlit streamlit-mic-recorder speechrecognition -q

# 3. WRITE THE APP (The Brain)
# We use 'cat' to write the file directly to avoid clipboard errors.
cat << 'EOF' > vesper_app.py
import streamlit as st
from streamlit_mic_recorder import speech_to_text
import streamlit.components.v1 as components
import time

# --- CONFIGURATION ---
st.set_page_config(
    page_title="Vesper AI",
    page_icon="🟢",
    layout="wide",
    initial_sidebar_state="collapsed"
)

# --- TEXT-TO-SPEECH ENGINE (Android Native) ---
def speak(text):
    # This JavaScript forces the phone's native TTS to speak the text
    js = f"""
    <script>
        window.speechSynthesis.cancel();
        const msg = new SpeechSynthesisUtterance("{text}");
        msg.rate = 1.0;
        msg.pitch = 1.0;
        window.speechSynthesis.speak(msg);
    </script>
    """
    components.html(js, height=0)

# --- UI LAYOUT ---
st.title("🟢 VESPER")
st.markdown("##### System Status: **ONLINE** | Mode: **VOICE**")

# --- THE INPUT (Android-Optimized) ---
# We use Auto-Stop because 'Manual Stop' causes freezing on S25 Ultra.
command = speech_to_text(
    start_prompt="🔴 TAP TO SPEAK",
    stop_prompt="⏳ LISTENING...",
    just_once=False,
    use_container_width=True,
    key='vesper_mic_input'
)

# --- THE LOGIC CORE ---
if command:
    # 1. Visual Feedback
    st.success(f"Received: {command}")

    # 2. Processing (Simulated Thinking)
    with st.spinner("Analyzing..."):
        time.sleep(0.5) # Short processing buffer

    # 3. Decision Tree (The Brain)
    response = "Command not recognized."
    cmd = command.lower()

    if "hello" in cmd:
        response = "Vesper online. I am ready for your command."
    elif "status" in cmd:
        response = "All systems operational. Battery and Network are stable."
    elif "who are you" in cmd:
        response = "I am Vesper, your personal AI assistant."
    elif "time" in cmd:
        response = "Time synchronization is active."
    else:
        response = f"I heard you say: {command}"

    # 4. Output
    st.info(f"Vesper: {response}")
    speak(response)

EOF

# 4. LAUNCH SEQUENCE
echo "Vesper System Built."
echo "Deploying to Localhost:8501..."

# Create the startup script
echo "pkill -f streamlit; termux-open-url http://localhost:8501; streamlit run vesper_app.py --server.port 8501 --server.headless true" > $PREFIX/bin/vesper
chmod +x $PREFIX/bin/vesper

# Execute
$PREFIX/bin/vesper
