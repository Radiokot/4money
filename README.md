# 4Money

I'm building a budget tracking app inspired by [1Money](https://play.google.com/store/apps/details?id=org.pixelrush.moneyiq), which was very nice but got screwed up. 

The bar for such applications is quite high. My current goal is not to build a general purpose budget tracker for wide audience, 
but only to cover the needs of my family and have some fun meanwhile. It's also my first Compose app, so I am actively learning.

Key aspects of the app:
- Local-first data storage
- Fast and reliable sync between devices
- Ability to self-host the backend
- Convenient handling of currency exchange
- Convenient handling of currencies with high precision

If you're looking for 1Money migration, there's `OneMoneyConvert` script in this repo which reads 1Money CSV exports.
You can modify it to get desired outputs.

If you're exploring PowerSync use case examples, take a look at:
- `AtomicCrudSupabaseConnector`
- `PowerSyncTransferHistoryRepository`
- `PowerSyncTransferFundsUseCase`
- `BackgroundPowerSyncWorker`

## Tech stack
- Kotlin
- Compost UI & Navigation
- Flow & coroutines for concurrency
- Koin for dependency injection
- Supabase as a backend
- PowerSync for local-first data storage and synchronization
- kotlin-logging & slf4j-handroid for logging
