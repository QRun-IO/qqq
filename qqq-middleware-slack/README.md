# QQQ Middleware - Slack

Slack integration middleware. Build Slack bots and apps with QQQ.

## Features

- Slack bot framework
- Slash command handling
- Interactive message components
- Event subscriptions

## Usage

```java
SlackBackendMetaData slackBackend = new SlackBackendMetaData()
   .withName("slack-bot")
   .withBotToken(System.getenv("SLACK_BOT_TOKEN"))
   .withSigningSecret(System.getenv("SLACK_SIGNING_SECRET"));

instance.addBackend(slackBackend);
```

## License

GNU Affero General Public License v3.0
