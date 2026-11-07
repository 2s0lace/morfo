import SwiftUI
import UIKit

/// Callbacks for the 3D viewport. All positions and deltas are in points.
struct ViewportGestureHandler {
    var onTap: (CGPoint) -> Void = { _ in }
    var onDoubleTap: (CGPoint) -> Void = { _ in }
    /// One finger drag: rotate.
    var onRotateDrag: (CGSize) -> Void = { _ in }
    /// Long press, then drag: move the tooth under the finger.
    var onMoveStart: (CGPoint) -> Void = { _ in }
    var onMoveDrag: (CGSize) -> Void = { _ in }
    var onMoveEnd: () -> Void = {}
    /// Two fingers: pan + pinch zoom the camera, twist rotates around the viewing axis.
    var onTransform: (_ pan: CGSize, _ zoom: CGFloat, _ twist: CGFloat, _ centroid: CGPoint) -> Void = { _, _, _, _ in }
}

/// Transparent layer over the RealityView that turns touches into viewport gestures.
/// UIKit recognisers are used because they compose one-finger, long-press and two-finger gestures reliably.
struct ViewportGestureLayer: UIViewRepresentable {
    var handler: ViewportGestureHandler

    func makeCoordinator() -> Coordinator { Coordinator() }

    func makeUIView(context: Context) -> UIView {
        let view = UIView()
        view.backgroundColor = .clear
        context.coordinator.install(on: view)
        context.coordinator.handler = handler
        return view
    }

    func updateUIView(_ view: UIView, context: Context) {
        context.coordinator.handler = handler
    }

    final class Coordinator: NSObject, UIGestureRecognizerDelegate {
        var handler = ViewportGestureHandler()
        private var lastPan: CGPoint = .zero
        private var lastMove: CGPoint = .zero
        private var lastTwoFingerPan: CGPoint = .zero
        private var twistTotal: CGFloat = 0
        private var twisting = false
        private var pinch: UIPinchGestureRecognizer!
        private var twist: UIRotationGestureRecognizer!
        private var twoFingerPan: UIPanGestureRecognizer!

        /// Twist only after a deliberate rotation, so pinching does not turn teeth by accident.
        private static let twistThreshold: CGFloat = 0.2

        func install(on view: UIView) {
            let doubleTap = UITapGestureRecognizer(target: self, action: #selector(onDoubleTap))
            doubleTap.numberOfTapsRequired = 2
            let tap = UITapGestureRecognizer(target: self, action: #selector(onTap))
            tap.require(toFail: doubleTap)

            let longPress = UILongPressGestureRecognizer(target: self, action: #selector(onLongPress))
            longPress.minimumPressDuration = 0.4
            longPress.allowableMovement = 8

            let pan = UIPanGestureRecognizer(target: self, action: #selector(onPan))
            pan.maximumNumberOfTouches = 1
            pan.require(toFail: longPress)

            twoFingerPan = UIPanGestureRecognizer(target: self, action: #selector(onTwoFingerPan))
            twoFingerPan.minimumNumberOfTouches = 2
            pinch = UIPinchGestureRecognizer(target: self, action: #selector(onPinch))
            twist = UIRotationGestureRecognizer(target: self, action: #selector(onTwist))
            for r in [twoFingerPan!, pinch!, twist!] as [UIGestureRecognizer] { r.delegate = self }

            for r in [doubleTap, tap, longPress, pan, twoFingerPan!, pinch!, twist!] as [UIGestureRecognizer] {
                view.addGestureRecognizer(r)
            }
        }

        func gestureRecognizer(_ a: UIGestureRecognizer, shouldRecognizeSimultaneouslyWith b: UIGestureRecognizer) -> Bool {
            let twoFinger: [UIGestureRecognizer] = [twoFingerPan, pinch, twist]
            return twoFinger.contains(a) && twoFinger.contains(b)
        }

        @objc private func onTap(_ r: UITapGestureRecognizer) { handler.onTap(r.location(in: r.view)) }
        @objc private func onDoubleTap(_ r: UITapGestureRecognizer) { handler.onDoubleTap(r.location(in: r.view)) }

        @objc private func onPan(_ r: UIPanGestureRecognizer) {
            let t = r.translation(in: r.view)
            if r.state == .began { lastPan = .zero }
            handler.onRotateDrag(CGSize(width: t.x - lastPan.x, height: t.y - lastPan.y))
            lastPan = t
        }

        @objc private func onLongPress(_ r: UILongPressGestureRecognizer) {
            let p = r.location(in: r.view)
            switch r.state {
            case .began:
                lastMove = p
                handler.onMoveStart(p)
            case .changed:
                handler.onMoveDrag(CGSize(width: p.x - lastMove.x, height: p.y - lastMove.y))
                lastMove = p
            default:
                handler.onMoveEnd()
            }
        }

        @objc private func onTwoFingerPan(_ r: UIPanGestureRecognizer) {
            let t = r.translation(in: r.view)
            if r.state == .began { lastTwoFingerPan = .zero }
            handler.onTransform(CGSize(width: t.x - lastTwoFingerPan.x, height: t.y - lastTwoFingerPan.y), 1, 0, r.location(in: r.view))
            lastTwoFingerPan = t
        }

        @objc private func onPinch(_ r: UIPinchGestureRecognizer) {
            guard r.state == .changed else { return }
            handler.onTransform(.zero, r.scale, 0, r.location(in: r.view))
            r.scale = 1
        }

        @objc private func onTwist(_ r: UIRotationGestureRecognizer) {
            if r.state == .began { twistTotal = 0; twisting = false }
            guard r.state == .changed else { return }
            twistTotal += r.rotation
            if !twisting && abs(twistTotal) > Self.twistThreshold { twisting = true }
            if twisting { handler.onTransform(.zero, 1, r.rotation, r.location(in: r.view)) }
            r.rotation = 0
        }
    }
}
