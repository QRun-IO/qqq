# PLAN: QSessionStoreQBit - Pluggable Session Storage

## Goal

Create a QBit that provides pluggable session storage/caching to avoid re-deriving expensive session data (security keys, permissions) on every request.

## Decisions

- **Location:** Separate Maven module `qqq-qbit-session-store` (like qqq-qbit-workflows)
- **Integration:** Explicit config via `QAuthenticationMetaData.withSessionStoreEnabled(true)`
- **Providers:** In-Memory, Table-based, and Redis (all three in initial implementation)

## Approach

Implement as a QBit in its own module with a pluggable provider interface, shipping three backends: in-memory (dev), table-based (persistent), and Redis (distributed).

## Design Pattern: Strategy

The provider system uses the **Strategy Pattern** for easy extensibility:

```
┌─────────────────────────────────────────┐
│     QSessionStoreProviderInterface      │  ← Strategy Interface
│  (store, load, remove, touch, etc.)     │
└─────────────────────────────────────────┘
          ▲           ▲           ▲
          │           │           │
┌─────────┴──┐ ┌──────┴─────┐ ┌───┴────────┐
│ InMemory   │ │ TableBased │ │   Redis    │  ← Concrete Strategies
│ Provider   │ │  Provider  │ │  Provider  │
└────────────┘ └────────────┘ └────────────┘
                      ▲
                      │
              ┌───────┴───────┐
              │ Custom impl   │  ← User-provided via QCodeReference
              │ (SPI pattern) │
              └───────────────┘
```

**Strategy Interface:**
```java
public interface QSessionStoreProviderInterface
{
   void store(String sessionUuid, QSession session, Duration ttl);
   Optional<QSession> load(String sessionUuid);
   void remove(String sessionUuid);
   void touch(String sessionUuid);  // sliding expiration
   void cleanExpired();
   String status();

   // Factory method for self-configuration
   default void configure(QSessionStoreQBitConfig config) {}
}
```

**Factory for Strategy Selection:**
```java
public class QSessionStoreProviderFactory
{
   public static QSessionStoreProviderInterface createProvider(QSessionStoreQBitConfig config)
   {
      QSessionStoreProviderInterface provider = switch(config.getProviderType())
      {
         case IN_MEMORY -> new InMemorySessionStoreProvider();
         case TABLE_BASED -> new TableBasedSessionStoreProvider();
         case REDIS -> new RedisSessionStoreProvider();
         case CUSTOM -> QCodeLoader.getAdHoc(QSessionStoreProviderInterface.class,
                                              config.getCustomProviderCodeReference());
      };
      provider.configure(config);
      return provider;
   }
}
```

This makes adding new providers trivial - just implement the interface and add an enum value (or use CUSTOM for external implementations).

## QBit Structure

**QSessionStoreQBitConfig:**
- `providerType` - IN_MEMORY, TABLE_BASED, REDIS, or CUSTOM
- `defaultTtl` - Session expiration (default: 1 hour)
- `enableSlidingExpiration` - Reset TTL on access (default: true)
- `backendName` - Required for TABLE_BASED
- `tableName` - Default: "storedSession"
- `maxCacheSize` - For IN_MEMORY LRU (default: 10000)
- `redisHost`, `redisPort`, `redisPassword` - For REDIS provider
- `redisKeyPrefix` - Namespace for Redis keys (default: "qqq:session:")
- `customProviderCodeReference` - For CUSTOM implementations

**What the QBit produces:**
- `StoredSession` table (for TABLE_BASED): sessionUuid, userId, sessionData (JSON), expiresAt
- `CleanExpiredSessionsProcess` - Scheduled cleanup process
- Registers provider in QInstance supplemental metadata

## Provider Implementations

1. **InMemorySessionStoreProvider**
   - ConcurrentHashMap with CachedSession(session, expiresAt)
   - Optional LRU eviction
   - Scheduled cleanup thread
   - Best for: dev, testing, single-instance

2. **TableBasedSessionStoreProvider**
   - Uses QQQ table actions (Insert/Get/Update/Delete)
   - Serializes QSession to JSON
   - Uses QSystemUserSession for DB operations
   - Best for: multi-instance, persistence across restarts

3. **RedisSessionStoreProvider**
   - Uses Jedis or Lettuce client
   - Native TTL support via Redis SETEX/EXPIRE
   - JSON serialization with Jackson
   - Best for: distributed HA deployments, horizontal scaling

## Adding New Providers

To add a new provider (e.g., Memcached):

1. **Built-in:** Add enum value to `QSessionStoreProviderType`, implement `QSessionStoreProviderInterface`, add case to factory
2. **External/Custom:** Implement interface in your app, use `CUSTOM` type with `QCodeReference` - no changes to QBit needed

```java
// External custom provider example
new QSessionStoreQBitConfig()
   .withProviderType(QSessionStoreProviderType.CUSTOM)
   .withCustomProviderCodeReference(new QCodeReference(MyMemcachedProvider.class))
```

## Integration with Auth Modules

**Explicit enablement in QAuthenticationMetaData:**
```java
qInstance.setAuthentication(new OAuth2AuthenticationMetaData()
   .withSessionStoreEnabled(true)  // NEW - opt-in
   // ... other config
);
```

**On session creation** (after finalCustomizeSession):
```java
if(authMetaData.getSessionStoreEnabled()) {
   QSessionStoreProviderInterface store = getSessionStore(qInstance);
   if(store != null) {
      store.store(session.getUuid(), session, config.getDefaultTtl());
   }
}
```

**On session resume** (before re-deriving from token):
```java
if(authMetaData.getSessionStoreEnabled()) {
   QSessionStoreProviderInterface store = getSessionStore(qInstance);
   if(store != null) {
      Optional<QSession> cached = store.load(sessionUuid);
      if(cached.isPresent()) {
         store.touch(sessionUuid);  // sliding expiration
         return cached.get();
      }
   }
}
// Fall back to existing token-based derivation
```

## Module Structure

**New Maven module:** `qqq-qbit-session-store`

```
qqq-qbit-session-store/
├── pom.xml
└── src/main/java/com/kingsrook/qqq/qbit/sessionstore/
    ├── QSessionStoreProviderInterface.java   ← Strategy interface
    ├── QSessionStoreProviderFactory.java     ← Factory for strategy selection
    ├── QSessionStoreProviderType.java        ← Enum of built-in strategies
    ├── QSessionStoreMetaData.java
    ├── QSessionStoreQBitConfig.java
    ├── QSessionStoreQBitProducer.java
    ├── providers/                            ← Concrete strategies
    │   ├── InMemorySessionStoreProvider.java
    │   ├── TableBasedSessionStoreProvider.java
    │   └── RedisSessionStoreProvider.java
    ├── model/
    │   └── StoredSession.java
    └── metadata/
        ├── StoredSessionTableMetaDataProducer.java
        └── CleanExpiredSessionsProcessMetaDataProducer.java
```

**Dependencies:**
- `qqq-backend-core` (required)
- `redis.clients:jedis` (optional, for Redis provider)

## Usage Example

```java
new QSessionStoreQBitProducer()
   .withConfig(new QSessionStoreQBitConfig()
      .withProviderType(QSessionStoreProviderType.TABLE_BASED)
      .withBackendName("primaryBackend")
      .withDefaultTtl(Duration.ofHours(8))
      .withEnableSlidingExpiration(true))
   .produce(qInstance);
```

## Key Files to Modify

**In qqq-backend-core:**
- `QAuthenticationMetaData.java` - Add `sessionStoreEnabled` field
- `OAuth2AuthenticationModule.java` - Add session store integration hooks
- `Auth0AuthenticationModule.java` - Add session store integration hooks
- `QInstance.java` - Add supplemental metadata getter for session store

**New module:**
- Create `qqq-qbit-session-store/pom.xml`
- All files in module structure above

## Implementation TODO

### Phase 1: Module Setup
- [ ] Create `qqq-qbit-session-store` Maven module
- [ ] Add to parent pom.xml modules list
- [ ] Configure pom.xml with dependencies (qqq-backend-core, jedis optional)

### Phase 2: Core Interfaces
- [ ] Create `QSessionStoreProviderInterface` (Strategy interface)
- [ ] Create `QSessionStoreProviderType` enum (IN_MEMORY, TABLE_BASED, REDIS, CUSTOM)
- [ ] Create `QSessionStoreProviderFactory` (Factory for strategy selection)
- [ ] Create `QSessionStoreQBitConfig` with all config options
- [ ] Create `QSessionStoreMetaData` (supplemental metadata holder)

### Phase 3: Provider Implementations
- [ ] Implement `InMemorySessionStoreProvider`
  - [ ] ConcurrentHashMap storage
  - [ ] LRU eviction support
  - [ ] Scheduled cleanup thread
  - [ ] Unit tests
- [ ] Implement `TableBasedSessionStoreProvider`
  - [ ] Create `StoredSession` RecordEntity
  - [ ] Create `StoredSessionTableMetaDataProducer`
  - [ ] JSON serialization of QSession
  - [ ] QSystemUserSession for DB ops
  - [ ] Unit tests
- [ ] Implement `RedisSessionStoreProvider`
  - [ ] Jedis client integration
  - [ ] SETEX/EXPIRE for TTL
  - [ ] Connection pooling
  - [ ] Unit tests (with Testcontainers)

### Phase 4: QBit Producer
- [ ] Create `QSessionStoreQBitProducer`
- [ ] Create `CleanExpiredSessionsProcessMetaDataProducer`
- [ ] Register provider in QInstance supplemental metadata
- [ ] QBit validation logic

### Phase 5: Auth Module Integration (qqq-backend-core side - COMPLETE)
- [x] Add `sessionStoreEnabled` to `QAuthenticationMetaData`
- [x] Create `QSessionStoreHelper.java` reflection bridge for optional QBit
- [x] Create `QSessionStoreHelperTest.java` for non-QBit classpath tests
- [x] Add `clearOIDCProviderMetadataCache()` for test stability
- [ ] Add session store hooks to `OAuth2AuthenticationModule` (when QBit is on classpath)
  - [ ] Store after finalCustomizeSession
  - [ ] Load before token re-derivation
  - [ ] Touch for sliding expiration
- [ ] Add session store hooks to `Auth0AuthenticationModule`
- [ ] Add `getSessionStore()` helper to `QInstance`

### Phase 6: Testing & Verification
- [ ] Unit tests for all providers
- [ ] Integration tests with OAuth2 + each provider
- [ ] TTL expiration tests
- [ ] Sliding expiration tests
- [ ] Backwards compatibility tests
- [ ] Redis failover tests
- [ ] Load/concurrency tests

### Phase 7: Documentation
- [ ] Update CLAUDE.md with session store info
- [ ] Add usage examples to QBit
- [ ] Update issue #336 with implementation notes

## Verification

1. Unit tests for all three providers (store/load/remove/touch/cleanExpired)
2. Integration test with OAuth2AuthenticationModule + each provider
3. Test TTL expiration and sliding expiration behavior
4. Test cleanup process scheduling (table-based)
5. Test Redis connection handling and failover
6. Verify backwards compatibility (sessionStoreEnabled=false = existing behavior)
7. Load test with concurrent sessions
