package space.taran.arkbrowser.mvp.model.repo

import android.util.Log
import space.taran.arkbrowser.mvp.model.entity.room.ResourceDao
import space.taran.arkbrowser.mvp.model.repo.PlainResourcesIndex.Companion.groupResources
import space.taran.arkbrowser.mvp.model.repo.PlainResourcesIndex.Companion.listAllFiles
import space.taran.arkbrowser.mvp.model.repo.PlainResourcesIndex.Companion.scanResources
import space.taran.arkbrowser.utils.CoroutineRunner
import space.taran.arkbrowser.utils.RESOURCES_INDEX
import java.nio.file.Path
import kotlin.system.measureTimeMillis

class ResourcesIndexFactory(private val dao: ResourceDao) {
    fun loadFromDatabase(root: Path): PlainResourcesIndex {
        Log.d(RESOURCES_INDEX, "loading index for $root from the database")

        val resources = CoroutineRunner.runAndBlock {
            dao.query(root.toString())
        }
        Log.d(RESOURCES_INDEX, "${resources.size} resources retrieved from DB")

        val index = PlainResourcesIndex(root, dao, groupResources(resources))

        index.reindexRoot(index.calculateDifference())
        return index
    }

    fun buildFromFilesystem(root: Path): PlainResourcesIndex {
        Log.d(RESOURCES_INDEX, "building index from root $root")

        var files: List<Path>

        val time1 = measureTimeMillis {
            files = listAllFiles(root)
        }
        Log.d(RESOURCES_INDEX, "listed ${files.size} files, took $time1 milliseconds")

        var metadata: Map<Path, ResourceMeta>

        val time2 = measureTimeMillis {
            metadata = scanResources(files)
        }
        Log.d(RESOURCES_INDEX, "hashes calculation took $time2 milliseconds")

        val index = PlainResourcesIndex(root, dao, metadata)

        index.persistResources(index.metaByPath)
        return index
    }
}