#!/bin/bash

# Seed data script for Order Platform
# This script populates the system with test orders

set -e

# Configuration
API_URL="${API_URL:-http://localhost:8080}"
KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:8080}"
REALM="order-platform"
CLIENT_ID="api-client"
USERNAME="${KEYCLOAK_USERNAME:-admin@test.com}"
PASSWORD="${KEYCLOAK_PASSWORD:-test123}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

# Function to get JWT token from Keycloak
get_token() {
    print_info "Authenticating with Keycloak..."
    
    local token_response=$(curl -s -X POST "$KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token" \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -d "grant_type=password" \
        -d "client_id=$CLIENT_ID" \
        -d "username=$USERNAME" \
        -d "password=$PASSWORD")
    
    if [ $? -ne 0 ]; then
        print_error "Failed to connect to Keycloak"
        exit 1
    fi
    
    local token=$(echo "$token_response" | grep -o '"access_token":"[^"]*"' | cut -d'"' -f4)
    
    if [ -z "$token" ]; then
        print_error "Failed to obtain access token"
        print_error "Response: $token_response"
        exit 1
    fi
    
    print_info "Successfully authenticated"
    echo "$token"
}

# Function to create an order
create_order() {
    local customer_id=$1
    local items=$2
    local currency=$3
    local token=$4
    
    local response=$(curl -s -w "\n%{http_code}" -X POST "$API_URL/api/v1/orders" \
        -H "Authorization: Bearer $token" \
        -H "Content-Type: application/json" \
        -d "{
            \"customerId\": \"$customer_id\",
            \"items\": $items,
            \"currency\": \"$currency\"
        }")
    
    local http_code=$(echo "$response" | tail -n1)
    local body=$(echo "$response" | sed '$d')
    
    if [ "$http_code" -eq 201 ]; then
        local order_id=$(echo "$body" | grep -o '"orderId":"[^"]*"' | cut -d'"' -f4)
        echo "$order_id"
    else
        print_error "Failed to create order (HTTP $http_code)"
        echo ""
    fi
}

# Function to approve an order
approve_order() {
    local order_id=$1
    local approver_id=$2
    local token=$3
    
    local response=$(curl -s -w "\n%{http_code}" -X POST "$API_URL/api/v1/orders/$order_id/approve" \
        -H "Authorization: Bearer $token" \
        -H "Content-Type: application/json" \
        -d "{
            \"approvedBy\": \"$approver_id\",
            \"reason\": \"Automated seed data approval\"
        }")
    
    local http_code=$(echo "$response" | tail -n1)
    
    if [ "$http_code" -eq 200 ]; then
        return 0
    else
        print_warning "Failed to approve order $order_id (HTTP $http_code)"
        return 1
    fi
}

# Function to ship an order
ship_order() {
    local order_id=$1
    local tracking_number=$2
    local token=$3
    
    local response=$(curl -s -w "\n%{http_code}" -X POST "$API_URL/api/v1/orders/$order_id/ship" \
        -H "Authorization: Bearer $token" \
        -H "Content-Type: application/json" \
        -d "{
            \"trackingNumber\": \"$tracking_number\",
            \"carrier\": \"FedEx\"
        }")
    
    local http_code=$(echo "$response" | tail -n1)
    
    if [ "$http_code" -eq 200 ]; then
        return 0
    else
        print_warning "Failed to ship order $order_id (HTTP $http_code)"
        return 1
    fi
}

# Main seeding logic
main() {
    print_info "Starting Order Platform data seeding..."
    print_info "API URL: $API_URL"
    print_info "Keycloak URL: $KEYCLOAK_URL"
    echo ""
    
    # Get authentication token
    TOKEN=$(get_token)
    
    if [ -z "$TOKEN" ]; then
        print_error "Cannot proceed without authentication token"
        exit 1
    fi
    
    echo ""
    print_info "Creating test orders..."
    echo ""
    
    # Customer IDs
    CUSTOMER_1="123e4567-e89b-12d3-a456-426614174001"
    CUSTOMER_2="123e4567-e89b-12d3-a456-426614174002"
    CUSTOMER_3="123e4567-e89b-12d3-a456-426614174003"
    APPROVER="123e4567-e89b-12d3-a456-426614174999"
    
    # Counter for statistics
    CREATED=0
    APPROVED=0
    SHIPPED=0
    
    # Create orders with various statuses
    
    # Order 1: CREATED status (not approved)
    print_info "Creating order 1 (CREATED status)..."
    ORDER_1=$(create_order "$CUSTOMER_1" '[
        {"sku": "LAPTOP-001", "productName": "Dell XPS 15", "quantity": 1, "unitPrice": 1299.99}
    ]' "USD" "$TOKEN")
    if [ -n "$ORDER_1" ]; then
        print_info "✓ Order created: $ORDER_1"
        CREATED=$((CREATED + 1))
    fi
    
    # Order 2: APPROVED status
    print_info "Creating order 2 (APPROVED status)..."
    ORDER_2=$(create_order "$CUSTOMER_1" '[
        {"sku": "MOUSE-001", "productName": "Logitech MX Master 3", "quantity": 2, "unitPrice": 99.99},
        {"sku": "KEYBOARD-001", "productName": "Keychron K8", "quantity": 1, "unitPrice": 89.99}
    ]' "USD" "$TOKEN")
    if [ -n "$ORDER_2" ]; then
        print_info "✓ Order created: $ORDER_2"
        CREATED=$((CREATED + 1))
        sleep 1
        if approve_order "$ORDER_2" "$APPROVER" "$TOKEN"; then
            print_info "✓ Order approved: $ORDER_2"
            APPROVED=$((APPROVED + 1))
        fi
    fi
    
    # Order 3: SHIPPED status
    print_info "Creating order 3 (SHIPPED status)..."
    ORDER_3=$(create_order "$CUSTOMER_2" '[
        {"sku": "MONITOR-001", "productName": "LG UltraWide 34\"", "quantity": 1, "unitPrice": 599.99}
    ]' "USD" "$TOKEN")
    if [ -n "$ORDER_3" ]; then
        print_info "✓ Order created: $ORDER_3"
        CREATED=$((CREATED + 1))
        sleep 1
        if approve_order "$ORDER_3" "$APPROVER" "$TOKEN"; then
            print_info "✓ Order approved: $ORDER_3"
            APPROVED=$((APPROVED + 1))
            sleep 1
            if ship_order "$ORDER_3" "TRACK-$(date +%s)" "$TOKEN"; then
                print_info "✓ Order shipped: $ORDER_3"
                SHIPPED=$((SHIPPED + 1))
            fi
        fi
    fi
    
    # Order 4: Another CREATED order
    print_info "Creating order 4 (CREATED status)..."
    ORDER_4=$(create_order "$CUSTOMER_2" '[
        {"sku": "HEADSET-001", "productName": "Sony WH-1000XM5", "quantity": 1, "unitPrice": 399.99}
    ]' "USD" "$TOKEN")
    if [ -n "$ORDER_4" ]; then
        print_info "✓ Order created: $ORDER_4"
        CREATED=$((CREATED + 1))
    fi
    
    # Order 5: SHIPPED status
    print_info "Creating order 5 (SHIPPED status)..."
    ORDER_5=$(create_order "$CUSTOMER_3" '[
        {"sku": "TABLET-001", "productName": "iPad Pro 12.9\"", "quantity": 1, "unitPrice": 1099.99},
        {"sku": "PENCIL-001", "productName": "Apple Pencil 2", "quantity": 1, "unitPrice": 129.99}
    ]' "USD" "$TOKEN")
    if [ -n "$ORDER_5" ]; then
        print_info "✓ Order created: $ORDER_5"
        CREATED=$((CREATED + 1))
        sleep 1
        if approve_order "$ORDER_5" "$APPROVER" "$TOKEN"; then
            print_info "✓ Order approved: $ORDER_5"
            APPROVED=$((APPROVED + 1))
            sleep 1
            if ship_order "$ORDER_5" "TRACK-$(date +%s)" "$TOKEN"; then
                print_info "✓ Order shipped: $ORDER_5"
                SHIPPED=$((SHIPPED + 1))
            fi
        fi
    fi
    
    # Order 6: APPROVED status
    print_info "Creating order 6 (APPROVED status)..."
    ORDER_6=$(create_order "$CUSTOMER_3" '[
        {"sku": "PHONE-001", "productName": "iPhone 15 Pro", "quantity": 1, "unitPrice": 999.99}
    ]' "USD" "$TOKEN")
    if [ -n "$ORDER_6" ]; then
        print_info "✓ Order created: $ORDER_6"
        CREATED=$((CREATED + 1))
        sleep 1
        if approve_order "$ORDER_6" "$APPROVER" "$TOKEN"; then
            print_info "✓ Order approved: $ORDER_6"
            APPROVED=$((APPROVED + 1))
        fi
    fi
    
    echo ""
    print_info "========================================="
    print_info "Seeding complete!"
    print_info "========================================="
    print_info "Orders created:  $CREATED"
    print_info "Orders approved: $APPROVED"
    print_info "Orders shipped:  $SHIPPED"
    echo ""
    print_info "Wait a few seconds for the query service to process events,"
    print_info "then query orders at: $API_URL/api/v1/orders"
}

# Run main function
main
