package com.axat.gyromouse.di

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import com.axat.gyromouse.data.bluetooth.BleHidManager
import com.axat.gyromouse.data.repository.BluetoothRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing Bluetooth-related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object BluetoothModule {

    @Provides
    @Singleton
    fun provideBluetoothAdapter(@ApplicationContext context: Context): BluetoothAdapter? {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        return manager?.adapter
    }

    @Provides
    @Singleton
    fun provideBleHidManager(
        @ApplicationContext context: Context,
        adapter: BluetoothAdapter?
    ): BleHidManager {
        return BleHidManager(context, adapter)
    }

    @Provides
    @Singleton
    fun provideBluetoothRepository(
        @ApplicationContext context: Context,
        adapter: BluetoothAdapter?,
        bleHidManager: BleHidManager
    ): BluetoothRepository {
        return BluetoothRepository(context, adapter, bleHidManager)
    }
}
