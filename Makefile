SERVICES_DIR := ./services

SERVICES := auth chat file-storage ingestion llm-router notification subscription vector workspace

.PHONY: help $(addsuffix -utest,$(SERVICES)) $(addsuffix -ittest,$(SERVICES)) all-utest all-ittest

help:
	@echo "Nuvine Services - Test Targets"
	@echo "=============================="
	@echo ""
	@echo "Usage: make <service-name>-utest    (unit tests)"
	@echo "       make <service-name>-ittest   (integration tests)"
	@echo ""
	@echo "Unit Test Targets:"
	@echo "  auth-utest          - Run auth service unit tests"
	@echo "  chat-utest          - Run chat service unit tests"
	@echo "  file-storage-utest  - Run file-storage service unit tests"
	@echo "  ingestion-utest     - Run ingestion service unit tests"
	@echo "  llm-router-utest    - Run llm-router service unit tests"
	@echo "  notification-utest  - Run notification service unit tests"
	@echo "  subscription-utest  - Run subscription service unit tests"
	@echo "  vector-utest        - Run vector service unit tests"
	@echo "  workspace-utest     - Run workspace service unit tests"
	@echo "  all-utest           - Run unit tests for all services"
	@echo ""
	@echo "Integration Test Targets:"
	@echo "  auth-ittest          - Run auth service integration tests"
	@echo "  chat-ittest          - Run chat service integration tests"
	@echo "  file-storage-ittest  - Run file-storage service integration tests"
	@echo "  ingestion-ittest     - Run ingestion service integration tests"
	@echo "  llm-router-ittest    - Run llm-router service integration tests"
	@echo "  notification-ittest  - Run notification service integration tests"
	@echo "  subscription-ittest  - Run subscription service integration tests"
	@echo "  vector-ittest        - Run vector service integration tests"
	@echo "  workspace-ittest     - Run workspace service integration tests"
	@echo "  all-ittest           - Run integration tests for all services"

auth-ittest:
	cd $(SERVICES_DIR)/auth && ./mvnw clean test-compile failsafe:integration-test

chat-ittest:
	cd $(SERVICES_DIR)/chat && ./mvnw clean test-compile failsafe:integration-test

file-storage-ittest:
	cd $(SERVICES_DIR)/file-storage && ./mvnw clean test-compile failsafe:integration-test

ingestion-ittest:
	cd $(SERVICES_DIR)/ingestion && ./mvnw clean test-compile failsafe:integration-test

llm-router-ittest:
	cd $(SERVICES_DIR)/llm-router && ./mvnw clean test-compile failsafe:integration-test

notification-ittest:
	cd $(SERVICES_DIR)/notification && ./mvnw clean test-compile failsafe:integration-test

subscription-ittest:
	cd $(SERVICES_DIR)/subscription && ./mvnw clean test-compile failsafe:integration-test

vector-ittest:
	cd $(SERVICES_DIR)/vector && ./mvnw clean test-compile failsafe:integration-test

workspace-ittest:
	cd $(SERVICES_DIR)/workspace && ./mvnw clean test-compile failsafe:integration-test

auth-utest:
	cd $(SERVICES_DIR)/auth && ./mvnw test

chat-utest:
	cd $(SERVICES_DIR)/chat && ./mvnw test

file-storage-utest:
	cd $(SERVICES_DIR)/file-storage && ./mvnw test

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

all-ittest: $(addsuffix -ittest,$(SERVICES))
	@echo "All integration tests completed!"
