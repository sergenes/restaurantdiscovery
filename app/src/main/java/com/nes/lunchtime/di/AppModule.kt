package com.nes.lunchtime.di

import com.nes.lunchtime.repo.RestaurantsRepository
import com.nes.lunchtime.repo.RestaurantsRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface AppModule {
    @Binds
    abstract fun bindRestaurantsRepository(
        restaurantsRepositoryImpl: RestaurantsRepositoryImpl
    ): RestaurantsRepository
}