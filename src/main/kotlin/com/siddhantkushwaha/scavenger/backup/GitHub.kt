package com.siddhantkushwaha.scavenger.backup

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.siddhantkushwaha.scavenger.index.IndexApp
import java.net.URL
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import kotlin.io.path.createDirectories


object GitHub {

    // keep this same for any other agent too
    private val pathClient = "scavenger_client"

    private val nameAgent = "github"
    private val typeGist = "gist"
    private val typeRepo = "repo"

    public fun index(username: String) {
        indexGists(username)
    }

    private fun indexGists(username: String) {
        val clientGistPath = Paths.get(pathClient, nameAgent, typeGist)
        clientGistPath.createDirectories()

        val gistsData = getGists(username)
        gistsData.forEach { gistId, gistData -> indexGist(clientGistPath, gistId, gistData) }
    }

    private fun getGists(username: String): Map<String, JsonObject> {
        val content = URL("https://api.github.com/users/$username/gists").readText()
        val gson = Gson()
        val jsonContent = gson.fromJson(content, JsonArray::class.java)
        return jsonContent.associate { je ->
            val jo = je.asJsonObject
            val gistId = jo.get("id").asString
            gistId to jo
        }
    }

    private fun indexGist(clientGistPath: Path, gistId: String, gistData: JsonObject): Int {
        val gistUrl = "https://gist.github.com/$gistId.git"
        val gistPath = Paths.get(clientGistPath.toString(), gistId)
        gistPath.toFile().deleteRecursively()

        val retCode = cloneRepoOrGist(gistUrl, gistPath)
        if (retCode > 0)
            return retCode

        IndexApp.indexDocumentsInDirectory(gistPath) { indexRequest ->
            indexRequest.dataSource = "${nameAgent}_${typeGist}"

            // overwrite with gist's description
            val gistDescription = gistData.get("description")?.asString ?: ""
            if (gistDescription.isNotEmpty())
                indexRequest.description = gistDescription
        }

        gistPath.toFile().deleteRecursively()
        return 0
    }

    private fun cloneRepoOrGist(url: String, path: Path): Int {
        var retCode = 1
        try {
            // TODO - track what's happening inside the sub-process
            // TODO - Or find a better way to clone
            val command = "git clone $url $path"
            val p = Runtime.getRuntime().exec(command)
            val res = p.waitFor(5, TimeUnit.MINUTES)
            if (res)
                retCode = 0
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return retCode
    }
}
