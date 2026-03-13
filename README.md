# Diversify for Solana Seeker

Cycler alpha for Solana Seeker: wallet-connect, randomized buy/sell swap orchestration on mainnet.

## Features

- Wallet connect required (Seed Vault)
- Mainnet Solana RPC + Jupiter swap payloads
- No app payment requirement
- Cycler-only flow with randomized inventory-aware BUY/SELL ordering
- Built-in high-quality token allowlist (`USDC`, `JUP`, `BONK`)
- Puzzle gate between transaction approvals
- 0.02 SOL fee reserve validation before session start

## Cycler Flow

1. Connect wallet.
2. Select an even total transaction count.
3. Set session amount and funding asset (`SOL` or `USDC`).
4. Approve each puzzle-gated swap as the app randomizes BUY/SELL while keeping counts balanced.
5. Complete the run with flattened positions.

## Notes

- Bundler/send/receive mode is removed.
- Room database persistence is removed.
- Session state is in-memory for alpha.
