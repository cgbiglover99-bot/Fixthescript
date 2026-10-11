package com.lookupproduction.vesper

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File

object ScriptManager {

    private const val TAG = "VesperScriptManager"
    private const val BASE_DIR_NAME = "LookUpProductionStudios"

    val baseDir: File
        get() = File(Environment.getExternalStorageDirectory(), BASE_DIR_NAME)

    fun setupEnvironment(context: Context) {
        createDirectories()
        writeScripts()
    }

    private fun createDirectories() {
        val dirs = listOf(
            "Inbox",
            "Output",
            "Vault_Sync",
            "Legal",
            "Assets",
            "Scripts",
            "Assets/Music"
        )

        for (dirName in dirs) {
            val dir = File(baseDir, dirName)
            if (!dir.exists()) {
                val created = dir.mkdirs()
                Log.d(TAG, "Created directory $dirName: $created")
            }
        }
    }

    private fun writeScripts() {
        val scriptsDir = File(baseDir, "Scripts")

        // SCRIPT 1: vesper_research.py
        val researchScript = File(scriptsDir, "vesper_research.py")
        if (!researchScript.exists()) {
            researchScript.writeText("""
import sys
import requests
import os

# SCRIPT 1: vesper_research.py
# - Function: Takes a user query, checks the Gatekeeper status, performs a 10-second timeout web scrape using DuckDuckGo HTML, strips tracking, and saves a clean text file to /sdcard/LookUpProductionStudios/Assets/.
# - Correction Applied: Hard timeout added to prevent socket hanging on bad cellular connections.

def main():
    if len(sys.argv) < 3:
        print("Usage: vesper_research.py <query> <status>")
        return

    query = sys.argv[1]
    status = sys.argv[2]

    if status.lower() != "online":
        print("Gatekeeper is OFFLINE. Research aborted.")
        return

    print(f"Gatekeeper ONLINE. Researching: {query}")

    # Simulate a web scrape with 10-second timeout
    try:
        # Example using a standard HTML endpoint for DDG (no JS required)
        url = f"https://html.duckduckgo.com/html/?q={query}"
        headers = {'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'}
        response = requests.get(url, headers=headers, timeout=10)
        response.raise_for_status()

        # In a real scenario, you'd parse with BeautifulSoup here.
        # For this skeleton, we save raw HTML/Text.
        output_dir = "/sdcard/LookUpProductionStudios/Assets"
        os.makedirs(output_dir, exist_ok=True)

        output_file = os.path.join(output_dir, f"research_{query.replace(' ', '_')}.txt")
        with open(output_file, "w", encoding="utf-8") as f:
            f.write(response.text[:1000]) # Saving a snippet for demonstration

        print(f"Research saved to {output_file}")

    except requests.exceptions.Timeout:
        print("Error: Connection timed out. Cellular connection may be poor.")
    except Exception as e:
        print(f"Error during research: {e}")

if __name__ == "__main__":
    main()
            """.trimIndent())
        }

        // SCRIPT 2: vesper_render.py
        val renderScript = File(scriptsDir, "vesper_render.py")
        if (!renderScript.exists()) {
            renderScript.writeText("""
import os
import subprocess
import glob

# SCRIPT 2: vesper_render.py
# - Function: Executes FFmpeg commands to take raw .mp4 files from the /Vault_Sync directory, crops them to 9:16 aspect ratio (for shorts), injects an audio track from /Assets/Music, and outputs the final file to /Output.
# - Correction Applied: Wrapped in a Try/Except block to silently skip corrupted media files instead of crashing the thread.

def main():
    vault_dir = "/sdcard/LookUpProductionStudios/Vault_Sync"
    output_dir = "/sdcard/LookUpProductionStudios/Output"
    music_dir = "/sdcard/LookUpProductionStudios/Assets/Music"

    os.makedirs(output_dir, exist_ok=True)

    # Find input files
    input_files = glob.glob(os.path.join(vault_dir, "*.mp4"))

    # Find first available music track (if any)
    music_files = glob.glob(os.path.join(music_dir, "*.mp3"))
    music_args = []
    if music_files:
        music_args = ["-i", music_files[0], "-map", "0:v:0", "-map", "1:a:0", "-shortest"]

    for input_file in input_files:
        filename = os.path.basename(input_file)
        output_file = os.path.join(output_dir, f"rendered_{filename}")

        print(f"Processing: {filename}")

        # FFmpeg command to crop to 9:16 (assuming 1080p input: crop=607:1080:656:0)
        # This is a generic crop, adjust parameters as needed based on source resolution.
        ffmpeg_cmd = [
            "ffmpeg", "-y",
            "-i", input_file
        ] + music_args + [
            "-vf", "crop=ih*(9/16):ih",
            "-c:v", "libx264",
            "-c:a", "aac",
            output_file
        ]

        try:
            # Run FFmpeg, wait for it to complete
            process = subprocess.run(ffmpeg_cmd, capture_output=True, text=True, check=True)
            print(f"Successfully rendered: {output_file}")
        except subprocess.CalledProcessError as e:
            # Silently skip corrupted or failing media files
            print(f"Skipping {filename} due to FFmpeg error.")
            # For debugging, you could log e.stderr to a file here
            pass
        except FileNotFoundError:
             print("Error: FFmpeg not found in PATH. Make sure it is installed in Termux.")

if __name__ == "__main__":
    main()
            """.trimIndent())
        }

        // SCRIPT 3: vesper_legal.py
        val legalScript = File(scriptsDir, "vesper_legal.py")
        if (!legalScript.exists()) {
            legalScript.writeText("""
import sys
import os
from datetime import datetime

# SCRIPT 3: vesper_legal.py
# - Function: Generates a local text-based Model Release Form based on talent name and date, saving it to the /Legal directory. Checks for local 18+ compliance timestamps.

def main():
    if len(sys.argv) < 3:
        print("Usage: vesper_legal.py <talent_name> <dob_yyyy_mm_dd>")
        return

    talent_name = sys.argv[1]
    dob_str = sys.argv[2]

    try:
        dob = datetime.strptime(dob_str, "%Y-%m-%d")
        today = datetime.now()

        # Calculate age
        age = today.year - dob.year - ((today.month, today.day) < (dob.month, dob.day))

        if age < 18:
            print(f"Warning: {talent_name} is under 18 (Age: {age}). Guardian signature required.")
            compliance_status = "MINOR - REQUIRES GUARDIAN"
        else:
            print(f"Success: {talent_name} is 18 or older (Age: {age}).")
            compliance_status = "ADULT - COMPLIANT"

        legal_dir = "/sdcard/LookUpProductionStudios/Legal"
        os.makedirs(legal_dir, exist_ok=True)

        filename = os.path.join(legal_dir, f"Release_{talent_name.replace(' ', '_')}_{today.strftime('%Y%m%d')}.txt")

        with open(filename, "w", encoding="utf-8") as f:
            f.write(f"--- MODEL RELEASE FORM ---\n")
            f.write(f"Talent Name: {talent_name}\n")
            f.write(f"Date of Birth: {dob_str}\n")
            f.write(f"Compliance Status: {compliance_status}\n")
            f.write(f"Date of Agreement: {today.strftime('%Y-%m-%d %H:%M:%S')}\n\n")
            f.write("I hereby grant Look Up Production Studios the absolute and irrevocable right and permission...\n")
            f.write("[Standard Legal Boilerplate Here]\n")

        print(f"Legal form generated at: {filename}")

    except ValueError:
        print("Error: Invalid date format. Use YYYY-MM-DD.")

if __name__ == "__main__":
    main()
            """.trimIndent())
        }
    }
}
