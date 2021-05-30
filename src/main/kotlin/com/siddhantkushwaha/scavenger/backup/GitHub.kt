package com.siddhantkushwaha.scavenger.backup

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.siddhantkushwaha.scavenger.index.IndexApp
import java.net.URL
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import kotlin.io.path.createDirectories


object GitHub {

    // keep this same for any other agent too
    private const val pathClient = "scavenger"

    private const val nameAgent = "github"
    private const val typeGist = "gist"
    private const val typeRepo = "repo"

    private val gson = Gson()

    public fun index(username: String) {
        indexGists(username)
        indexRepos(username)
    }

    private fun getGists(username: String): Map<String, JsonObject> {
        val content = URL("https://api.github.com/users/$username/gists").readText()
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

        if (IndexApp.isIndexedRecently(gistPath.toString())) {
            println("Skipping gist [$gistId], since already indexed.")
            return 0
        }

        gistPath.toFile().deleteRecursively()

        val retCode = cloneRepoOrGist(gistUrl, gistPath)
        if (retCode > 0)
            return retCode

        var gistDescription = ""
        val descriptionJsonElement = gistData["description"]
        if (descriptionJsonElement != null && descriptionJsonElement != JsonNull.INSTANCE)
            gistDescription = descriptionJsonElement.asString

        IndexApp.indexDocumentsInDirectory(gistPath) { indexRequest ->
            indexRequest.dataSource = "${nameAgent}_${typeGist}"

            // overwrite with gist's description
            if (gistDescription.isNotEmpty())
                indexRequest.description = gistDescription
        }

        gistPath.toFile().deleteRecursively()
        return 0
    }

    private fun indexGists(username: String) {
        val clientGistPath = Paths.get(pathClient, nameAgent, typeGist)
        clientGistPath.createDirectories()

        val gistsData = getGists(username)
        for (gistData in gistsData) {
            indexGist(clientGistPath, gistData.key, gistData.value)
        }
    }

    private fun getRepos(username: String): Map<String, JsonObject> {
        val content = URL("https://api.github.com/users/$username/repos").readText()
        val jsonContent = gson.fromJson(content, JsonArray::class.java)
        return jsonContent.associate { je ->
            val jo = je.asJsonObject
            val repoName = jo.get("name").asString
            repoName to jo
        }
    }

    private fun indexRepo(clientRepoPath: Path, username: String, repoName: String, repoData: JsonObject): Int {
        val repoUrl = "https://github.com/$username/$repoName.git"
        val repoPath = Paths.get(clientRepoPath.toString(), repoName)

        if (IndexApp.isIndexedRecently(repoPath.toString())) {
            println("Skipping repo [$username/$repoName], since already indexed.")
            return 0
        }

        repoPath.toFile().deleteRecursively()

        val retCode = cloneRepoOrGist(repoUrl, repoPath)
        if (retCode > 0)
            return retCode

        var repoDescription = ""
        val descriptionJsonElement = repoData["description"]
        if (descriptionJsonElement != null && descriptionJsonElement != JsonNull.INSTANCE)
            repoDescription = descriptionJsonElement.asString

        IndexApp.indexDocumentsInDirectory(repoPath) { indexRequest ->
            indexRequest.dataSource = "${nameAgent}_${typeRepo}"

            // overwrite with repo's description
            if (repoDescription.isNotEmpty())
                indexRequest.description = repoDescription
        }

        repoPath.toFile().deleteRecursively()
        return 0
    }

    private fun indexRepos(username: String) {
        val clientRepoPath = Paths.get(pathClient, nameAgent, typeRepo, username)
        clientRepoPath.createDirectories()

        val repoDataAll = getRepos(username)
        for (repoData in repoDataAll) {
            indexRepo(clientRepoPath, username, repoData.key, repoData.value)
        }
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