# Aspectow AppMon

[![Build Status](https://github.com/aspectran/aspectow/workflows/Java%20CI/badge.svg)](https://github.com/aspectran/aspectow/actions?query=workflow%3A%22Java+CI%22)
[![Maven Central Version](https://img.shields.io/maven-central/v/com.aspectran/aspectow-appmon)](https://central.sonatype.com/artifact/com.aspectran/aspectow-appmon)
[![License](https://img.shields.io/:license-apache-brightgreen.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)

Aspectow AppMon is a powerful, real-time monitoring engine and dashboard for Aspectran-based applications. As a core module of the **Aspectow** platform, it provides deep observability into application logs, events, and performance metrics, helping you ensure system stability and optimize performance.

<img width="1042" alt="AppMon Dashboard Screenshot" src="https://appmon-assets.aspectran.com/screenshots/appmon-v3-dashboard-dark.jpg">

## Features

- **Real-time Monitoring**: Observe logs, application events, and performance metrics as they happen.
- **Cluster Integration**: Seamlessly works with **Aspectow Node Manager** for monitoring distributed nodes.
- **Live Data Streaming**: Supports WebSockets and long-polling for streaming live data to the dashboard.
- **Data Persistence**: Persists event and metric data for historical analysis and reporting.
- **Activity Charting**: Visualize application activity with real-time charts.
- **Extensible Exporters**: Easily extend the system to export data to other monitoring systems or databases.

## Requirements

- Java 21 or later
- Maven 3.9.4 or later (the included Maven wrapper in the project root is recommended)

## Building from Source

Aspectow AppMon is managed as part of the [Aspectow](https://github.com/aspectran/aspectow) project. To build it from source:

1.  **Clone the main repository:**
    ```sh
    git clone https://github.com/aspectran/aspectow.git
    ```

2.  **Navigate to the project directory:**
    ```sh
    cd aspectow
    ```

3.  **Build the entire platform:**
    ```sh
    ./mvnw clean install
    ```

## Running the Demo

You can run the standalone AppMon demo located in the `appmon-demo` directory.

1.  **Navigate to the demo directory:**
    ```sh
    cd appmon-demo
    ```

2.  **Start the demo server:**
    ```sh
    ./app/bin/shell.sh
    ```

3.  **Access the dashboard:**
    Open your web browser and navigate to [http://localhost:8083](http://localhost:8083).

## License

Aspectow AppMon is licensed under the [Apache License 2.0](LICENSE.txt).
