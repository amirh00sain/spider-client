package dev.spider.spider.platform

actual fun platformLog(tag: String, message: String) {
    android.util.Log.i(tag, message)
    InAppLog.write('I', tag, message, null, android.os.Process.myPid())
}
