## build docker images

build example

```bash
infrastructure/scripts/build-docker-images.sh rest-customers 1.0 --build --push
```

## deploy rest-xxx using helm charts

create namespace
```bash
kubectl create namespace ora-services
```
## deploy rest-customers using helm chart
```bash
helm install rest-customers infrastructure/helm/rest-customers --namespace ora-services --set image.tag=1.1 --create-namespace
```
## undeploy rest-customers using helm chart
```bash
helm uninstall rest-customers --namespace ora-services
```


## deploy rest-locations using helm chart
```bash
helm install rest-locations infrastructure/helm/rest-locations --namespace ora-services --set image.tag=1.1 --create-namespace
```

## undeploy rest-locations using helm chart
```bash
helm uninstall rest-locations --namespace ora-services
```