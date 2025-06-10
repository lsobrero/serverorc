## build docker images

Now you can use the script like this:
infrastructure/scripts/build-docker-images.sh customersrv - Build customersrv with latest tag
infrastructure/scripts/build-docker-images.sh locationsrv v1.0 - Build locationsrv with v1.0 tag
infrastructure/scripts/build-docker-images.sh customersrv v2.1 --build - Build customersrv with Maven build first
infrastructure/scripts/build-docker-images.sh locationsrv v1.5 --push - Build and push locationsrv

```bash
infrastructure/scripts/build-docker-images.sh customersrv v2.1 
```