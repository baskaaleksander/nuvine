SERVICES_DIR := ./services

SERVICES := auth chat config-server discovery file-storage gateway ingestion llm-router notification subscription vector workspace

.PHONY: help $(addsuffix -utest,$(SERVICES)) all-utest

help:
	@echo "Nuvine Services - Unit Test Targets"
	@echo "===================================="
	@echo ""
	@echo "Usage: make <service-name>-utest"
	@echo ""
	@echo "Available targets:"
	@echo "  auth-utest          - Run auth service unit tests"
	@echo "  chat-utest          - Run chat service unit tests"
	@echo "  config-server-utest - Run config-server service unit tests"
	@echo "  discovery-utest     - Run discovery service unit tests"
	@echo "  file-storage-utest  - Run file-storage service unit tests"
	@echo "  gateway-utest       - Run gateway service unit tests"
	@echo "  ingestion-utest     - Run ingestion service unit tests"
	@echo "  llm-router-utest    - Run llm-router service unit tests"
	@echo "  notification-utest  - Run notification service unit tests"
	@echo "  subscription-utest  - Run subscription service unit tests"
	@echo "  vector-utest        - Run vector service unit tests"
	@echo "  workspace-utest     - Run workspace service unit tests"
	@echo ""
	@echo "  all-utest           - Run unit tests for all services"

auth-utest:
	cd $(SERVICES_DIR)/auth && ./mvnw test

chat-utest:
	cd $(SERVICES_DIR)/chat && ./mvnw test

config-server-utest:
	cd $(SERVICES_DIR)/config-server && ./mvnw test

discovery-utest:
	cd $(SERVICES_DIR)/discovery && ./mvnw test

file-storage-utest:
	cd $(SERVICES_DIR)/file-storage && ./mvnw test

gateway-utest:
	cd $(SERVICES_DIR)/gateway && ./mvnw test

ingestion-utest:
	cd $(SERVICES_DIR)/ingestion && ./mvnw test

llm-router-utest:
	cd $(SERVICES_DIR)/llm-router && ./mvnw test

notification-utest:
	cd $(SERVICES_DIR)/notification && ./mvnw test

subscription-utest:
	cd $(SERVICES_DIR)/subscription && ./mvnw test

vector-utest:
	cd $(SERVICES_DIR)/vector && ./mvnw test

workspace-utest:
	cd $(SERVICES_DIR)/workspace && ./mvnw test

all-utest: $(addsuffix -utest,$(SERVICES))
	@echo "All unit tests completed!"
