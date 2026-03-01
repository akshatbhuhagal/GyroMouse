package com.axat.gyromouse.domain.model

/**
 * Represents the mouse buttons supported by the HID mouse.
 */
enum class MouseButton {
    LEFT,
    RIGHT,
    MIDDLE
}

/**
 * Sealed class representing all possible mouse actions.
 * These are the high-level gestures detected by the Trackpad UI
 * that get translated into HID reports.
 */
sealed class MouseAction {
    /** Move the cursor by (dx, dy) pixels */
    data class Move(val dx: Int, val dy: Int) : MouseAction()

    /** Single click with the specified button */
    data class Click(val button: MouseButton) : MouseAction()

    /** Double-click with the left button */
    data object DoubleClick : MouseAction()

    /** Scroll wheel movement */
    data class Scroll(val amount: Int) : MouseAction()

    /** Begin a click-drag (press and hold left button) */
    data object DragStart : MouseAction()

    /** Move while dragging (left button held) */
    data class DragMove(val dx: Int, val dy: Int) : MouseAction()

    /** Release the drag (release left button) */
    data object DragEnd : MouseAction()
}
