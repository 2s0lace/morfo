import Foundation
import RealityKit

/// Loads tooth GLBs lazily and shares their GPU resources between every placed copy.
///
/// - File parsing runs off the main actor; mesh and texture upload happen on it.
/// - Each model is loaded once; all teeth using it (e.g. 14, 24 and a duplicate all use t24)
///   share the same materials. Right-side teeth use a mirrored copy of the mesh: a negative scale
///   would flip their normals and break the lighting.
final class ModelRepository {
    struct Resources {
        let mesh: MeshResource
        /// Mirror image for right-side teeth (only left-side teeth were scanned).
        let mirroredMesh: MeshResource
        let textured: PhysicallyBasedMaterial
        /// Plain enamel colour without the scan texture (quiz option), so only the shape is visible.
        let uniform: PhysicallyBasedMaterial
    }

    private var cache: [String: Resources] = [:]
    private var loading: Set<String> = []
    private var failed: Set<String> = []

    var isBusy: Bool { !loading.isEmpty }

    /// Returns the model if loaded, otherwise starts loading it and returns nil (ask again next frame).
    func resources(_ id: String) -> Resources? {
        if let r = cache[id] { return r }
        prefetch(id)
        return nil
    }

    func prefetch(_ id: String) {
        guard cache[id] == nil, !failed.contains(id), !loading.contains(id),
              let url = Bundle.main.url(forResource: id, withExtension: "glb") else { return }
        loading.insert(id)
        Task {
            defer { loading.remove(id) }
            do {
                let (glb, mirrored) = try await Task.detached(priority: .userInitiated) {
                    let glb = try GLBLoader.read(url: url)
                    return (glb, glb.mirroredX())
                }.value
                cache[id] = try await Self.upload(glb, mirrored: mirrored)
            } catch {
                failed.insert(id)
                print("Cannot load model \(id): \(error)")
            }
        }
    }

    private static func mesh(_ glb: GLBMesh) async throws -> MeshResource {
        var descriptor = MeshDescriptor(name: "tooth")
        descriptor.positions = MeshBuffers.Positions(glb.positions)
        if !glb.normals.isEmpty { descriptor.normals = MeshBuffers.Normals(glb.normals) }
        if !glb.uvs.isEmpty { descriptor.textureCoordinates = MeshBuffers.TextureCoordinates(glb.uvs) }
        descriptor.primitives = .triangles(glb.indices)
        return try await MeshResource(from: [descriptor])
    }

    private static func upload(_ glb: GLBMesh, mirrored: GLBMesh) async throws -> Resources {
        let mesh = try await Self.mesh(glb)
        let mirroredMesh = try await Self.mesh(mirrored)

        var base = PhysicallyBasedMaterial()
        base.roughness = .init(floatLiteral: glb.roughness)
        base.metallic = .init(floatLiteral: 0)
        // The glTF material is double-sided.
        base.faceCulling = .none

        var uniform = base
        uniform.baseColor = .init(tint: .init(red: 0.86, green: 0.84, blue: 0.79, alpha: 1))

        var textured = base
        if let image = glb.baseColor {
            let texture = try await TextureResource(image: image, options: .init(semantic: .color))
            textured.baseColor = .init(texture: .init(texture))
        } else {
            textured.baseColor = uniform.baseColor
        }
        return Resources(mesh: mesh, mirroredMesh: mirroredMesh, textured: textured, uniform: uniform)
    }
}
