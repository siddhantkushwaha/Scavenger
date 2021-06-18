package com.siddhantkushwaha.scavenger

import com.siddhantkushwaha.scavenger.backup.BackupThread
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ScavengerApplication

fun main(args: Array<String>) {
    runApplication<ScavengerApplication>(*args)

    // this is configured to run every 6 hours
    // runSyncThread()
}

fun runSyncThread() {
    val backupThread = BackupThread(
        gitHubUserName = "siddhantkushwaha"
    )
    backupThread.start()
}
