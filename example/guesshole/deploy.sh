#!/bin/bash

# Exit on any error
set -e

# Function to help setup SSH keys
setup_ssh_keys() {
    echo "To avoid password prompts, you need to set up SSH key authentication."
    echo "This script will guide you through the process:"

    # Check if SSH key exists
    if [ ! -f "$HOME/.ssh/id_rsa" ]; then
        echo "Generating a new SSH key pair..."
        ssh-keygen -t rsa -b 4096 -f "$HOME/.ssh/id_rsa" -N ""
    else
        echo "SSH key already exists at $HOME/.ssh/id_rsa"
    fi

    # Copy the key to the remote server
    echo "Copying your SSH public key to the remote server..."
    echo "You'll be prompted for your password one last time:"
    ssh-copy-id ${REMOTE_USER}@${REMOTE_HOST}

    echo "SSH key setup complete! Future deployments shouldn't prompt for passwords."
    echo "To continue with deployment now, run this script again."
    exit 0
}

# Function to purge Cloudflare cache
purge_cloudflare_cache() {
    echo "🧹 Purging Cloudflare cache..."

    # Cloudflare configuration
    CF_ZONE_ID="a6bea7c7fba015c3af9f6cb3f0ebc73a"
    CF_AUTH_EMAIL=${CF_AUTH_EMAIL:-""}  # Set via environment variable
    CF_AUTH_KEY=${CF_AUTH_KEY:-""}      # Set via environment variable

    # Check if auth credentials are set
    if [ -z "$CF_AUTH_EMAIL" ] || [ -z "$CF_AUTH_KEY" ]; then
        echo "⚠️  Cloudflare credentials not found. Cache purge skipped."
        echo "   Set CF_AUTH_EMAIL and CF_AUTH_KEY environment variables to enable cache purging."
        return 1
    fi

    # Perform API request to purge cache
    CF_RESULT=$(curl -s -X POST "https://api.cloudflare.com/client/v4/zones/${CF_ZONE_ID}/purge_cache" \
        -H "X-Auth-Email: ${CF_AUTH_EMAIL}" \
        -H "X-Auth-Key: ${CF_AUTH_KEY}" \
        -H "Content-Type: application/json" \
        --data '{"purge_everything":true}')

    # Check if the API request was successful
    if echo "$CF_RESULT" | grep -q '"success"\s*:\s*true'; then
        echo "✅ Cloudflare cache purged successfully!"
    else
        echo "❌ Failed to purge Cloudflare cache:"
        echo "$CF_RESULT" | grep -o '"message":"[^"]*"' || echo "$CF_RESULT"
    fi
}

# Configuration
REMOTE_USER="mdoms"
REMOTE_HOST="192.168.0.10"  # Set to IP of your remote server if not deploying locally
REMOTE_DIR="/home/mdoms/guesshole"  # Where to deploy on the remote server
SSH_KEY_PATH="$HOME/.ssh/id_rsa"  # Path to your SSH private key
SSH_OPTIONS="-i $SSH_KEY_PATH -o StrictHostKeyChecking=no -o BatchMode=yes"
APP_NAME="guesshole"

# Determine if we're deploying locally or to a remote server
if [ "$1" == "--setup-ssh" ]; then
    setup_ssh_keys
elif [ "$1" == "--remote" ]; then
    DEPLOY_REMOTE=true
    echo "🚀 Preparing for remote deployment to ${REMOTE_HOST}..."

    # Test SSH connectivity with keys
    if ! ssh $SSH_OPTIONS -q ${REMOTE_USER}@${REMOTE_HOST} exit 2>/dev/null; then
        echo "❌ SSH key authentication failed. Run the script with --setup-ssh to configure passwordless login."
        echo "   Command: ./deploy.sh --setup-ssh"
        exit 1
    fi
else
    DEPLOY_REMOTE=false
    echo "🚀 Preparing for local deployment..."
fi

# Create necessary directories
mkdir -p nginx/conf
mkdir -p nginx/ssl

# Copy nginx configuration if it doesn't exist yet
if [ ! -f nginx/conf/default.conf ]; then
    echo "Creating default nginx configuration..."
    cat > nginx/conf/default.conf << EOF
server {
    listen 80;
    server_name yourdomain.com;

    location / {
        proxy_pass http://app:8088;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;

        # WebSocket support
        proxy_http_version 1.1;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_read_timeout 86400; # Increased timeout for long-lived connections
    }
}
EOF
fi

if [ "$DEPLOY_REMOTE" = true ]; then
    # Package everything needed for deployment
    echo "📦 Creating deployment package..."

    # Create a list of files to package, checking if they exist first
    FILES_TO_PACKAGE="docker-compose.yml Dockerfile nginx bin"

    # Add core project files only if they exist
    for file in src frontend gradle gradlew build.gradle settings.gradle gradle.properties config; do
        if [ -e "$file" ]; then
            FILES_TO_PACKAGE="$FILES_TO_PACKAGE $file"
        fi
    done

    tar -czf ${APP_NAME}.tar.gz $FILES_TO_PACKAGE

    # Create remote directory if it doesn't exist
    ssh $SSH_OPTIONS ${REMOTE_USER}@${REMOTE_HOST} "mkdir -p ${REMOTE_DIR}"

    # Copy files to remote server
    echo "📤 Copying files to remote server..."
    ssh $SSH_OPTIONS ${REMOTE_USER}@${REMOTE_HOST} "cd ${REMOTE_DIR} && \
      rm -rf ./src"

    scp $SSH_OPTIONS ${APP_NAME}.tar.gz ${REMOTE_USER}@${REMOTE_HOST}:${REMOTE_DIR}

    # Extract and deploy on remote server
    echo "🔧 Deploying on remote server..."
    ssh $SSH_OPTIONS ${REMOTE_USER}@${REMOTE_HOST} "cd ${REMOTE_DIR} && \
        tar -xzf ${APP_NAME}.tar.gz && \
        docker-compose down || true && \
        docker-compose up -d --build"

    # Clean up local tar file
    rm ${APP_NAME}.tar.gz

    echo "✅ Deployment to remote server completed!"
else
    # Deploy locally
    echo "🔧 Deploying locally..."
    docker-compose down || true
    docker-compose up -d --build

    echo "✅ Local deployment completed!"
fi

# Purge Cloudflare cache
purge_cloudflare_cache

# Display container status
if [ "$DEPLOY_REMOTE" = true ]; then
    echo "📊 Container status on remote server:"
    ssh $SSH_OPTIONS ${REMOTE_USER}@${REMOTE_HOST} "cd ${REMOTE_DIR} && docker-compose ps"
else
    echo "📊 Container status:"
    docker-compose ps
fi

echo "🌐 Your Guesshole application should now be running behind Nginx!"
echo "📝 To set up Cloudflare Tunnel, follow the instructions at https://developers.cloudflare.com/cloudflare-one/connections/connect-apps/"