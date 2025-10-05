#!/bin/bash

# AuctionFlow Unified Deployment Script
# This script handles all deployment scenarios for the AuctionFlow application

set -e  # Exit on error

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored messages
print_info() {
    echo -e "${BLUE}ℹ ${NC}$1"
}

# Resolve compose file (support new repo structure)
COMPOSE_FILE="docker-compose.yml"
if [ -f "infrastructure/docker/docker-compose.yml" ]; then
    COMPOSE_FILE="infrastructure/docker/docker-compose.yml"
fi

# Wrapper to call docker compose with the correct file
dc() {
  docker compose -f "$COMPOSE_FILE" "$@"
}

print_success() {
    echo -e "${GREEN}✓${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

print_error() {
    echo -e "${RED}✗${NC} $1"
}

print_header() {
    echo ""
    echo -e "${BLUE}=========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}=========================================${NC}"
    echo ""
}

# Function to show usage
show_usage() {
    cat << EOF
AuctionFlow Deployment Script

Usage: ./deploy.sh [OPTION]

Options:
  full          Full rebuild (frontend + backend + docker) - Use for major changes
  backend       Backend only rebuild - Use for Java code changes
  frontend      Frontend only rebuild - Use for UI changes
  restart       Restart containers without rebuild - Use for config changes
  quick         Quick backend update (no cache clear) - Fastest option
  test          Run integration tests only
  logs          Show application logs
  status        Show container status
  clean         Clean all build artifacts and stop containers
  help          Show this help message

Examples:
  ./deploy.sh full       # Complete rebuild and deploy
  ./deploy.sh backend    # Rebuild only backend
  ./deploy.sh quick      # Fast backend update
  ./deploy.sh logs       # View logs

EOF
}

# Function to check prerequisites
check_prerequisites() {
    print_info "Checking prerequisites..."
    
    if ! command -v docker &> /dev/null; then
        print_error "Docker is not installed"
        exit 1
    fi
    
    if ! command -v docker compose &> /dev/null; then
        print_error "Docker Compose is not installed"
        exit 1
    fi
    
    if ! command -v npm &> /dev/null; then
        print_warning "npm is not installed (required for frontend builds)"
    fi
    
    print_success "Prerequisites check passed"
}

# Function to build frontend
build_frontend() {
    print_header "Building Frontend"
    
    cd auctionflow-ui
    
    if [ ! -d "node_modules" ]; then
        print_info "Installing npm dependencies..."
        npm install
    fi
    
    print_info "Building frontend application..."
    npm run build
    
    cd ..
    
    print_info "Copying frontend to backend static resources..."
    rm -rf auction-api/src/main/resources/static/ui/*
    mkdir -p auction-api/src/main/resources/static/ui
    cp -r auctionflow-ui/client/dist/* auction-api/src/main/resources/static/ui/
    
    print_success "Frontend build completed"
}

# Function to build backend
build_backend() {
    print_header "Building Backend"
    
    print_info "Compiling Java application..."
    ./gradlew :auction-api:build -x test
    
    print_success "Backend build completed"
}

# Function to rebuild docker images
rebuild_docker() {
    print_header "Rebuilding Docker Images"
    
    print_info "Building auction-api image..."
    dc build --no-cache auction-api
    
    print_success "Docker images rebuilt"
}

# Function to start containers
start_containers() {
    print_header "Starting Docker Containers"
    
    print_info "Starting all services..."
    dc up -d
    
    print_info "Waiting for services to initialize (30 seconds)..."
    for i in {1..30}; do
        echo -n "."
        sleep 1
    done
    echo ""
    
    print_success "Containers started"
}

# Function to stop containers
stop_containers() {
    print_header "Stopping Docker Containers"
    
    print_info "Stopping all services..."
    dc down
    
    print_success "Containers stopped"
}

# Function to restart specific service
restart_service() {
    local service=$1
    print_info "Restarting $service..."
    dc restart $service
    print_success "$service restarted"
}

# Function to show logs
show_logs() {
    local service=${1:-auction-api}
    local lines=${2:-50}
    
    print_header "Application Logs - $service"
    dc logs $service --tail=$lines -f
}

# Function to show status
show_status() {
    print_header "Container Status"
    dc ps
    
    echo ""
    print_header "Service Health"
    
    # Check API health
    if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
        print_success "API is healthy"
    else
        print_error "API is not responding"
    fi
    
    # Check UI
    if curl -s http://localhost:8080/ui/ > /dev/null 2>&1; then
        print_success "UI is accessible"
    else
        print_error "UI is not accessible"
    fi
}

# Function to run tests
run_tests() {
    print_header "Running Integration Tests"
    
    if [ -f "test-api-integration.sh" ]; then
        chmod +x test-api-integration.sh
        ./test-api-integration.sh
    else
        print_warning "test-api-integration.sh not found, skipping tests"
    fi
}

# Function to clean everything
clean_all() {
    print_header "Cleaning Build Artifacts"
    
    print_info "Stopping containers..."
    dc down -v
    
    print_info "Cleaning Gradle build..."
    ./gradlew clean
    
    print_info "Cleaning frontend build..."
    rm -rf auctionflow-ui/client/dist
    rm -rf auctionflow-ui/client/node_modules
    
    print_info "Cleaning backend static resources..."
    rm -rf auction-api/src/main/resources/static/ui/*
    
    print_success "Clean completed"
}

# Function for full deployment
deploy_full() {
    print_header "Full Deployment"
    check_prerequisites
    stop_containers
    build_frontend
    build_backend
    rebuild_docker
    start_containers
    show_status
    
    print_header "Deployment Complete!"
    print_success "Frontend and Backend rebuilt successfully"
    print_success "Docker containers are running"
    echo ""
    echo "🌐 Access the application:"
    echo "   - UI: http://localhost:8080/ui/"
    echo "   - API: http://localhost:8080/api/v1"
    echo "   - Swagger: http://localhost:8080/swagger-ui.html"
    echo ""
}

# Function for backend only deployment
deploy_backend() {
    print_header "Backend Only Deployment"
    check_prerequisites
    
    print_info "Stopping auction-api..."
    dc stop auction-api
    
    build_backend
    rebuild_docker
    
    print_info "Starting auction-api..."
    dc up -d auction-api
    
    print_info "Waiting for service to start (20 seconds)..."
    for i in {1..20}; do
        echo -n "."
        sleep 1
    done
    echo ""
    
    print_success "Backend deployment completed"
    show_status
}

# Function for frontend only deployment
deploy_frontend() {
    print_header "Frontend Only Deployment"
    check_prerequisites
    
    build_frontend
    
    print_info "Stopping auction-api..."
    dc stop auction-api
    
    build_backend
    rebuild_docker
    
    print_info "Starting auction-api..."
    dc up -d auction-api
    
    print_info "Waiting for service to start (20 seconds)..."
    for i in {1..20}; do
        echo -n "."
        sleep 1
    done
    echo ""
    
    print_success "Frontend deployment completed"
    show_status
}

# Function for quick backend update
deploy_quick() {
    print_header "Quick Backend Update"
    
    print_info "Stopping auction-api..."
    docker compose stop auction-api
    
    print_info "Building backend..."
    ./gradlew :auction-api:build -x test
    
    print_info "Rebuilding Docker image..."
    dc build auction-api
    
    print_info "Starting auction-api..."
    docker compose up -d auction-api
    
    print_info "Waiting for service to start (20 seconds)..."
    for i in {1..20}; do
        echo -n "."
        sleep 1
    done
    echo ""
    
    print_header "Testing Endpoints"
    echo ""
    echo "1. Testing /api/v1/auctions:"
    curl -s -o /dev/null -w "HTTP Status: %{http_code}\n" http://localhost:8080/api/v1/auctions
    
    echo ""
    echo "2. Testing WebSocket endpoint:"
    curl -s -o /dev/null -w "HTTP Status: %{http_code}\n" http://localhost:8080/ws
    
    echo ""
    print_success "Quick update completed"
    echo ""
    echo "🌐 Open http://localhost:8080/ui/ and verify:"
    echo "   - Auctions load correctly"
    echo "   - WebSocket connects successfully"
    echo ""
}

# Function to restart containers
deploy_restart() {
    print_header "Restarting Containers"
    
    print_info "Restarting all services..."
    dc restart
    
    print_info "Waiting for services to start (20 seconds)..."
    for i in {1..20}; do
        echo -n "."
        sleep 1
    done
    echo ""
    
    print_success "Restart completed"
    show_status
}

# Main script logic
main() {
    local command=${1:-help}
    
    case $command in
        full)
            deploy_full
            ;;
        backend)
            deploy_backend
            ;;
        frontend)
            deploy_frontend
            ;;
        quick)
            deploy_quick
            ;;
        restart)
            deploy_restart
            ;;
        test)
            run_tests
            ;;
        logs)
            show_logs ${2:-auction-api} ${3:-50}
            ;;
        status)
            show_status
            ;;
        clean)
            clean_all
            ;;
        help|--help|-h)
            show_usage
            ;;
        *)
            print_error "Unknown command: $command"
            echo ""
            show_usage
            exit 1
            ;;
    esac
}

# Run main function
main "$@"
