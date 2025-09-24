# Refs:

* List of metrics: https://kubernetes.io/docs/reference/instrumentation/metrics/#list-of-stable-kubernetes-metrics
* Helm - Git: https://github.com/bitnami/charts/tree/main/bitnami/redis
* Helm - LGTM: https://github.com/grafana/helm-charts/tree/main/charts/lgtm-distributed
* Helm - Kube Prom Stack: https://github.com/prometheus-community/helm-charts/tree/main/charts/kube-prometheus-stack
* Helm - Grafana: https://github.com/grafana/helm-charts/blob/main/charts/grafana/values.yaml

# Prerequisites:

* Docker
* Kind (Local Kubernetes)
* Helm Chart

# Setup K8s cluster:

# Install Helm charts & Setup services:

### Prepare:

    kubectl create namespace monitoring && \
      kubectl create namespace app

  Create configs and secrets:

    kubectl apply -f k8s/constant/env-common.yaml -n monitoring && \
    kubectl apply -f k8s/constant/env-common.yaml -n app && \
    kubectl apply -f k8s/constant/grafana-admin.yaml

### Install monitoring stack:

  Add repo:

    helm repo add prometheus-community https://prometheus-community.github.io/helm-charts && \
      helm repo add grafana https://grafana.github.io/helm-charts && \
      helm repo update

  Install kube-prometheus-stack (included Grafana):
    
    helm install prom-stack prometheus-community/kube-prometheus-stack \
      -f k8s/monitoring/values-kube-prometheus-stack.yaml --timeout 10m0s \
      -n monitoring

  Install Tempo (tracing):

    helm install tempo grafana/tempo-distributed \
      -f k8s/monitoring/values-tempo.yaml \
      -n monitoring

  Install Loki (logs):

    helm install loki grafana/loki-distributed \
      -f k8s/monitoring/values-loki.yaml \
      -n monitoring

  Verify resources:
  
    kubectl get all -n monitoring

### Deploy App & K6 load simulator:

  Install Redis:

    helm install redis oci://registry-1.docker.io/bitnamicharts/redis \
      -f k8s/infras/values-redis.yaml \
      -n app

  Deploy app:
    
    docker build -t order-processing-system/cart-service services/cart
    docker build -t order-processing-system/inventory-service services/inventory
    
    kind load docker-image \
      order-processing-system/cart-service \
      order-processing-system/inventory-service

    kubectl apply -f k8s/services

  Verify:
  
    kubectl get all -n app

# Test:

### Run load test and monitor:

  
  Simulate 100 request/second for 1 hour:

    kubectl delete configmap k6-script-cart-request -n app && \
      kubectl create configmap k6-script-cart-request -n app --from-file=k6/scripts/cart-request.js && \
      kubectl apply -f k8s/k6/cart-request.yaml

  Accessing Grafana Dashboards (http://localhost:3000):

    kubectl port-forward -n monitoring svc/prom-stack-grafana 3000:80

# Extra:

    kubectl rollout restart deploy cart-service -n app

# Clean up:

    helm uninstall prom-stack
    helm uninstall tempo
    helm uninstall loki
  
    kubectl delete all --all -n monitoring
    kubectl delete all --all -n app
