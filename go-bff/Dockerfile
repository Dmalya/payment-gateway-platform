# ==========================================
# Stage 1: Build
# ==========================================
FROM golang:1.26-alpine AS builder

# Set the working directory inside the container
WORKDIR /app

# Install git and ca-certificates (required for fetching dependencies and SSL)
RUN apk update && apk add --no-cache git ca-certificates tzdata

# Copy go mod and sum files first to leverage Docker layer caching
COPY go.mod go.sum ./
RUN go mod download

# Copy the rest of the source code
COPY . .

# Build the Go app
# CGO_ENABLED=0 ensures a fully static binary
# -ldflags="-w -s" strips debugging information to reduce binary size
RUN CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build -ldflags="-w -s" -o go-bff main.go

# ==========================================
# Stage 2: Final Runtime
# ==========================================
# Use distroless for a highly secure, minimal attack surface runtime
FROM gcr.io/distroless/static:nonroot

# Import the time zone data and CA certificates from the builder
COPY --from=builder /usr/share/zoneinfo /usr/share/zoneinfo
COPY --from=builder /etc/ssl/certs/ca-certificates.crt /etc/ssl/certs/

# Copy the compiled binary from the builder stage
COPY --from=builder /app/go-bff /go-bff

# Use the non-root user provided by the distroless image
USER nonroot:nonroot

# Expose the application port
EXPOSE 8080

# Command to run the executable
ENTRYPOINT ["/go-bff"]