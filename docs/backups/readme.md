# Refs:

* List of metrics: https://kubernetes.io/docs/reference/instrumentation/metrics/#list-of-stable-kubernetes-metrics
* Helm - Redis: https://github.com/bitnami/charts/tree/main/bitnami/redis
* Helm - LGTM: https://github.com/grafana/helm-charts/tree/main/charts/lgtm-distributed
* Helm - Kube Prom Stack: https://github.com/prometheus-community/helm-charts/tree/main/charts/kube-prometheus-stack
* Helm - Grafana: https://github.com/grafana/helm-charts/blob/main/charts/grafana/values.yaml
* Helm - Kafka: https://github.com/bitnami/charts/blob/main/bitnami/kafka/values.yaml

* Loki V2 config: https://grafana.com/docs/loki/v2.9.x/configure/

# Prerequisites:

* Docker
* Kind (Local Kubernetes)
* Helm Chart

# Setup K8s cluster:

# Install Helm charts & Setup services:

### Prepare:

    kubectl create namespace monitoring app infras 

  Create configs and secrets:

    kubectl apply -f k8s/constant/global-env-configmap.yaml -n app && \
      kubectl apply -f k8s/constant/grafana-secret.yaml -n monitoring

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

### Deploy Infrastructure Services:

  Install Redis:
  
    helm install redis oci://registry-1.docker.io/bitnamicharts/redis \
      -f k8s/infras/values-redis.yaml \
      -n infras
  
  Install Kafka:
  
    helm install kafka oci://registry-1.docker.io/bitnamicharts/kafka \
      -f k8s/infras/values-kafka.yaml \
      -n infras \
      --timeout 20m0s

  Verify:
    
    kubectl get all -n infras
  
  Create topic:

    kubectl exec -it kafka-controller-0 -n infras -c kafka -- bash

      /opt/bitnami/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --delete --topic cart-update-requests

      /opt/bitnami/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list

      /opt/bitnami/kafka/bin/kafka-topics.sh \
        --bootstrap-server localhost:9092 \
        --create \
        --replication-factor 3 \
        --partitions 3 \
        --topic cart-update-requests

      /opt/bitnami/kafka/bin/kafka-topics.sh \
        --bootstrap-server localhost:9092 \
        --describe \
        --topic cart-update-requests

      /opt/bitnami/kafka/bin/kafka-topics.sh --alter \
        --topic cart-update-requests \
        --partitions 3 \
        --bootstrap-server localhost:9092

      kafka-reassign-partitions.sh \
        --bootstrap-server <broker_address> \
        --topics-to-move-json-file topics.json \
        --broker-list "1,2,3" \
        --generate


### Deploy App & K6 load simulator:

  Deploy app:

    docker build -t nvhuu991/order-system-shop-service services/shop
    docker build -t nvhuu991/order-system-inventory-service services/inventory
    docker build -t nvhuu991/order-system-auth-service services/auth
    docker build -t nvhuu991/order-system-api-gateway services/api-gateway

    docker push nvhuu991/order-system-shop-service
    docker push nvhuu991/order-system-inventory-service
    docker push nvhuu991/order-system-auth-service
    docker push nvhuu991/order-system-api-gateway
    
    kind load docker-image \
      order-system/cart-service \
      order-system/inventory-service \
      order-system/shop-service

    kubectl apply -f k8s/services

  Verify:
  
    kubectl get all -n app
  
  Test:
    
    kubectl port-forward svc/shop-service -n app 8080:8080

      curl -X PUT "http://localhost:8080/api/v1/carts/user1" \
        -H "Content-Type: application/json" \
        -d '{ "userId": "user1", "versionNumber": 1, "entries": [{ "productId": "P001", "productName": "Laptop", "qtyAdjustment": 1, "action": "QTY_CHANGE" }] }'
      
      curl -X GET "http://localhost:8080/api/v1/carts/user1" -H "Accept: application/json"

# Test:

### Run load test and verify result:

    kubectl delete job cart-update-load-test -n app && \
      kubectl delete configmap cart-update-load-test-script -n app && \
      kubectl create configmap cart-update-load-test-script -n app --from-file=k6/scripts/cart-service/cart-update-load-test.js && \
      kubectl apply -f k8s/k6/cart-update-load-test.yaml

    kubectl logs -f -n app \
      $(kubectl get pods -n app --selector=job-name=cart-update-load-test --output=jsonpath='{.items[0].metadata.name}')

### Run soak test and monitor:

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
