package com.axat.gyromouse.data.bluetooth

import com.axat.gyromouse.domain.model.MouseButton

/**
 * Builds HID mouse reports to be sent over the Bluetooth HID profile.
 *
 * Report format (4 bytes):
 * - Byte 0: Button state (bit0 = left, bit1 = right, bit2 = middle)
 * - Byte 1: X movement (signed, -127 to 127)
 * - Byte 2: Y movement (signed, -127 to 127)
 * - Byte 3: Scroll wheel (signed, -127 to 127)
 *
 * All movement values are clamped to the range [-127, 127].
 */
object HidReportBuilder {

    /** Clamp a value to the HID signed byte range [-127, 127] */
    private fun clamp(value: Int): Byte {
        return value.coerceIn(-127, 127).toByte()
    }

    /** Get the button bitmask for a given MouseButton */
    private fun buttonBit(button: MouseButton): Byte {
        return when (button) {
            MouseButton.LEFT -> 0x01
            MouseButton.RIGHT -> 0x02
            MouseButton.MIDDLE -> 0x04
        }.toByte()
    }

    /**
     * Build a mouse movement report.
     *
     * @param dx Horizontal movement delta (positive = right)
     * @param dy Vertical movement delta (positive = down)
     * @param buttonsPressed Set of currently pressed buttons (for drag support)
     * @return 4-byte HID report [buttons, dx, dy, 0]
     */
    fun buildMoveReport(
        dx: Int,
        dy: Int,
        buttonsPressed: Set<MouseButton> = emptySet()
    ): ByteArray {
        var buttonByte: Byte = 0
        buttonsPressed.forEach { button ->
            buttonByte = (buttonByte.toInt() or buttonBit(button).toInt()).toByte()
        }
        return byteArrayOf(
            buttonByte,
            clamp(dx),
            clamp(dy),
            0 // No scroll
        )
    }

    /**
     * Build a button click (press) report.
     * Must be followed by a release report after a short delay to register as a click.
     *
     * @param button The mouse button to press
     * @return 4-byte HID report with the button pressed
     */
    fun buildClickReport(button: MouseButton): ByteArray {
        return byteArrayOf(
            buttonBit(button),
            0, // No X movement
            0, // No Y movement
            0  // No scroll
        )
    }

    /**
     * Build a scroll wheel report.
     *
     * @param scroll Scroll amount (positive = scroll up/away, negative = scroll down/toward)
     * @return 4-byte HID report with scroll data
     */
    fun buildScrollReport(scroll: Int): ByteArray {
        return byteArrayOf(
            0, // No buttons
            0, // No X movement
            0, // No Y movement
            clamp(scroll)
        )
    }

    /**
     * Build a release report — all buttons released, no movement.
     * Used to complete a click action or end a drag operation.
     *
     * @return 4-byte HID report with all zeros
     */
    fun buildReleaseReport(): ByteArray {
        return byteArrayOf(0, 0, 0, 0)
    }
}
