all:
	mvn assembly:assembly
	docker build -t nof20/threadmarketdata:latest .
	docker push nof20/threadmarketdata

tar:
	mvn assembly:assembly
	tar cvf ThreadMarketData.tar ThreadMarketData.sh
	tar rf ThreadMarketData.tar *.properties
	tar rf ThreadMarketData.tar target/MarketData-*with-dependencies.jar
	gzip ThreadMarketData.tar
