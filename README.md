# Aspectow

[![Build Status](https://github.com/aspectran/aspectow/workflows/Java%20CI/badge.svg)](https://github.com/aspectran/aspectow/actions?query=workflow%3A%22Java+CI%22)
[![Maven Central Version](https://img.shields.io/maven-central/v/com.aspectran/aspectow)](https://central.sonatype.com/artifact/com.aspectran/aspectow)
[![javadoc](https://javadoc.io/badge2/com.aspectran/aspectow/javadoc.svg)](https://javadoc.io/doc/com.aspectran/aspectow-node)
[![License](https://img.shields.io/:license-apache-brightgreen.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)
![Aspectow](https://aspectran.com/images/header_aspectow.png)

Aspectow is an enterprise-grade platform management system and control plane for Aspectran-based web applications. Serving as the foundational framework for **Aspectow Enterprise**, it provides integrated **Observability**, **Controllability**, and **Security Governance** across distributed and clustered environments through the **Aspectow Management Console**.

## Core Architecture & Modules

Aspectow features a modular architecture designed to scale seamlessly from standalone servers to multi-node enterprise clusters:

- **`aspectow-node` (Node Manager & Clustering)**: Manages cluster topology and node-centric grouping. Offers **Direct Mode** (static peer-to-peer 1:1 connectivity) and **Gateway Mode** (dynamic Redis membership), Lettuce Redis-based distributed schedule locking (`RedisScheduledJobLockProvider`), virtualized WebSockets over Redis, remote command dispatching, and PBE token-based inter-node security.
- **`aspectow-appmon` (Application Telemetry & Observability)**: Collects performance metrics, traffic stats, and application logs. Features group-level time-series metric aggregation for auto-scaling environments, on-demand real-time log streaming with keep-alive signal detection, selective app monitoring, and MyBatis-backed persistent metric rollups.
- **`aspectow-console` (Control Plane Management Console)**: Web-based central management console providing dedicated control panels for cluster administration, real-time framework introspection (Anatomy), security vault management (PBE Credentials), role-based user management (RBAC), and observability telemetry dashboards.

## Key Features

- **Distributed Clustering & Node Management**
  - **Node-Centric Grouping**: Assigns logical group identities at the node level, enabling applications on a node to inherit group parameters for unified control.
  - **Distributed Schedule Lock**: Guarantees single-node task execution across clusters using Redis `SET NX` locks (`releasedOnUnlock=false` prevents clock drift duplicates).
  - **Virtual WebSocket & Remote Command Dispatching**: Routes WebSocket frames via Redis Pub/Sub across private networks and dispatches async control commands to targeted nodes.
- **Observability & Telemetry (AppMon)**
  - **Group-Level Aggregation**: Aggregates time-series performance metrics (TPS, error rate, active sessions) by group ID to maintain continuous metrics during container auto-scaling.
  - **On-Demand Log Streaming**: Streams live application logs only when requested by administrators in the console UI, automatically stopping when session heartbeats fade.
- **Framework Introspection & Anatomy**
  - Live inspection of running Aspectran rules (Translets, Beans, Aspects, Schedules).
  - Interactive AsEL expression tester, APON ↔ JSON/XML converters, and path matching utilities.
  - Automatic sensitive data masking (`********`) for passwords, secret keys, and passphrases during APON serialization.
- **Security & Vault Governance**
  - Role-Based Access Control (RBAC) with detailed login audit logs.
  - PBE-encrypted credential vault supporting `SIMPLE`, `PERSISTENT`, and `TIME_LIMITED` security tokens for sensitive properties.

## Requirements

- **Java**: 21 or later
- **Maven**: 3.9.4 or later (the included Maven wrapper `./build.sh` is recommended)
- **Redis** (Optional): Required for cluster mode, distributed locking, and virtual WebSocket relaying.

## Building from Source

To build Aspectow from the source code, run the following commands:

1. Clone the repository:
   ```sh
   git clone https://github.com/aspectran/aspectow.git
   ```

2. Navigate to the project directory:
   ```sh
   cd aspectow
   ```

3. Build the project:
   ```sh
   ./build.sh rebuild
   ```

## Running the Demo

After building the project, launch the included Aspectow Management Console demo:

1. Start the demo server:
   ```sh
   ./build.sh demo
   ```
   *(Note: If the project has not been built yet, `./build.sh demo` will automatically compile and package the required dependencies before starting.)*

2. Access the console in your web browser:
   [http://localhost:8082/console](http://localhost:8082/console)

## License

Aspectow is licensed under the [Apache License 2.0](LICENSE.txt).
