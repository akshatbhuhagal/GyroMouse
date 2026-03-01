package com.axat.gyromouse.di

import android.content.Context
import com.axat.gyromouse.data.datastore.AppPreferences
import com.axat.gyromouse.data.repository.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing app-level dependencies:
 * - AppPreferences (DataStore wrapper)
 * - SettingsRepository
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppPreferences(
        @ApplicationContext context: Context
    ): AppPreferences {
        return AppPreferences(context)
    }

    @Provides
    @Singleton
    fun provideSettingsRepository(
        appPreferences: AppPreferences
    ): SettingsRepository {
        return SettingsRepository(appPreferences)
    }
}
