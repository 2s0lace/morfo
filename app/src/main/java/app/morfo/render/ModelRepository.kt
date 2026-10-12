package app.morfo.render

import android.content.res.AssetManager
import android.util.Log
import com.google.android.filament.gltfio.AssetLoader
import com.google.android.filament.gltfio.FilamentAsset
import com.google.android.filament.gltfio.FilamentInstance
import com.google.android.filament.gltfio.ResourceLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Loads tooth GLBs lazily and hands out reusable instances.
 *
 * - File bytes are read off the main thread; parsing and GPU upload happen on the main thread.
 * - Each model is uploaded once; up to [INSTANCES_PER_ASSET] placed teeth share its vertex/index
 *   buffers and textures (e.g. 14, 24 and a duplicate all use t24). More copies get a second asset.
 * - Instances are recycled rather than destroyed (gltfio cannot destroy single instances).
 * - Textures are decoded asynchronously, one asset at a time; a tooth is shown only once complete.
 */
class ModelRepository(
    private val assets: AssetManager,
    private val assetLoader: AssetLoader,
    private val resourceLoader: ResourceLoader,
    private val scope: CoroutineScope,
) {
    private class Slot(val asset: FilamentAsset, val free: ArrayDeque<FilamentInstance>, var source: ByteBuffer?) {
        var ready = false
            set(value) {
                field = value
                if (value) source = null // resources are on the GPU; the GLB bytes are no longer needed
            }
    }

    private val bytes = HashMap<String, ByteBuffer>()
    private val reading = HashSet<String>()
    private val failed = HashSet<String>()
    private val slots = HashMap<String, MutableList<Slot>>()
    private val loadQueue = ArrayDeque<Slot>()
    private var loading: Slot? = null

    val isBusy get() = loading != null || loadQueue.isNotEmpty() || reading.isNotEmpty()

    /** Starts reading a model file in the background if it is not cached yet. */
    fun prefetch(id: String) {
        val hasCapacity = slots[id]?.any { !it.ready || it.free.isNotEmpty() } == true
        if (id in bytes || id in failed || hasCapacity || !reading.add(id)) return
        scope.launch {
            val buffer = runCatching { withContext(Dispatchers.IO) { readAsset("models/$id.glb") } }
                .onFailure { Log.e(TAG, "Cannot read model $id", it) }
                .getOrNull()
            if (buffer != null) bytes[id] = buffer else failed += id
            reading.remove(id)
        }
    }

    /** Returns a fully loaded instance, or null while it is still loading (call again next frame). */
    fun acquire(id: String): FilamentInstance? {
        val modelSlots = slots.getOrPut(id) { mutableListOf() }
        modelSlots.firstOrNull { it.ready && it.free.isNotEmpty() }?.let { return it.free.removeLast() }
        if (modelSlots.any { !it.ready }) return null

        val buffer = bytes[id] ?: run { prefetch(id); return null }
        val instances = arrayOfNulls<FilamentInstance>(INSTANCES_PER_ASSET)
        val asset = assetLoader.createInstancedAsset(buffer, instances) ?: error("Cannot parse model $id")
        val slot = Slot(asset, ArrayDeque(instances.filterNotNull()), bytes.remove(id))
        modelSlots += slot
        loadQueue.addLast(slot)
        return null
    }

    fun release(instance: FilamentInstance) {
        slots.values.asSequence().flatten().firstOrNull { it.asset == instance.asset }?.free?.addLast(instance)
    }

    /** Drives asynchronous texture decoding. Call once per frame. */
    fun update() {
        loading?.let { slot ->
            resourceLoader.asyncUpdateLoad()
            if (resourceLoader.asyncGetLoadProgress() >= 1f) {
                slot.ready = true
                loading = null
            }
        }
        if (loading == null) {
            val next = loadQueue.removeFirstOrNull() ?: return
            if (resourceLoader.asyncBeginLoad(next.asset)) loading = next else next.ready = true
        }
    }

    fun destroy() {
        if (loading != null) resourceLoader.asyncCancelLoad()
        slots.values.flatten().forEach { assetLoader.destroyAsset(it.asset) }
        slots.clear()
    }

    private fun readAsset(path: String): ByteBuffer {
        val data = assets.open(path).use { it.readBytes() }
        return ByteBuffer.allocateDirect(data.size).order(ByteOrder.nativeOrder()).put(data).also { it.flip() }
    }

    companion object {
        const val INSTANCES_PER_ASSET = 4
        private const val TAG = "ModelRepository"
    }
}
