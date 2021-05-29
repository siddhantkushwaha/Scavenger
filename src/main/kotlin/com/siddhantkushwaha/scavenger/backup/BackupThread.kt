package com.siddhantkushwaha.scavenger.backup

class BackupThread(
    private val gitHubUserName: String?
) : Thread() {

    override fun run() {
        while (true) {
            try {

                // sync GitHub data if username provided
                if (gitHubUserName != null) {
                    GitHub.index(gitHubUserName)
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }

            // sync every 24 hours
            sleep(24 * 3600 * 1000)
        }
    }
}