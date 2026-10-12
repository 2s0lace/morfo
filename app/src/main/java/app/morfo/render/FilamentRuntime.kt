package app.morfo.render

import android.content.Context
import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
import com.google.android.filament.gltfio.AssetLoader
import com.google.android.filament.gltfio.ResourceLoader
import com.google.android.filament.gltfio.UbershaderProvider
import com.google.android.filament.utils.Utils
import kotlinx.coroutines.CoroutineScope

/**
 * Process-wide Filament objects. One engine, one set of glTF loaders and one model cache,
 * shared by every viewport so geometry is loaded at most once. Main-thread only.
 */
class FilamentRuntime(context: Context, scope: CoroutineScope) {

    val engine: Engine = Engine.create()
    private val materialProvider = UbershaderProvider(engine)
    private val assetLoader = AssetLoader(engine, materialProvider, EntityManager.get())
    private val resourceLoader = ResourceLoader(engine, true)

    val models = ModelRepository(context.assets, assetLoader, resourceLoader, scope)

    fun destroy() {
        models.destroy()
        resourceLoader.destroy()
        assetLoader.destroy()
        materialProvider.destroyMaterials()
        materialProvider.destroy()
        engine.destroy()
    }

    companion object {
        init {
            Utils.init()
        }
    }
}
