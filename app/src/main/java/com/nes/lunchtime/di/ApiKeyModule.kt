package com.nes.lunchtime.di

import com.nes.lunchtime.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named

@Module
@InstallIn(SingletonComponent::class)
object ApiKeyModule {
    @Provides
    @Named("google_places_api_key")
    fun provideApiKey(): String {
        return BuildConfig.GOOGLE_PLACES_API_KEY
    }
}
