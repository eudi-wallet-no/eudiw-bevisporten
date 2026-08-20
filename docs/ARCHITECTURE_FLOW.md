# Application Flow and Interaction

Diagrams showing how applications interact in the system.

## High-Level Flow

```mermaid
graph TD
    A["EUDI Wallet - User"]
    
    A -->|1. Receive Credentials| B["ISSUANCE FLOW"]
    A -->|3. Present Credentials| C["VERIFICATION FLOW"]
    A -->|5. Verify Status| D["STATUS MANAGEMENT"]
    
    B --> B1["eudiw-idporten-login"]
    B1 --> B2["eudiw-oauth-server"]
    B2 --> B3["eudiw-issuer-ui / eudiw-issuer-ui-demo"]
    B3 --> B4["eudiw-issuer-server"]
    B4 --> B5["eudiw-authoritativ-sources-connector"]
    B4 --> B6["eudiw-status-list"]
    B6 --> A
    
    C --> C1["eudiw-verifier-service"]
    C1 --> C2["eudiw-status-list"]
    C2 --> C3["eudiw-verifier-demo (Relying Party)"]
    
    D --> D1["eudiw-status-list"]
    D1 -->|Status tracking| A
```

## Authentication & Session Flow

```mermaid
sequenceDiagram
    participant User
    participant IdPorten as eudiw-idporten-login
    participant OAuth as eudiw-oauth-server
    participant BYOB as eudiw-byob-service
    
    User->>IdPorten: Authenticate via ID-porten
    IdPorten->>User: Authorization code
    User->>OAuth: Exchange code for token
    OAuth->>BYOB: Create/manage wallet session
    BYOB->>User: Session established
```

## Credential Issuance Flow

```mermaid
sequenceDiagram
    participant Issuer
    participant UI as eudiw-issuer-ui
    participant Server as eudiw-issuer-server
    participant AuthSource as eudiw-authoritativ-sources
    participant StatusList as eudiw-status-list
    participant Wallet as EUDI Wallet
    
    Issuer->>UI: Request credential issuance
    UI->>Server: Submit credential data
    Server->>AuthSource: Fetch attribute data
    AuthSource->>Server: Return attributes
    Server->>StatusList: Initialize status entry
    StatusList->>Server: Status entry created
    Server->>Wallet: Deliver credential
    Wallet->>Issuer: Credential stored
```

## Credential Verification Flow

```mermaid
sequenceDiagram
    participant Wallet as EUDI Wallet
    participant Verifier as eudiw-verifier-service
    participant StatusList as eudiw-status-list
    participant RP as Relying Party
    
    Wallet->>Verifier: Present credential
    Verifier->>StatusList: Check revocation status
    StatusList->>Verifier: Status (active/revoked)
    alt Status is valid
        Verifier->>RP: ✓ Credential valid
        RP->>Wallet: Accept
    else Status is revoked
        Verifier->>RP: ✗ Credential revoked
        RP->>Wallet: Reject
    end
```

## Service Dependency Graph

```mermaid
graph LR
    IdPorten["eudiw-idporten-login"]
    OAuth["eudiw-oauth-server"]
    IssuerUI["eudiw-issuer-ui"]
    IssuerServer["eudiw-issuer-server"]
    AuthSource["eudiw-authoritativ-sources-connector"]
    StatusList["eudiw-status-list"]
    Verifier["eudiw-verifier-service"]
    VerifierDemo["eudiw-verifier-demo"]
    BYOB["eudiw-byob-service"]
    
    IssuerUI --> OAuth
    IssuerUI --> IssuerServer
    IssuerServer --> AuthSource
    IssuerServer --> StatusList
    OAuth --> IdPorten
    BYOB --> OAuth
    Verifier --> StatusList
    VerifierDemo --> Verifier
```

## Data Flow Through System

```mermaid
graph LR
    AuthData["Authentic Sources<br/>(External Data)"]
    AuthSource["eudiw-authoritativ-sources-connector"]
    IssuerServer["eudiw-issuer-server"]
    StatusList["eudiw-status-list"]
    Wallet["EUDI Wallet"]
    Verifier["eudiw-verifier-service"]
    RP["Relying Party"]
    
    AuthData -->|Fetch attributes| AuthSource
    AuthSource -->|Provide data| IssuerServer
    IssuerServer -->|Create entry| StatusList
    IssuerServer -->|Issue credential| Wallet
    Wallet -->|Present credential| Verifier
    Verifier -->|Check status| StatusList
    Verifier -->|Validate| RP
    RP -->|Decision| Wallet
```

---

## See Also

- [EUDI_ROLES.md](./EUDI_ROLES.md) - Role details and components

