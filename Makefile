CLUSTER_NAME := microservices-demo
CONFIG_FILE := kind-config.yaml
SKAFFOLD_CONFIG := skaffold.yaml
NAMESPACE := online-boutique
# 
# Reuquired: skaffold, Docker Desktop, Kind 
# if on mac run: softwareupdate --install-rosetta 
# 

.PHONY: init all create-cluster delete-cluster get-cluster-info configure-context create-namespace deploy clean

all: create-cluster create-namespace dev

create-cluster:
	@echo "Creating kind cluster..."
	kind create cluster --name $(CLUSTER_NAME) --config $(CONFIG_FILE)

delete-cluster:
	@echo "Deleting kind cluster..."
	kind delete cluster --name $(CLUSTER_NAME)

get-cluster-info:
	@echo "Getting cluster info..."
	kubectl cluster-info --context kind-$(CLUSTER_NAME)

create-namespace:
	@echo "Creating namespace $(NAMESPACE)..."
	kubectl create namespace $(NAMESPACE) || echo "Namespace $(NAMESPACE) already exists"
	kubectl get namespaces
	@echo "Switching kubectl context to kind cluster..."
	kubectl config use-context kind-$(CLUSTER_NAME) --namespace=dev

#  If you need to rebuild the images automatically as you refactor the code, run skaffold dev command
dev:
	@echo "verify connection to control plane...."
	kubectl get nodes
	@echo "Star application with Skaffold dev..."
	skaffold dev -f $(SKAFFOLD_CONFIG)

deploy:
	@echo "verify connection to control plane...."
	kubectl get nodes
	@echo "Building and deploying application with Skaffold..."
	skaffold run -f $(SKAFFOLD_CONFIG)


clean:
	@echo "Cleaning up Skaffold resources..."
	skaffold delete -f $(SKAFFOLD_CONFIG)
	@echo "Cleaning up kind cluster"
	kind delete cluster --name $(CLUSTER_NAME)
	# docker stop $(docker ps -q)
# docker stop $(docker ps -q)
# docker image prune --all


# # --------------- Terraform ---------------------
# tfdeploy:
# 	cd terraform
# 	terraform init