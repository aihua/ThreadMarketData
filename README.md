# Thread Market Data
A market data gateway to subscribe to Bitcoin market data and make it available on ZeroMQ.

See [ThreadKDBFeed](https://github.com/nof20/ThreadKDBFeed) for a component to subscribe to the ZeroMQ data and publish to KDB, and [ThreadKDB](https://github.com/nof20/ThreadKDB) for KDB schema and configuration.  If you are new to ZeroMQ, consider [SimpleZeroMQSubscriber](https://github.com/nof20/SimpleZeroMQSubscriber) to visualize the data published by this component.

See [Documentation](https://github.com/nof20/ThreadMarketData/wiki) and a list of [Current Issues](https://github.com/nof20/ThreadMarketData/issues).

## Build and run instructions

* Use Maven to build and run the Java code.
* Use the makefile to build Java and push code & configuration to Docker, or build a tar.gz for manual deployment.
* Run built code using the shell script.

## Exchanges supported

* Coinbase (in progress).
