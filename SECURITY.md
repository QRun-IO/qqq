# Security Policy

## Supported Versions

| Version | Supported          | Notes |
| ------- | ------------------ | ----- |
| 0.36.x  | :white_check_mark: | Current development |
| 0.35.x  | :white_check_mark: | Latest stable |
| < 0.35  | :x:                | End of life |

We recommend using the latest stable version for security updates.

## Reporting a Vulnerability

**Security vulnerabilities should NEVER be reported publicly.**

### Private Reporting

Use GitHub's private vulnerability reporting:
**[Report a vulnerability](https://github.com/QRun-IO/qqq/security/advisories/new)**

Or email: **security@qrun.io**

### What to Include

- Description of the vulnerability
- Steps to reproduce
- Potential impact
- QQQ version, Java version, OS details

### Response Timeline

| Stage | Timeline |
|-------|----------|
| Initial response | 24 hours |
| Assessment | 3 business days |
| Resolution | Based on severity |

## Automated Security Scanning

This repository uses automated security scanning:

| Tool | Purpose | Status |
|------|---------|--------|
| **CodeQL** | Static analysis for vulnerabilities | Enabled |
| **Dependabot** | Dependency vulnerability alerts | Enabled |
| **Secret Scanning** | Detect leaked credentials | Enabled |
| **Push Protection** | Block commits with secrets | Enabled |

### Dependency Updates

- Dependabot automatically creates PRs for vulnerable dependencies
- Security updates are prioritized and typically merged within 48 hours
- All dependencies are regularly audited

## Security Features

QQQ includes built-in security features:

- **Authentication** - Table-based, OAuth2, Auth0 support
- **Authorization** - Role-based access control with security keys
- **Input Validation** - Comprehensive sanitization
- **Audit Logging** - Configurable audit trails
- **Password Hashing** - PBKDF2 with SHA256 (100k iterations)

## For Contributors

- All PRs require passing CodeQL analysis
- Dependencies must not introduce HIGH/CRITICAL vulnerabilities
- Secrets must never be committed (push protection enabled)
- Follow secure coding practices per [CODE_STYLE.md](CODE_STYLE.md)

## Contact

- **Security issues**: security@qrun.io
- **General contact**: contact@qrun.io
- **Organization**: [QRun-IO](https://github.com/QRun-IO)
