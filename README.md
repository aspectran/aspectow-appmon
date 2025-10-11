# Aspectow AppMon

[![Build Status](https://github.com/aspectran/aspectow-appmon/workflows/Java%20CI/badge.svg)](https://github.com/aspectran/aspectow-appmon/actions?query=workflow%3A%22Java+CI%22)
[![Maven Central Version](https://img.shields.io/maven-central/v/com.aspectran/aspectow-appmon)](https://central.sonatype.com/artifact/com.aspectran/aspectow-appmon)
[![javadoc](https://javadoc.io/badge2/com.aspectran/aspectow-appmon/javadoc.svg)](https://javadoc.io/doc/com.aspectran/aspectow-appmon)
[![License](https://img.shields.io/:license-apache-brightgreen.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)


Aspectow AppMon is a powerful monitoring tool that provides an integrated, real-time view of logs, events, and metrics for your Aspectran-based application servers. It helps you observe and analyze the behavior of your applications, making it easier to debug issues, optimize performance, and ensure stability.

## Features

- **Real-time Monitoring**: Observe logs, application events, and performance metrics as they happen.
- **Agent-Based Collection**: A lightweight agent collects data from your Aspectran application without significant overhead.
- **Live Data Streaming**: Supports WebSockets and long-polling for streaming live data to the monitoring dashboard.
- **Data Persistence**: Persists event and metric data for historical analysis and reporting.
- **Activity Charting**: Visualize application activity with real-time charts.
- **Extensible Exporters**: Easily extend the system to export data to other monitoring systems or databases.

## Requirements

- Java 21 or later
- Maven 3.9.4 or later (the included Maven wrapper is recommended)

## Building from Source

To build Aspectow AppMon from the source code, follow these steps:

1.  **Clone the repository:**
    ```sh
    git clone https://github.com/aspectran/aspectow-appmon.git
    ```

2.  **Navigate to the project directory:**
    ```sh
    cd aspectow-appmon
    ```

3.  **Build the project:**
    Use the provided build script to compile the code and package the application. The `rebuild` command cleans the project before building.
    ```sh
    ./build rebuild
    ```

## Running the Demo

After a successful build, you can run the included demo application to see Aspectow AppMon in action.

1.  **Start the demo server:**
    ```sh
    ./build demo
    ```

2.  **Access the dashboard:**
    Open your web browser and navigate to [http://localhost:8082](http://localhost:8082).

The demo will showcase the real-time monitoring dashboard with sample data.

## Contributing

Contributions are welcome! If you would like to contribute, please feel free to fork the repository and submit a pull request. For major changes, please open an issue first to discuss what you would like to change.

## License

Aspectow AppMon is licensed under the [Apache License 2.0](LICENSE.txt).
