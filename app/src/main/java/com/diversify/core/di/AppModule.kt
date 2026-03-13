package com.diversify.core.di

import android.content.Context
import com.diversify.solana.seedvault.SeedVaultManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSeedVaultManager(@ApplicationContext context: Context): SeedVaultManager {
        return SeedVaultManager(context)
    }
}
