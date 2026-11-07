import CoreGraphics
import Foundation
import ImageIO
import simd

/// Minimal glTF 2.0 binary (.glb) reader for the tooth models produced by tools/asset-pipeline:
/// one triangle primitive with POSITION/NORMAL/TEXCOORD_0, an index buffer and one PNG base-color texture.
nonisolated struct GLBMesh: Sendable {
    var positions: [SIMD3<Float>]
    var normals: [SIMD3<Float>]
    var uvs: [SIMD2<Float>]
    var indices: [UInt32]
    var baseColor: CGImage?
    var roughness: Float
    var boundsMin: SIMD3<Float>
    var boundsMax: SIMD3<Float>
}

enum GLBError: Error {
    case invalidHeader, missingChunk, unsupported(String)
}

nonisolated enum GLBLoader {
    static func read(url: URL) throws -> GLBMesh {
        try parse(Data(contentsOf: url, options: .mappedIfSafe))
    }

    static func parse(_ data: Data) throws -> GLBMesh {
        guard data.count >= 20, data.u32(0) == 0x4654_6C67 /* "glTF" */, data.u32(4) == 2 else { throw GLBError.invalidHeader }

        var json: [String: Any]?
        var bin: Data?
        var offset = 12
        while offset + 8 <= data.count {
            let length = Int(data.u32(offset))
            let type = data.u32(offset + 4)
            let chunk = data.subdata(in: (offset + 8)..<(offset + 8 + length))
            if type == 0x4E4F_534A { json = try JSONSerialization.jsonObject(with: chunk) as? [String: Any] }
            if type == 0x004E_4942 { bin = chunk }
            offset += 8 + length
        }
        guard let json, let bin else { throw GLBError.missingChunk }

        let accessors = json["accessors"] as? [[String: Any]] ?? []
        let views = json["bufferViews"] as? [[String: Any]] ?? []
        guard let primitive = ((json["meshes"] as? [[String: Any]])?.first?["primitives"] as? [[String: Any]])?.first,
              let attributes = primitive["attributes"] as? [String: Int],
              let positionIndex = attributes["POSITION"], let indexIndex = primitive["indices"] as? Int
        else { throw GLBError.unsupported("mesh layout") }

        func floats(_ accessorIndex: Int, components: Int) throws -> [Float] {
            let accessor = accessors[accessorIndex]
            guard accessor["componentType"] as? Int == 5126 else { throw GLBError.unsupported("non-float attribute") }
            let view = views[accessor["bufferView"] as? Int ?? 0]
            let count = accessor["count"] as? Int ?? 0
            let base = (view["byteOffset"] as? Int ?? 0) + (accessor["byteOffset"] as? Int ?? 0)
            let stride = view["byteStride"] as? Int ?? components * 4
            var out = [Float](repeating: 0, count: count * components)
            bin.withUnsafeBytes { raw in
                for i in 0..<count {
                    for c in 0..<components {
                        out[i * components + c] = raw.loadUnaligned(fromByteOffset: base + i * stride + c * 4, as: Float.self)
                    }
                }
            }
            return out
        }

        func vec3(_ flat: [Float]) -> [SIMD3<Float>] {
            stride(from: 0, to: flat.count, by: 3).map { SIMD3(flat[$0], flat[$0 + 1], flat[$0 + 2]) }
        }

        let positions = vec3(try floats(positionIndex, components: 3))
        let normals = try attributes["NORMAL"].map { vec3(try floats($0, components: 3)) } ?? []
        // glTF puts the UV origin at the top-left; RealityKit expects bottom-left.
        let uvs: [SIMD2<Float>] = try attributes["TEXCOORD_0"].map { index in
            let flat = try floats(index, components: 2)
            return stride(from: 0, to: flat.count, by: 2).map { SIMD2(flat[$0], 1 - flat[$0 + 1]) }
        } ?? []

        let indexAccessor = accessors[indexIndex]
        let indexView = views[indexAccessor["bufferView"] as? Int ?? 0]
        let indexCount = indexAccessor["count"] as? Int ?? 0
        let indexBase = (indexView["byteOffset"] as? Int ?? 0) + (indexAccessor["byteOffset"] as? Int ?? 0)
        let indices: [UInt32] = try bin.withUnsafeBytes { raw in
            switch indexAccessor["componentType"] as? Int {
            case 5123: (0..<indexCount).map { UInt32(raw.loadUnaligned(fromByteOffset: indexBase + $0 * 2, as: UInt16.self)) }
            case 5125: (0..<indexCount).map { raw.loadUnaligned(fromByteOffset: indexBase + $0 * 4, as: UInt32.self) }
            default: throw GLBError.unsupported("index type")
            }
        }

        let material = (json["materials"] as? [[String: Any]])?.first
        let pbr = material?["pbrMetallicRoughness"] as? [String: Any]
        var baseColor: CGImage?
        if let textureIndex = (pbr?["baseColorTexture"] as? [String: Any])?["index"] as? Int,
           let source = (json["textures"] as? [[String: Any]])?[textureIndex]["source"] as? Int,
           let viewIndex = (json["images"] as? [[String: Any]])?[source]["bufferView"] as? Int {
            let view = views[viewIndex]
            let start = view["byteOffset"] as? Int ?? 0
            let imageData = bin.subdata(in: start..<(start + (view["byteLength"] as? Int ?? 0)))
            if let imageSource = CGImageSourceCreateWithData(imageData as CFData, nil) {
                baseColor = CGImageSourceCreateImageAtIndex(imageSource, 0, nil)
            }
        }

        let boundsMin = positions.reduce(SIMD3(repeating: .infinity)) { simd_min($0, $1) }
        let boundsMax = positions.reduce(SIMD3(repeating: -.infinity)) { simd_max($0, $1) }
        return GLBMesh(
            positions: positions, normals: normals, uvs: uvs, indices: indices, baseColor: baseColor,
            roughness: (pbr?["roughnessFactor"] as? NSNumber)?.floatValue ?? 0.6,
            boundsMin: boundsMin, boundsMax: boundsMax
        )
    }
}

nonisolated extension GLBMesh {
    /// Mirror image across the sagittal plane (x → -x). Normals follow and the triangle winding is
    /// reversed, so the mirrored copy is lit exactly like the original instead of inside out.
    func mirroredX() -> GLBMesh {
        var m = self
        m.positions = positions.map { SIMD3(-$0.x, $0.y, $0.z) }
        m.normals = normals.map { SIMD3(-$0.x, $0.y, $0.z) }
        m.indices = stride(from: 0, to: indices.count - indices.count % 3, by: 3).flatMap { [indices[$0], indices[$0 + 2], indices[$0 + 1]] }
        m.boundsMin = SIMD3(-boundsMax.x, boundsMin.y, boundsMin.z)
        m.boundsMax = SIMD3(-boundsMin.x, boundsMax.y, boundsMax.z)
        return m
    }
}

private extension Data {
    nonisolated func u32(_ offset: Int) -> UInt32 {
        withUnsafeBytes { $0.loadUnaligned(fromByteOffset: offset, as: UInt32.self) }.littleEndian
    }
}
