# Employee & Department microservices on EKS

```
client --> employee-service (LoadBalancer :80 -> 8081) --> department-service (ClusterIP :8082)
```

## 1. Run locally
```bash
docker compose up --build

curl -X POST localhost:8082/api/departments -H 'Content-Type: application/json' \
  -d '{"name":"Engineering","location":"Hyderabad"}'
curl -X POST localhost:8081/api/employees -H 'Content-Type: application/json' \
  -d '{"name":"Ravi","email":"ravi@example.com","departmentId":1}'
curl localhost:8081/api/employees/1/details
```

## 2. Push images to ECR (ap-south-2)
```bash
export AWS_REGION=ap-south-2
export ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
export REGISTRY=$ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com

aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $REGISTRY

for svc in department-service employee-service; do
  aws ecr create-repository --repository-name $svc --region $AWS_REGION || true
  docker build -t $REGISTRY/$svc:latest ./$svc
  docker push $REGISTRY/$svc:latest
done
```

## 3. Create the EKS cluster
```bash
eksctl create cluster --name demo-cluster --region ap-south-2 \
  --nodegroup-name workers --node-type t3.medium --nodes 2
aws eks update-kubeconfig --name demo-cluster --region ap-south-2
kubectl get nodes
```
(If t3.medium isn't offered in your AZs, pick another type, e.g. m5.large.)

## 4. Deploy
```bash
# replace the placeholder account id in the manifests
sed -i "s/<AWS_ACCOUNT_ID>/$ACCOUNT_ID/g" k8s/*.yaml

kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/department.yaml
kubectl apply -f k8s/employee.yaml
kubectl get pods,svc -n microservices
```

## 5. Test
```bash
LB=$(kubectl get svc employee-service -n microservices -o jsonpath='{.status.loadBalancer.ingress[0].hostname}')
curl http://$LB/api/employees
```
Creating a department requires a call inside the cluster (it's ClusterIP):
```bash
kubectl port-forward svc/department-service 8082:8082 -n microservices
curl -X POST localhost:8082/api/departments -H 'Content-Type: application/json' -d '{"name":"Engineering","location":"Hyderabad"}'
```

## 6. Clean up (avoid charges)
```bash
kubectl delete -f k8s/
eksctl delete cluster --name demo-cluster --region ap-south-2
```

## Notes
- H2 is in-memory: data is lost on restart and each replica has its own copy. Use Amazon RDS (PostgreSQL/MySQL) for real use.
- Using Java 25 to match your CI workflow; Spring Boot 3.5.x supports it. Bump the parent version to the latest patch.
