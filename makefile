all:
	mvn assembly:assembly
	docker build -t nof20/threadmarketdata:latest .
	docker push nof20/threadmarketdata
