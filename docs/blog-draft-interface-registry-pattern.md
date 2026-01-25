# [ Core/QBit Architecture ] - The Interface + Registry Pattern

When qqq-backend-core needs optional functionality from a qbit, reflection is tempting. Don't do it. Here's the pattern that actually works.

## The Problem We Had

PR #381 introduced session store integration. The original approach used reflection:

```java
// BAD - Core reaching out to qbit via reflection
private static final String CONTEXT_CLASS = "com.kingsrook.qbits.sessionstore.QSessionStoreQBitContext";

private static Object getProvider() throws Exception {
   Class<?> contextClass = Class.forName(CONTEXT_CLASS);
   Method getProviderMethod = contextClass.getMethod("getProvider");
   return getProviderMethod.invoke(null);
}
```

This inverts the dependency direction. Core should never know about specific qbits - even reflectively.

**Why reflection is wrong here:**
- Brittle: class/method name changes break silently at runtime
- No compile-time safety: errors only appear when code executes
- Hard to refactor: IDEs can't track usage through reflection
- Violates architecture: core depends on qbit (backwards)

## The Pattern: Interface + Registry

The fix follows a pattern already established in QQQ (see `SpaNotFoundHandlerRegistry`):

1. Core defines the interface
2. Core provides a registry
3. QBit implements the interface
4. QBit registers itself on startup
5. Core uses the registry with graceful fallback

### Step 1: Define Interface in Core

```java
// qqq-backend-core/.../QSessionStoreProviderInterface.java
public interface QSessionStoreProviderInterface
{
   void store(String sessionUuid, QSession session, Duration ttl);
   Optional<QSession> load(String sessionUuid);
   void remove(String sessionUuid);
   void touch(String sessionUuid);
   Duration getDefaultTtl();

   // Combined operation - providers can override with optimized impl
   default Optional<QSession> loadAndTouch(String sessionUuid)
   {
      Optional<QSession> session = load(sessionUuid);
      session.ifPresent(s -> touch(sessionUuid));
      return session;
   }
}
```

### Step 2: Create Registry in Core

```java
// qqq-backend-core/.../QSessionStoreRegistry.java
public class QSessionStoreRegistry
{
   private static final QSessionStoreRegistry INSTANCE = new QSessionStoreRegistry();
   private QSessionStoreProviderInterface provider = null;

   public static QSessionStoreRegistry getInstance() { return INSTANCE; }

   public synchronized void register(QSessionStoreProviderInterface provider)
   {
      this.provider = provider;
      LOG.info("Registered session store provider", logPair("provider", provider.getClass().getName()));
   }

   public boolean isAvailable() { return provider != null; }

   public Optional<QSessionStoreProviderInterface> getProvider()
   {
      return Optional.ofNullable(provider);
   }

   public synchronized void clear() { provider = null; }  // For testing
}
```

### Step 3: QBit Extends Interface

```java
// qbit-session-store/.../QSessionStoreProviderInterface.java
public interface QSessionStoreProviderInterface
   extends com.kingsrook.qqq.backend.core.modules.authentication.QSessionStoreProviderInterface
{
   // QBit-specific methods
   void cleanExpired();
   String status();
   default void configure(QSessionStoreQBitConfig config) {}
}
```

### Step 4: QBit Registers on Startup

```java
// qbit-session-store/.../QSessionStoreQBitProducer.java
@Override
public void produce(QInstance qInstance, String namespace) throws QException
{
   // ... validation and setup ...

   QSessionStoreProviderInterface provider = QSessionStoreProviderFactory.createProvider(config);

   // Store in qbit context (for qbit-internal use)
   QSessionStoreQBitContext.setProvider(provider);

   // Register with core registry (for core to use)
   QSessionStoreRegistry.getInstance().register(provider);
}
```

### Step 5: Core Uses Registry with Fallback

```java
// qqq-backend-core/.../QSessionStoreHelper.java
public static Optional<QSession> loadSession(String sessionUuid)
{
   Optional<QSessionStoreProviderInterface> provider = QSessionStoreRegistry.getInstance().getProvider();
   if(provider.isEmpty())
   {
      return Optional.empty();  // Graceful fallback when qbit not present
   }

   try
   {
      return provider.get().load(sessionUuid);
   }
   catch(Exception e)
   {
      LOG.warn("Failed to load session", e, logPair("sessionUuid", sessionUuid));
      return Optional.empty();
   }
}
```

## Optimizing for Remote Stores

When your provider talks to Redis, a database, or any remote service, round-trips matter. The interface should support combined operations.

**Before (two round-trips):**
```java
Optional<QSession> session = QSessionStoreHelper.loadSession(uuid);
if(session.isPresent())
{
   QSessionStoreHelper.touchSession(uuid);  // Second call to remote store
   return session.get();
}
```

**After (one round-trip):**
```java
Optional<QSession> session = QSessionStoreHelper.loadAndTouchSession(uuid);
if(session.isPresent())
{
   return session.get();
}
```

The interface provides a default implementation that calls `load()` then `touch()`. Providers override with optimized versions:

```java
// Redis provider - uses GETEX for atomic get+TTL-reset
@Override
public Optional<QSession> loadAndTouch(String sessionUuid)
{
   try(Jedis jedis = jedisPool.getResource())
   {
      String key = buildKey(sessionUuid);
      String sessionJson = jedis.getEx(key, GetExParams.getExParams().ex(defaultTtl.toSeconds()));
      if(sessionJson == null) return Optional.empty();
      return Optional.of(JsonUtils.toObject(sessionJson, QSession.class));
   }
}
```

## What Changed

**qqq-backend-core:**
- `QSessionStoreProviderInterface.java` (new) - interface definition
- `QSessionStoreRegistry.java` (new) - singleton registry
- `QSessionStoreHelper.java` - removed reflection, uses registry
- `OAuth2AuthenticationModule.java` - uses `loadAndTouchSession()`

**qbit-session-store:**
- `QSessionStoreProviderInterface.java` - extends core interface
- `QSessionStoreQBitProducer.java` - registers with core on startup
- `InMemorySessionStoreProvider.java` - added `getDefaultTtl()`, `loadAndTouch()`
- `TableBasedSessionStoreProvider.java` - added `getDefaultTtl()`, `loadAndTouch()`
- `RedisSessionStoreProvider.java` - added `getDefaultTtl()`, `loadAndTouch()`

## When to Use This Pattern

**Use Interface + Registry when:**
- Core needs functionality that's provided by an optional module
- Multiple implementations should be possible (Redis vs DB vs in-memory)
- The feature should work without the optional module (graceful degradation)

**Don't use this pattern when:**
- The dependency is mandatory (just add it to pom.xml)
- There's only one possible implementation
- The functionality is core to QQQ itself

## Existing Registries in QQQ

- `SpaNotFoundHandlerRegistry` - allows multiple SPAs to register 404 handlers
- `QSessionStoreRegistry` - session store provider registration

If you're building a qbit that provides optional functionality to core, follow this pattern. It keeps the architecture clean and the dependency direction correct.
