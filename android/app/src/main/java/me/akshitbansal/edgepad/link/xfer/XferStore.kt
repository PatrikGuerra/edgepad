package me.akshitbansal.edgepad.link.xfer

import android.annotation.TargetApi
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import java.io.File
import java.io.IOException

/** Where a transfer's bytes wait and where they end up. A fake in the tests, [AndroidXferStore] on a phone. */
interface XferStore {
    /** Bytes a transfer may use, counting that the part and the published copy exist together for a moment. */
    fun freeBytes(): Long

    /** The part file for a file of this name and size: the same one for the same pair, so a resume finds it. */
    fun part(
        name: String,
        size: Long,
    ): File

    /** Deletes parts last written before [cutoffMs]. */
    fun purge(cutoffMs: Long)

    /** Copies [part] under a name that clashes with nothing, then deletes it. The name used, or null on failure. */
    fun publish(
        part: File,
        name: String,
    ): String?
}

/** Parts in the app-private `incoming` folder; the file itself in Download/Edgepad through MediaStore, at the end. */
class AndroidXferStore(
    private val context: Context,
) : XferStore {
    private val incoming = File(context.filesDir, "incoming")

    override fun freeBytes(): Long = StatFs(context.filesDir.path).availableBytes / 2

    override fun part(
        name: String,
        size: Long,
    ): File {
        if (!incoming.isDirectory && !incoming.mkdirs()) throw IOException("No incoming folder")
        return File(incoming, "$name.$size${XferLimits.PARTIAL_EXTENSION}")
    }

    override fun purge(cutoffMs: Long) {
        incoming
            .listFiles { f -> f.name.endsWith(XferLimits.PARTIAL_EXTENSION) && f.lastModified() < cutoffMs }
            ?.forEach { it.delete() }
    }

    override fun publish(
        part: File,
        name: String,
    ): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return publishLegacy(part, name)
        val resolver = context.contentResolver
        val final = SafeName.unique(name) { taken(it) }
        val values =
            ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, final)
                put(MediaStore.MediaColumns.RELATIVE_PATH, FOLDER)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
        return try {
            val out = resolver.openOutputStream(uri) ?: throw IOException("No output stream")
            out.use { sink -> part.inputStream().use { it.copyTo(sink, XferLimits.BUFFER_BYTES) } }
            resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
            part.delete()
            // MediaStore renames a clash `taken` cannot see (a file of an earlier install): report the name it kept.
            displayName(uri) ?: final
        } catch (e: IOException) {
            resolver.delete(uri, null, null)
            null
        } catch (e: RuntimeException) {
            resolver.delete(uri, null, null)
            null
        }
    }

    /**
     * Before Android 10 MediaStore has no Downloads collection: the file goes to this app's own Download/Edgepad on
     * shared storage (Android/data/<package>/files), which needs no permission and shows in a file manager.
     */
    private fun publishLegacy(
        part: File,
        name: String,
    ): String? {
        val folder = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: return null, "Edgepad")
        if (!folder.isDirectory && !folder.mkdirs()) return null
        val final = SafeName.unique(name) { File(folder, it).exists() }
        return try {
            part.copyTo(File(folder, final))
            part.delete()
            final
        } catch (e: IOException) {
            null
        }
    }

    private fun displayName(uri: Uri): String? =
        context.contentResolver
            .query(uri, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME), null, null, null)
            ?.use { if (it.moveToFirst()) it.getString(0) else null }

    @TargetApi(Build.VERSION_CODES.Q)
    private fun taken(name: String): Boolean =
        context.contentResolver
            .query(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.MediaColumns._ID),
                "${MediaStore.MediaColumns.DISPLAY_NAME}=? AND ${MediaStore.MediaColumns.RELATIVE_PATH}=?",
                arrayOf(name, "$FOLDER/"),
                null,
            )?.use { it.count > 0 } ?: false

    private companion object {
        const val FOLDER = "Download/Edgepad"
    }
}
