package main

import (
	"bytes"
	"encoding/json"
	"io"
	"log/slog"
	"net"
	"net/http"
	"os"
	"time"
	"sync"
	"fmt"
	"strings"

	"github.com/google/uuid"
)

// PaymentRequest represents the incoming JSON payload.
type PaymentRequest struct {
	UserID     string  `json:"userId"`
	Amount     float64 `json:"amount"`
	Currency   string  `json:"currency"`
	MerchantID string  `json:"merchantId"`
}

// Config holds environment-driven configuration.
type Config struct {
	Port          string
	GatewayURL    string
	ClientTimeout time.Duration
	LogstashHost  string
	LogstashPort  string
}

// resilientWriter writes to a TCP connection but falls back to a secondary
// writer (Stdout) if the socket drops during runtime.
type resilientWriter struct {
	mu       sync.Mutex
	tcpConn  net.Conn
	fallback io.Writer
}

func (w *resilientWriter) Write(p []byte) (n int, err error) {
	w.mu.Lock()
        defer w.mu.Unlock()

        if w.tcpConn != nil {
           n, err = w.tcpConn.Write(p)
           if err == nil {
              return n, nil
           }
           // Connection broke during runtime. Close it and fall back to stdout
           w.tcpConn.Close()
           w.tcpConn = nil
        }
        // Write to os.Stdout if Logstash was never available or just dropped
        return w.fallback.Write(p)
}

func main() {
	cfg := Config{
		Port:          getEnv("PORT", "8080"),
		GatewayURL:    getEnv("GATEWAY_URL", "http://app-spring-gateway:8081/ingest"),
		LogstashHost:  getEnv("LOGSTASH_HOST", ""),
		LogstashPort:  getEnv("LOGSTASH_PORT", ""),
		ClientTimeout: 5 * time.Second,
	}

	// Initialize the Logstash/Stdout resilient logger
	setupLogger(cfg.LogstashHost, cfg.LogstashPort)

	// Reusable HTTP client with a strict timeout
	httpClient := &http.Client{
		Timeout: cfg.ClientTimeout,
	}

	mux := http.NewServeMux()
	mux.HandleFunc("POST /api/v1/payments", handlePayment(httpClient, cfg.GatewayURL))

	server := &http.Server{
		Addr:         ":" + cfg.Port,
		Handler:      mux,
		ReadTimeout:  5 * time.Second,
		WriteTimeout: 10 * time.Second,
		IdleTimeout:  120 * time.Second,
	}

	slog.Info("Starting BFF Service", "port", cfg.Port, "gateway", cfg.GatewayURL)
	if err := server.ListenAndServe(); err != nil && err != http.ErrServerClosed {
		slog.Error("Server failed", "error", err)
		os.Exit(1)
	}
}

// setupLogger configures structured JSON logging with a TCP Logstash target and stdout fallback.
func setupLogger(host, port string) {
	var logWriter io.Writer = os.Stdout

	if host != "" && port != "" {
		address := net.JoinHostPort(host, port)
		// Use a short timeout so the BFF doesn't hang indefinitely on boot if Logstash is down
		conn, err := net.DialTimeout("tcp", address, 2*time.Second)
		if err != nil {
			// Boot-time fallback: just use stdout
			logger := slog.New(slog.NewJSONHandler(logWriter, nil))
			slog.SetDefault(logger)
			slog.Warn("Logstash unavailable on boot, falling back to os.Stdout", "address", address, "error", err.Error())
			return
		}

		// Wrap the successful connection in our resilient writer
		logWriter = &resilientWriter{
			tcpConn:  conn,
			fallback: os.Stdout,
		}

		logger := slog.New(slog.NewJSONHandler(logWriter, nil))
		slog.SetDefault(logger)
		slog.Info("Successfully connected to Logstash TCP", "address", address)
		return
	}

	// Default fallback if no Logstash env vars were provided
	logger := slog.New(slog.NewJSONHandler(logWriter, nil))
	slog.SetDefault(logger)
}

func handlePayment(client *http.Client, targetURL string) http.HandlerFunc {
    return func(w http.ResponseWriter, r *http.Request) {
       var payload PaymentRequest

       decoder := json.NewDecoder(r.Body)
       decoder.DisallowUnknownFields()
       if err := decoder.Decode(&payload); err != nil {
          respondWithError(w, http.StatusBadRequest, "Invalid request payload")
          return
       }

       if payload.Amount <= 0 {
          respondWithError(w, http.StatusBadRequest, "Amount must be greater than 0")
          return
       }
       if payload.UserID == "" || payload.Currency == "" || payload.MerchantID == "" {
          respondWithError(w, http.StatusBadRequest, "Missing required fields")
          return
       }

       // 1. Generate a standard W3C traceparent: 00-{32 hex trace ID}-{16 hex span ID}-01
       rawUUID := strings.ReplaceAll(uuid.New().String(), "-", "")
       traceID := rawUUID                 // 32 chars
       spanID := rawUUID[:16]             // 16 chars
       traceparent := fmt.Sprintf("00-%s-%s-01", traceID, spanID)

       log := slog.With("traceId", traceID, "userId", payload.UserID)

       reqBytes, err := json.Marshal(payload)
       if err != nil {
          log.Error("Failed to marshal forwarded payload", "error", err.Error())
          respondWithError(w, http.StatusInternalServerError, "Internal server error")
          return
       }

       outboundReq, err := http.NewRequestWithContext(r.Context(), http.MethodPost, targetURL, bytes.NewReader(reqBytes))
       if err != nil {
          log.Error("Failed to create outbound request", "error", err.Error())
          respondWithError(w, http.StatusInternalServerError, "Internal server error")
          return
       }

       // 2. Set W3C header instead of custom X-Trace-Id
       outboundReq.Header.Set("Content-Type", "application/json")
       outboundReq.Header.Set("traceparent", traceparent)

       resp, err := client.Do(outboundReq)
       if err != nil {
          log.Error("Failed to forward request to gateway", "error", err.Error())
          respondWithError(w, http.StatusBadGateway, "Gateway unavailable")
          return
       }
       defer resp.Body.Close()

       // 3. Return W3C header to the client
       w.Header().Set("Content-Type", "application/json")
       w.Header().Set("traceparent", traceparent)
       w.WriteHeader(resp.StatusCode)

       if resp.StatusCode >= 200 && resp.StatusCode <= 299 {
                 io.Copy(io.Discard, resp.Body) // Drain the connection
                 json.NewEncoder(w).Encode(map[string]string{
                    "status":  "accepted",
                    "message": "Payment event ingested successfully",
                    "traceId": traceID,
                 })
              } else {
                 // Proxy downstream error payloads (e.g., Spring validation errors)
                 if _, err := io.Copy(w, resp.Body); err != nil {
                    log.Error("Failed to write error response body", "error", err.Error())
                 }
              }

       log.Info("Payment request processed", "status", resp.StatusCode)
    }
}

func respondWithError(w http.ResponseWriter, code int, message string) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(code)
	json.NewEncoder(w).Encode(map[string]string{"error": message})
}

func getEnv(key, fallback string) string {
	if value, exists := os.LookupEnv(key); exists {
		return value
	}
	return fallback
}
