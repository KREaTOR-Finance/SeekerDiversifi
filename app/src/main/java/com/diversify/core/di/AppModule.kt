package com.diversify.core.di

import android.content.Context
import com.diversify.core.util.DebugHelper
import com.diversify.data.local.database.AppDatabase
import com.diversify.data.repository.SessionRepository
import com.diversify.domain.usecase.GenerateTransactionBatchUseCase
import com.diversify.gamification.doors.ThreeDoorsGame
import com.diversify.gamification.neutralzone.NeutralZoneManager
import com.diversify.gamification.puzzle.MathPuzzleEngine
import com.diversify.solana.rpc.SolanaRpcClient
import com.diversify.solana.seedvault.SeedVaultManager
import com.diversify.solana.wallet.CyclerWalletManager
import com.diversify.solana.wallet.ReturnScheduler
import android.content.SharedPreferences
import com.diversify.data.repository.SettingsRepository
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
    
    @Provides
    @Singleton
    fun provideCyclerWalletManager(@ApplicationContext context: Context, rpcClient: SolanaRpcClient): CyclerWalletManager {
        return CyclerWalletManager(context, rpcClient)
    }
    
    @Provides
    @Singleton
    fun provideMathPuzzleEngine(): MathPuzzleEngine {
        return MathPuzzleEngine()
    }
    
    @Provides
    @Singleton
    fun provideThreeDoorsGame(): ThreeDoorsGame {
        return ThreeDoorsGame()
    }
    
    @Provides
    @Singleton
    fun provideNeutralZoneManager(threeDoorsGame: ThreeDoorsGame): NeutralZoneManager {
        return NeutralZoneManager(threeDoorsGame)
    }
    
    @Provides
    @Singleton
    fun provideSolanaRpcClient(): SolanaRpcClient {
        return SolanaRpcClient()
    }
    
    @Provides
    @Singleton
    fun provideDebugHelper(): DebugHelper {
        return DebugHelper()
    }
    
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }
    
    @Provides
    @Singleton
    fun provideSessionRepository(database: AppDatabase): SessionRepository {
        return SessionRepository(database.sessionDao(), database.transactionDao())
    }
    
    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("diversify_prefs", Context.MODE_PRIVATE)
    }
    
    @Provides
    @Singleton
    fun provideSettingsRepository(sharedPreferences: SharedPreferences): SettingsRepository {
        return SettingsRepository(sharedPreferences)
    }
    
    @Provides
    @Singleton
    fun provideGenerateTransactionBatchUseCase(
        walletManager: CyclerWalletManager
    ): GenerateTransactionBatchUseCase {
        return GenerateTransactionBatchUseCase(walletManager)
    }
    
    @Provides
    @Singleton
    fun provideReturnScheduler(
        walletManager: CyclerWalletManager
    ): ReturnScheduler {
        return ReturnScheduler(walletManager)
    }
}
