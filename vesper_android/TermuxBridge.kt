package com.lookupproduction.vesper

import android.content.Context
import android.content.Intent
import android.util.Log

object TermuxBridge {
    private const val TAG = "TermuxBridge"

    fun executePythonScript(context: Context, scriptPath: String, args: Array<String> = emptyArray()) {
        try {
            val intent = Intent("com.termux.RUN_COMMAND")
            intent.setClassName("com.termux", "com.termux.app.RunCommandService")
            intent.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/python")

            val intentArgs = arrayOf(scriptPath) + args
            intent.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", intentArgs)
            intent.putExtra("com.termux.RUN_COMMAND_BACKGROUND", true)

            // Since BACKGROUND is true, Termux won't start a foreground notification.
            // Using startService prevents ForegroundServiceDidNotStartInTimeException.
            context.startService(intent)
            Log.d(TAG, "Sent Termux intent for script: $scriptPath")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to send Termux intent", e)
        }
    }
}
