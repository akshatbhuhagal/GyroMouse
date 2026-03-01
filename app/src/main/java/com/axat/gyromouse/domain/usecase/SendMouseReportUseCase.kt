package com.axat.gyromouse.domain.usecase

import com.axat.gyromouse.data.repository.BluetoothRepository
import com.axat.gyromouse.domain.model.MouseAction
import javax.inject.Inject

/**
 * Use case for sending mouse HID reports.
 * Translates high-level MouseAction into HID reports via the repository.
 */
class SendMouseReportUseCase @Inject constructor(
    private val bluetoothRepository: BluetoothRepository
) {
    /**
     * Send a mouse action as a HID report.
     * @param action The MouseAction to send
     * @return true if the report was sent successfully
     */
    suspend operator fun invoke(action: MouseAction): Boolean {
        return bluetoothRepository.sendMouseAction(action)
    }
}
