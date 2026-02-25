# Packet SDK Integration Blueprint (v1.0)

## Overview
This document outlines the integration of the **Packet SDK** into the Etherion Network application. The Packet SDK allows users to monetize their unused internet bandwidth in exchange for an enhanced mining hashrate within the Etherion ecosystem.

## Current Status: Placeholder
The current implementation uses a `PacketManager` wrapper that simulates the SDK's behavior. This is designed to allow the Etherion team to present the app for approval while having the "pipes" ready for immediate activation once the SDK is provided.

## Features
- **Mandatory Consent:** Users must read and accept a specialized Privacy Policy before the SDK can be activated.
- **Dynamic Hashrate Boost:** When active, the user receives a base **+25% H/s** bonus.
- **Revenue-Linked Scaling:** The logic is prepared to re-evaluate the hashrate boost based on real-time earnings from the Packet Network.

## Implementation Details

### 1. The Manager (`PacketManager.kt`)
- **State Management:** Uses Jetpack DataStore to persist `isEnabled` and `isPolicyAccepted` states.
- **Lifecycle Control:** Contains `setEnabled(Boolean)` which acts as the hook for starting/stopping the SDK background service.

### 2. Economic Integration
The mining engine calculates the final hashrate using the following formula:
`H_final = (Base * Hardware_Mult * Team_Mult * Streak_Mult) * Packet_Mult`

### 3. Play Store Compliance
To meet Google Play's "Prominent Disclosure" requirements:
- The terminal command `packet info` displays the full policy.
- The user must type `packet accept` to log consent.
- A toggle switch or terminal command `packet enable` is required for final activation.

## Plug-and-Play Instructions
Once the real `packet-sdk.aar` or dependency is available:
1. Update `libs.versions.toml` with the real library coordinates.
2. In `PacketManager.kt`, replace the `// TODO` comments in `setEnabled` with the actual SDK initialization calls:
   ```kotlin
   // Example
   if (enabled) PacketSDK.start(context, apiKey) else PacketSDK.stop()
   ```
3. Update `getBonusMultiplier()` to query the SDK's live earning rate if available.
