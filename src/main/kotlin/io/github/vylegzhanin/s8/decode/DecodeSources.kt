package io.github.vylegzhanin.s8.decode

import java.io.File
import java.io.FileFilter
import java.io.IOException
import java.nio.file.Files
import javax.json.Json
import javax.json.JsonArray
import javax.json.JsonObject

fun main(args: Array<String>) {
    SourcesDirectoryDecoder(
        sources = File(args[0]).absoluteFile,
        target = File(args[1]).absoluteFile
    ).decodeToFiles()
}

private data class SourceData(
    val dataDir: File
)

private class SourcesDirectoryDecoder(
    val sources: File,
    val target: File
) {
    fun decodeToFiles() {
        (sources
            .listFiles(FileFilter { it.isDirectory && it.resolve("source.json").isFile })
            ?.map { SourceData(it) }
                ?: emptyList())
            .forEach {
                it.copyTo(target)
            }
    }
}

private fun SourceData.copyTo(targetDir: File) {
    val json = dataDir.resolve("source.json").readAsJson()
    val title0 = json.getString("title", dataDir.name)
    (json["FOVs"] as? JsonArray)?.forEachIndexed { index, fov ->
        (((fov as? JsonObject)?.get("sourceImages") as? JsonArray)?.firstOrNull() as? JsonObject)?.let { sourceImage ->
            val id = sourceImage.getString("id") ?: throw IOException("id not found in source.json: $dataDir")
            val title = sourceImage.getString("title", "$index")
            val dataFile = dataDir.resolve(id).resolve("data.svs")
            var targetFile = targetDir.resolve("$title0 $title.svs".safeFileName)
            if (targetFile.exists()) {
                for (f in 1..999) {
                    targetFile = targetDir.resolve("$title0 $title ($f).svs".safeFileName)
                    if (!targetFile.exists()) break
                }
                if (targetFile.exists()) throw IOException("File $targetFile already exists")
            }
            Files.createLink(targetFile.toPath(), dataFile.toPath())
        }
    }
}

private val String.safeFileName: String
    get()  = replace("[\\\\/:\"]".toRegex(), "_")


private fun File.readAsJson() = Json.createReader(bufferedReader()).use { it.readObject() }
