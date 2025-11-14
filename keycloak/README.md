# Keycloak Configuration

This directory contains the Keycloak realm configuration for the Event-Sourced Order Platform. Keycloak provides OAuth2/OIDC authentication and authorization for all API endpoints.

## Overview

The Order Platform uses Keycloak for:
- **Authentication**: JWT token issuance and validation
- **Authorization**: Role-based access control (RBAC)
- **User Management**: Test users for local development
- **Client Management**: Multiple clients for different access patterns

## Realm Structure

### Realm: `order-platform`

The `order-platform` realm is the security domain for all services and users.

**Key Settings**:
- **Access Token Lifespan**: 5 minutes (300 seconds)
- **SSO Session Idle Timeout**: 30 minutes (1800 seconds)
- **SSO Session Max Lifespan**: 10 hours (36000 seconds)
- **Brute Force Protection**: Enabled (5 failed attempts locks account for 15 minutes)
- **SSL Required**: External (required for production, optional for localhost)

## Clients

### 1. api-gateway (Confidential Client)

**Purpose**: Service-to-service authentication for the API Gateway

**Configuration**:
- **Client ID**: `api-gateway`
- **Client Type**: Confidential (requires client secret)
- **Client Secret**: `api-gateway-secret-change-in-production` ⚠️ **Change in production!**
- **Access Type**: Confidential
- **Standard Flow**: Enabled (Authorization Code)
- **Direct Access Grants**: Enabled (Resource Owner Password Credentials)
- **Service Accounts**: Enabled

**Valid Redirect URIs**:
- `http://localhost:8080/*` (local development)
- `https://api.orderplatform.com/*` (production)

**Use Cases**:
- API Gateway validates JWT tokens from this client
- Service-to-service authentication

### 2. web-app (Public Client)

**Purpose**: Browser-based web application using Authorization Code Flow with PKCE

**Configuration**:
- **Client ID**: `web-app`
- **Client Type**: Public (no client secret)
- **Standard Flow**: Enabled (Authorization Code with PKCE)
- **PKCE**: Required (S256 code challenge method)
- **Direct Access Grants**: Disabled (security best practice)
- **Implicit Flow**: Disabled (deprecated)

**Valid Redirect URIs**:
- `http://localhost:3000/*` (local development)
- `https://app.orderplatform.com/*` (production)

**Use Cases**:
- Single Page Applications (React, Angular, Vue)
- Server-side rendered web applications

### 3. postman (Public Client)

**Purpose**: API testing with Postman or other API clients

**Configuration**:
- **Client ID**: `postman`
- **Client Type**: Public
- **Standard Flow**: Enabled
- **Direct Access Grants**: Enabled (for password grant type)

**Valid Redirect URIs**:
- `https://oauth.pstmn.io/v1/callback` (Postman OAuth callback)
- `http://localhost:*` (local testing)

**Use Cases**:
- Manual API testing with Postman
- Integration testing scripts
- Development and debugging

## Roles

The platform defines three realm-level roles for role-based access control:

### 1. admin

**Description**: Administrator role with full access to all commands and queries

**Permissions**:
- ✅ Create orders
- ✅ Approve orders
- ✅ Reject orders
- ✅ Cancel orders
- ✅ Ship orders
- ✅ Add/remove items
- ✅ Query orders
- ✅ Search orders

**Use Cases**: System administrators, platform operators

### 2. ops

**Description**: Operations role with access to queries and operational commands

**Permissions**:
- ✅ Approve orders
- ✅ Ship orders
- ✅ Query orders
- ✅ Search orders
- ❌ Create orders (typically done by customers/systems)
- ❌ Reject orders
- ❌ Cancel orders

**Use Cases**: Warehouse staff, fulfillment operators, customer service

### 3. analyst

**Description**: Analyst role with read-only access to queries

**Permissions**:
- ✅ Query orders
- ✅ Search orders
- ❌ All command operations (create, approve, reject, cancel, ship)

**Use Cases**: Business analysts, reporting systems, dashboards

## Test Users

The realm includes three pre-configured test users for local development:

| Username | Password | Role | Description |
|----------|----------|------|-------------|
| `admin@test.com` | `test123` | admin | Full access administrator |
| `ops@test.com` | `test123` | ops | Operations user |
| `analyst@test.com` | `test123` | analyst | Read-only analyst |

⚠️ **Security Warning**: These test users are for **local development only**. Never use these credentials in production environments!

## Importing the Realm

### Option 1: Docker Compose (Automatic Import)

When using the provided `docker-compose.yml`, the realm is automatically imported on Keycloak startup:

```bash
# Start all services including Keycloak
make up

# Or manually with docker-compose
docker-compose up -d
```

The realm export file is mounted as a volume and imported automatically.

### Option 2: Manual Import via Admin Console

1. Start Keycloak:
   ```bash
   docker run -p 8080:8080 \
     -e KEYCLOAK_ADMIN=admin \
     -e KEYCLOAK_ADMIN_PASSWORD=admin \
     quay.io/keycloak/keycloak:24.0.0 start-dev
   ```

2. Access the Admin Console:
   - URL: http://localhost:8080
   - Username: `admin`
   - Password: `admin`

3. Import the realm:
   - Click **"Add realm"** in the top-left dropdown
   - Click **"Select file"** and choose `keycloak/realm-export.json`
   - Click **"Create"**

### Option 3: Keycloak CLI Import

```bash
# Using Keycloak CLI
/opt/keycloak/bin/kc.sh import \
  --file /path/to/realm-export.json \
  --override true
```

## Getting Access Tokens

### Using Password Grant (Direct Access)

**For testing with curl or Postman:**

```bash
curl -X POST "http://localhost:8080/realms/order-platform/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=postman" \
  -d "username=admin@test.com" \
  -d "password=test123"
```

**Response:**
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expires_in": 300,
  "refresh_expires_in": 1800,
  "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "Bearer",
  "not-before-policy": 0,
  "session_state": "...",
  "scope": "profile email"
}
```

### Using Authorization Code Flow (Web Applications)

1. **Redirect user to authorization endpoint:**
   ```
   http://localhost:8080/realms/order-platform/protocol/openid-connect/auth
     ?client_id=web-app
     &redirect_uri=http://localhost:3000/callback
     &response_type=code
     &scope=openid profile email
     &code_challenge=<PKCE_CODE_CHALLENGE>
     &code_challenge_method=S256
   ```

2. **User logs in and authorizes**

3. **Exchange authorization code for tokens:**
   ```bash
   curl -X POST "http://localhost:8080/realms/order-platform/protocol/openid-connect/token" \
     -H "Content-Type: application/x-www-form-urlencoded" \
     -d "grant_type=authorization_code" \
     -d "client_id=web-app" \
     -d "code=<AUTHORIZATION_CODE>" \
     -d "redirect_uri=http://localhost:3000/callback" \
     -d "code_verifier=<PKCE_CODE_VERIFIER>"
   ```

### Using Client Credentials (Service-to-Service)

```bash
curl -X POST "http://localhost:8080/realms/order-platform/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials" \
  -d "client_id=api-gateway" \
  -d "client_secret=api-gateway-secret-change-in-production"
```

## Using Access Tokens with the API

Once you have an access token, include it in the `Authorization` header:

```bash
curl -X GET "http://localhost:8080/api/v1/orders" \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
```

## JWT Token Structure

The access tokens are JWTs with the following structure:

**Header:**
```json
{
  "alg": "RS256",
  "typ": "JWT",
  "kid": "..."
}
```

**Payload:**
```json
{
  "exp": 1699876543,
  "iat": 1699876243,
  "jti": "...",
  "iss": "http://localhost:8080/realms/order-platform",
  "aud": "account",
  "sub": "user-uuid",
  "typ": "Bearer",
  "azp": "postman",
  "session_state": "...",
  "realm_access": {
    "roles": ["admin"]
  },
  "scope": "profile email",
  "email_verified": true,
  "preferred_username": "admin@test.com",
  "email": "admin@test.com"
}
```

**Key Claims**:
- `sub`: User ID (UUID)
- `realm_access.roles`: Array of assigned roles
- `preferred_username`: Username/email
- `exp`: Token expiration timestamp
- `iss`: Token issuer (Keycloak realm URL)

## Role-Based Access Control (RBAC)

The API Gateway enforces RBAC based on the `realm_access.roles` claim in the JWT:

### Command Endpoints (Write Operations)

| Endpoint | Method | Required Roles |
|----------|--------|----------------|
| `/api/v1/orders` | POST | `admin`, `ops` |
| `/api/v1/orders/{id}/approve` | POST | `admin`, `ops` |
| `/api/v1/orders/{id}/reject` | POST | `admin` |
| `/api/v1/orders/{id}/cancel` | POST | `admin` |
| `/api/v1/orders/{id}/ship` | POST | `admin`, `ops` |
| `/api/v1/orders/{id}/items` | POST | `admin`, `ops` |
| `/api/v1/orders/{id}/items/{sku}` | DELETE | `admin`, `ops` |

### Query Endpoints (Read Operations)

| Endpoint | Method | Required Roles |
|----------|--------|----------------|
| `/api/v1/orders` | GET | `admin`, `ops`, `analyst` |
| `/api/v1/orders/{id}` | GET | `admin`, `ops`, `analyst` |
| `/api/v1/orders/search` | GET | `admin`, `ops`, `analyst` |

### Authorization Flow

```
1. Client sends request with JWT token
   ↓
2. API Gateway validates JWT signature with Keycloak public key
   ↓
3. API Gateway checks token expiration
   ↓
4. API Gateway extracts realm_access.roles claim
   ↓
5. API Gateway checks if user has required role for endpoint
   ↓
6. If authorized: Forward request to backend service
   If unauthorized: Return 403 Forbidden
```

## Production Configuration

### Security Hardening

For production deployments, ensure the following:

1. **Change Client Secrets**:
   ```bash
   # Generate a strong secret
   openssl rand -base64 32
   
   # Update in Keycloak Admin Console:
   # Clients → api-gateway → Credentials → Regenerate Secret
   ```

2. **Enable SSL/TLS**:
   - Set `sslRequired` to `all` (not just `external`)
   - Configure valid SSL certificates
   - Update `frontendUrl` to use HTTPS

3. **Update Redirect URIs**:
   - Remove localhost URIs
   - Add only production domain URIs
   - Use exact URIs (avoid wildcards when possible)

4. **Disable Test Users**:
   - Delete or disable test users (`admin@test.com`, etc.)
   - Create real user accounts with strong passwords
   - Enable email verification

5. **Configure Token Lifespans**:
   - Consider shorter access token lifespan (e.g., 5 minutes)
   - Enable refresh tokens for long-lived sessions
   - Configure appropriate session timeouts

6. **Enable Additional Security Features**:
   - Multi-factor authentication (MFA/2FA)
   - Password policies (complexity, expiration)
   - Account lockout policies
   - IP whitelisting (if applicable)

### Environment-Specific Configuration

**Development**:
- Use realm export file as-is
- Test users enabled
- Relaxed SSL requirements

**Staging**:
- Change client secrets
- Enable SSL
- Use staging-specific redirect URIs
- Keep test users for testing

**Production**:
- Strong client secrets stored in Kubernetes Secrets
- SSL required for all connections
- Production redirect URIs only
- Real user accounts only
- Enable all security features
- Regular security audits

## Troubleshooting

### Token Validation Fails

**Symptom**: API returns 401 Unauthorized

**Possible Causes**:
1. Token expired (check `exp` claim)
2. Invalid signature (Keycloak public key mismatch)
3. Wrong issuer URL in application configuration
4. Token not included in Authorization header

**Solution**:
```bash
# Verify token is valid
curl -X POST "http://localhost:8080/realms/order-platform/protocol/openid-connect/token/introspect" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "token=<ACCESS_TOKEN>" \
  -d "client_id=postman"

# Check application configuration
# Ensure spring.security.oauth2.resourceserver.jwt.issuer-uri matches Keycloak URL
```

### Insufficient Permissions

**Symptom**: API returns 403 Forbidden

**Possible Causes**:
1. User doesn't have required role
2. Role not included in JWT token
3. Role mapping incorrect

**Solution**:
```bash
# Decode JWT to check roles
echo "<ACCESS_TOKEN>" | cut -d. -f2 | base64 -d | jq .

# Verify user has role in Keycloak Admin Console:
# Users → <username> → Role Mappings
```

### Cannot Import Realm

**Symptom**: Import fails or realm not created

**Possible Causes**:
1. JSON syntax error
2. Keycloak version incompatibility
3. Realm already exists

**Solution**:
```bash
# Validate JSON
jq . keycloak/realm-export.json

# Check Keycloak logs
docker logs keycloak

# Delete existing realm and retry
# (Admin Console → Realm Settings → Delete)
```

## Additional Resources

- [Keycloak Documentation](https://www.keycloak.org/documentation)
- [OAuth 2.0 RFC 6749](https://tools.ietf.org/html/rfc6749)
- [OpenID Connect Core 1.0](https://openid.net/specs/openid-connect-core-1_0.html)
- [Spring Security OAuth2 Resource Server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/index.html)
- [JWT.io](https://jwt.io/) - JWT decoder and debugger

## Support

For issues or questions:
1. Check the troubleshooting section above
2. Review Keycloak logs: `docker logs keycloak`
3. Review API Gateway logs for authentication errors
4. Consult the main project README.md for architecture details
