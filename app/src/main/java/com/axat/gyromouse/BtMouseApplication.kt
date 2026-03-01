package com.axat.gyromouse

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class annotated with @HiltAndroidApp to trigger
 * Hilt code generation and serve as the dependency container root.
 */
@HiltAndroidApp
class BtMouseApplication : Application()
