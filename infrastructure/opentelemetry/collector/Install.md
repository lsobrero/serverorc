## from 
[Install on kubernetes](https://opentelemetry.io/docs/platforms/kubernetes/getting-started/)

## using the helm chart
```bash
helm repo add otel https://open-telemetry.github.io/opentelemetry-helm-charts
helm repo update
```
# or
```bash
helm --namespace otel-collector install otel-collector open-telemetry/opentelemetry-collector --values infrastructure/opentelemetry/collector/values.yml \
  --namespace otel-collector --create-namespace
```


## using the helm chart as deployment
```bash
helm --namespace otel-collector install otel-deployment open-telemetry/opentelemetry-collector --set mode=deployment \
--set image.repository="ghcr.io/open-telemetry/opentelemetry-collector-releases/opentelemetry-collector-k8s" --set command.name="otelcol-k8s"
```


