package com.siddhantkushwaha.scavenger.backup

class BackupThread(
    private val gitHubUserName: String?
) : Thread() {

    override fun run() {
        while (true) {
            try {

                // sync GitHub data if username proviced
                if (gitHubUserName != null) {
                    GitHub.index(gitHubUserName)
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }

            // sync every 6 hours
            sleep(6 * 3600 * 1000)
        }
    }
}