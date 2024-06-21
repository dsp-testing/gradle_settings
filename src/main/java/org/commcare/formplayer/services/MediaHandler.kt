package org.commcare.formplayer.services

import org.apache.juli.logging.LogFactory
import org.commcare.formplayer.objects.MediaMetadataRecord
import org.commcare.formplayer.objects.SerializableFormSession
import org.commcare.formplayer.services.MediaValidator.validateFile
import org.commcare.util.FileUtils
import org.javarosa.core.util.PropertyUtils
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Supporting methods to process and save media files on the filesystem
 */
class MediaHandler(val file: MultipartFile, val mediaMetaDataService: MediaMetaDataService) {

    companion object {
        val log = LogFactory.getLog(this.javaClass)

        private fun getMediaFilePath(parentDirPath: Path, fileId: String): Path {
            return Paths.get(parentDirPath.toString(), fileId)
        }

        @JvmStatic
        fun cleanMedia(parentDirPath: Path, fileIdWithExt: String, mediaMetaDataService: MediaMetaDataService): Boolean {
            val currentMedia = getMediaFilePath(parentDirPath, fileIdWithExt).toFile()
            val deleted = currentMedia.delete()
            val fileId = fileIdWithExt.substring(0, fileIdWithExt.indexOf("."))
            if (deleted) {
                try {
                    mediaMetaDataService.deleteByFileId(fileId)
                } catch (e: Exception) {
                    // just log, we don't want to crash even if delete fails
                    log.info("Could not delete media data record for file id $fileId")
                }
            } else {
                log.info("Could not delete media from filesystem at path $currentMedia")
            }
            return deleted
        }
    }

    /**
     * Saves file in the given parent directory
     */
    fun saveFile(
        parentDirPath: Path,
        session: SerializableFormSession,
        username: String,
        asUser: String?,
        domain: String,
        appId: String
    ): String {
        validateFile(file.inputStream, file.originalFilename, file.size)
        val fileId = PropertyUtils.genUUID()
        val parent = parentDirPath.toFile()
        parent.mkdirs()
        var fileIdWithExt = fileId
        val fileExtension = FileUtils.getExtension(file.originalFilename)
        fileExtension?.let { fileIdWithExt = "$fileId.$it" }
        val filePath = getMediaFilePath(parentDirPath, fileIdWithExt)
        val destinationFile = filePath.toFile()

        try {
            FileUtils.copyFile(file.inputStream, destinationFile)
        } catch (e: IOException) {
            throw IOException("Could not copy file to destination due to " + e.message, e)
        }
        val mediaMetaData = MediaMetadataRecord(
            fileId,
            filePath.toString(),
            session,
            fileExtension,
            file.size.toInt(),
            username,
            asUser,
            domain,
            appId
        )
        mediaMetaDataService.saveMediaMetaData(mediaMetaData)

        return fileIdWithExt
    }
}
