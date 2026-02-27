# Monetization SDK Integration & Review Guide

## Information for App Reviewers
The Etherion Network application is built with a modular architecture to support future monetization through specialized SDKs (such as proxy sharing, bandwidth monetization, or similar technologies).

### Architecture Readiness
The app is currently "Injection Ready" for third-party SDKs:
- **Custom Application Class**: Found in `com.etherion.network.EtherionApplication`. This class serves as the central initialization hook for any chosen monetization provider.
- **Background Service**: Found in `com.etherion.network.miner.MiningService`. This foreground service is designed to manage long-running tasks, ensuring consistent monetization performance even in the background.
- **Modular Managers**: We use specialized managers (like `BrightManager`) to handle user consent, privacy policies, and state management independently of the core mining logic.

### Compliance & Transparency
Any future SDK integration will strictly adhere to the following principles:
1. **User Consent**: No background monetization activity will occur without explicit, prominent disclosure and user acceptance of the provider's terms.
2. **Lifecycle Control**: Users will always have the ability to enable or disable these features through the "Command Console" or app settings.
3. **Performance Monitoring**: The app is designed to monitor resource usage to ensure that monetization SDKs do not negatively impact the user's primary device experience.

### Next Steps for Implementation
Once a monetization partner is finalized:
1. The dependency will be added to `build.gradle.kts`.
2. The `initializeSDK()` method will be invoked within `EtherionApplication.onCreate()`.
3. The `MiningViewModel` will be updated to reflect real-time bonuses provided by the SDK.
