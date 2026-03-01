package com.axat.gyromouse.data.bluetooth

/**
 * HID Descriptors for a standard USB HID mouse.
 *
 * This descriptor defines a mouse with:
 * - 3 buttons (left, right, middle)
 * - X axis (signed 8-bit, relative)
 * - Y axis (signed 8-bit, relative)
 * - Scroll wheel (signed 8-bit, relative)
 *
 * Report format: [buttons (1 byte), X (1 byte), Y (1 byte), wheel (1 byte)]
 * Total report size: 4 bytes
 */
object HidDescriptors {

    /**
     * Standard HID mouse descriptor following the USB HID specification.
     *
     * Structure breakdown:
     * - Usage Page: Generic Desktop (0x01)
     * - Usage: Mouse (0x02)
     * - Collection: Application
     *   - Usage: Pointer (0x01)
     *   - Collection: Physical
     *     - Usage Page: Button (0x09)
     *       - 3 buttons (1 bit each)
     *       - 5 bits padding
     *     - Usage Page: Generic Desktop (0x01)
     *       - X axis: 8-bit signed relative (-127 to 127)
     *       - Y axis: 8-bit signed relative (-127 to 127)
     *       - Wheel: 8-bit signed relative (-127 to 127)
     *   - End Collection (Physical)
     * - End Collection (Application)
     */
    val MOUSE_DESCRIPTOR: ByteArray = byteArrayOf(
        // Usage Page (Generic Desktop)
        0x05.toByte(), 0x01.toByte(),
        // Usage (Mouse)
        0x09.toByte(), 0x02.toByte(),
        // Collection (Application)
        0xA1.toByte(), 0x01.toByte(),

        // Usage (Pointer)
        0x09.toByte(), 0x01.toByte(),
        // Collection (Physical)
        0xA1.toByte(), 0x00.toByte(),

        // ---- Buttons (3 buttons) ----
        // Usage Page (Button)
        0x05.toByte(), 0x09.toByte(),
        // Usage Minimum (Button 1 - Left)
        0x19.toByte(), 0x01.toByte(),
        // Usage Maximum (Button 3 - Middle)
        0x29.toByte(), 0x03.toByte(),
        // Logical Minimum (0)
        0x15.toByte(), 0x00.toByte(),
        // Logical Maximum (1)
        0x25.toByte(), 0x01.toByte(),
        // Report Count (3 buttons)
        0x95.toByte(), 0x03.toByte(),
        // Report Size (1 bit per button)
        0x75.toByte(), 0x01.toByte(),
        // Input (Data, Variable, Absolute)
        0x81.toByte(), 0x02.toByte(),

        // ---- Padding (5 bits) ----
        // Report Count (1)
        0x95.toByte(), 0x01.toByte(),
        // Report Size (5 bits padding)
        0x75.toByte(), 0x05.toByte(),
        // Input (Constant) - padding bits
        0x81.toByte(), 0x03.toByte(),

        // ---- X, Y, Wheel axes ----
        // Usage Page (Generic Desktop)
        0x05.toByte(), 0x01.toByte(),
        // Usage (X)
        0x09.toByte(), 0x30.toByte(),
        // Usage (Y)
        0x09.toByte(), 0x31.toByte(),
        // Usage (Wheel)
        0x09.toByte(), 0x38.toByte(),
        // Logical Minimum (-127)
        0x15.toByte(), 0x81.toByte(),
        // Logical Maximum (127)
        0x25.toByte(), 0x7F.toByte(),
        // Report Size (8 bits per axis)
        0x75.toByte(), 0x08.toByte(),
        // Report Count (3 axes: X, Y, Wheel)
        0x95.toByte(), 0x03.toByte(),
        // Input (Data, Variable, Relative)
        0x81.toByte(), 0x06.toByte(),

        // End Collection (Physical)
        0xC0.toByte(),
        // End Collection (Application)
        0xC0.toByte()
    )

    /** Report size in bytes: [buttons, X, Y, wheel] */
    const val REPORT_SIZE = 4
}
