# Diversify for Solana Seeker

A gamified transaction orchestration application for Solana Seeker devices.

## Features

- **Seed Vault Integration (via Solana Mobile Wallet Adapter)**: Hardware-level security with biometric authentication and seamless wallet collaboration
- **Transaction Orchestration**: 50+ daily transactions for Season 2 level management
- **Math Puzzles**: Human verification between transactions
- **Cycler Wallets**: 50 return wallets for fund recycling
- **Neutral Zone**: Post-session games and partner content
- **Matrix UI**: Cyberpunk-inspired design

## Architecture

- **MVVM + Clean Architecture**
- **Jetpack Compose** for UI
- **Dagger Hilt** for dependency injection
- **Room Database** for local storage
- **EncryptedSharedPreferences** for secure key storage

## Building

1. Clone the repository
2. Open in Android Studio
3. Build and run on Solana Seeker device

## Requirements

- Android SDK 34
- Solana Seeker device with Seed Vault
- Internet connection for Solana RPC calls (used by wallet adapter and optional RPC client)

## License

Proprietary - All rights reserved.
