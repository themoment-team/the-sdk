package team.themoment.sdk.logging.logger

import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun Any.logger(): Logger = LoggerFactory.getLogger(this.javaClass)

fun logger(name: String): Logger = LoggerFactory.getLogger(name)

fun logger(clazz: Class<*>): Logger = LoggerFactory.getLogger(clazz)

fun Any.companionLogger(): Logger {
    val companionClass = this.javaClass
    val enclosingClass = companionClass.enclosingClass
    return if (enclosingClass != null) {
        LoggerFactory.getLogger(enclosingClass)
    } else {
        LoggerFactory.getLogger(companionClass)
    }
}
