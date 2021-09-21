package io.vlinx.duplicated.checker

import io.vlinx.detector.Constants
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.util.jar.JarEntry
import java.util.jar.JarFile


/**
 * @author vlinx <vlinx@vlinx.io>
 * @create 2021-09-21
 * @version 1.0.0
 */

class Checker(private val source: String) {

    private val tmpFolder = Files.createTempDirectory("classpath-checker")

    val duplicatedEntriesList = ArrayList<String>()
    val entriesJarMap = HashMap<String, String>()

    fun check() {
        check(File(source))
    }

    private fun check(source: File) {

        if (source.isDirectory) {
            source.listFiles().forEach {
                if (it.isDirectory) {
                    check(it)
                } else if (it.name.toLowerCase().endsWith(".jar")
                    || it.name.toLowerCase().endsWith(".zip")) {
                    checkJarFile(JarFile(it), true)
                }
            }
        }else{
            checkJarFile(JarFile(source), true)
        }
    }

    private fun checkJarFile(jarFile: JarFile, checkEmbed: Boolean) {
        jarFile.entries().asIterator().forEach {
            if (it.isDirectory) {
                return@forEach
            }

            if (it.name == "META-INF/MANIFEST.MF"
                || it.name == "BOOT-INF/classes/classpath.idx"
                || it.name == "BOOT-INF/classes/layers.idx"
                || it.name.toLowerCase().endsWith(".txt")
                || it.name.toLowerCase().endsWith(".md")
                || it.name.toLowerCase() == "plugin.xml"
                || it.name.toUpperCase() == "META-INF/DEPENDENCIES"
                || it.name.toUpperCase() == "META-INF/LICENSE"
                || it.name.toUpperCase() == "META-INF/NOTICE"
                || it.name.equals("META-INF/spring.tooling")
                || it.name.equals("META-INF/spring.schemas")
                || it.name.equals("META-INF/spring.handlers")
                || it.name.equals("META-INF/spring.factories")
                || it.name.toUpperCase().equals("META-INF/INDEX.LIST")
                || it.name == "module-info.class"
                || it.name.toUpperCase() == "LICENSE"
            ) {
                return@forEach
            }

            if (it.name.toLowerCase().endsWith(".jar")
                || it.name.toLowerCase().endsWith(".zip") && checkEmbed) {
                handleJarFileEntry(jarFile, it)
            } else {
                handleEntry(jarFile, it)
            }
        }
    }

    private fun handleEntry(jarFile: JarFile, entry: JarEntry) {
        val name = entry.name.removePrefix(Constants.BOOT_INF_PREFIX).removePrefix(Constants.WEB_INF_PREFIX)
        if (entriesJarMap.containsKey(name)) {
            val jarFileNames = "${entriesJarMap[name]} && ${
                jarFile.name.removePrefix(tmpFolder.toString()).removePrefix("/").removePrefix("\\")
            }"
            entriesJarMap[name] = jarFileNames
            if (!duplicatedEntriesList.contains(name)) {
                duplicatedEntriesList.add(name)
            }
        } else {
            entriesJarMap[name] = jarFile.name.removePrefix(tmpFolder.toString()).removePrefix("/").removePrefix("\\")
        }
    }

    private fun handleJarFileEntry(jarFile: JarFile, entry: JarEntry) {
        val target = "$tmpFolder${File.separator}${Paths.get(jarFile.name).fileName}${File.separator}${entry.name}"
        extractJarEntry(jarFile, entry, target)
        checkJarFile(JarFile(target), false)
    }

    private fun extractJarEntry(jarFile: JarFile, entry: JarEntry, target: String) {
        val bis = BufferedInputStream(jarFile.getInputStream(entry))
        Files.createDirectories(Paths.get(target).parent)

        bis.use {
            val bos = BufferedOutputStream(FileOutputStream(target))
            bos.use {
                val buffer = ByteArray(1024)
                while (bis.read(buffer) != -1) {
                    bos.write(buffer)
                }
                bos.flush()
            }
        }
    }
}