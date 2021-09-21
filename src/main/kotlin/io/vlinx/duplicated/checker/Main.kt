package io.vlinx.duplicated.checker

import io.vlinx.detector.Constants
import io.vlinx.logging.Logger
import kotlin.system.exitProcess

fun main(args: Array<String>) {

    if(args.isEmpty()){
        println("Usage: java -jar duplicated-checker.jar folder/jar-file")
        exitProcess(1)
    }

    Logger.INFO("Java Duplicated Entries Checker ${Constants.VERSION}")

    val checker = Checker(args[0])
    Logger.INFO("Checking......")
    checker.check()
    for(entry in checker.duplicatedEntriesList){
        Logger.INFO("$entry: ${checker.entriesJarMap[entry]}")
    }

}


