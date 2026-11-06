package main

import (
	"context"
	"crypto/ed25519"
	"encoding/base64"
	"encoding/hex"
	"encoding/json"
	"flag"
	"fmt"
	"io"
	"net"
	"os"
	"strconv"
	"strings"
	"time"

	soar "github.com/getlantern/soar"
)

func main() {
	configPath := flag.String("config", "", "JSON client settings file")
	zone := flag.String("zone", "", "delegated DNS zone")
	keyText := flag.String("key", "", "base64 or hex Ed25519 server key")
	resolversText := flag.String("resolvers", "", "comma-separated recursive resolvers")
	listen := flag.String("listen", "127.0.0.1:0", "local SOCKS5 address")
	flag.Parse()
	if *configPath != "" {
		data, err := os.ReadFile(*configPath)
		if err != nil {
			fmt.Println(err)
			return
		}
		var config struct {
			Zone      string   `json:"zone"`
			Key       string   `json:"serverKey"`
			Resolvers []string `json:"resolvers"`
			Listen    string   `json:"listen"`
		}
		if err := json.Unmarshal(data, &config); err != nil {
			fmt.Println(err)
			return
		}
		*zone, *keyText, *resolversText, *listen = config.Zone, config.Key, strings.Join(config.Resolvers, ","), config.Listen
	}
	key, err := decodeKey(*keyText)
	if err != nil || *zone == "" {
		fmt.Println("invalid Soar zone or server key")
		return
	}
	resolvers := make([]string, 0)
	for _, item := range strings.Split(*resolversText, ",") {
		item = strings.TrimSpace(item)
		if item != "" {
			if _, _, err := net.SplitHostPort(item); err != nil {
				item = net.JoinHostPort(item, "53")
			}
			resolvers = append(resolvers, item)
		}
	}
	if len(resolvers) == 0 {
		fmt.Println("at least one DNS resolver is required")
		return
	}
	client, err := soar.NewClient(soar.Config{Zone: *zone, ServerKey: ed25519.PublicKey(key), Resolvers: resolvers})
	if err != nil {
		fmt.Println(err)
		return
	}
	defer client.Close()
	listener, err := net.Listen("tcp", *listen)
	if err != nil {
		fmt.Println(err)
		return
	}
	defer listener.Close()
	fmt.Printf("READY %s\n", listener.Addr().String())
	for {
		conn, err := listener.Accept()
		if err != nil {
			return
		}
		go serve(conn, client)
	}
}

func decodeKey(value string) ([]byte, error) {
	value = strings.TrimSpace(value)
	if data, err := hex.DecodeString(value); err == nil && len(data) == ed25519.PublicKeySize {
		return data, nil
	}
	for _, encoding := range []*base64.Encoding{base64.RawStdEncoding, base64.StdEncoding, base64.RawURLEncoding, base64.URLEncoding} {
		if data, err := encoding.DecodeString(value); err == nil && len(data) == ed25519.PublicKeySize {
			return data, nil
		}
	}
	return nil, fmt.Errorf("server key must encode 32 bytes")
}

func serve(local net.Conn, client *soar.Client) {
	defer local.Close()
	_ = local.SetDeadline(time.Now().Add(10 * time.Second))
	var greeting [2]byte
	if _, err := io.ReadFull(local, greeting[:]); err != nil || greeting[0] != 5 || greeting[1] == 0 {
		return
	}
	methods := make([]byte, int(greeting[1]))
	if _, err := io.ReadFull(local, methods); err != nil {
		return
	}
	if _, err := local.Write([]byte{5, 0}); err != nil {
		return
	}
	var req [4]byte
	if _, err := io.ReadFull(local, req[:]); err != nil || req[0] != 5 || req[1] != 1 {
		_, _ = local.Write([]byte{5, 7, 0, 1, 0, 0, 0, 0, 0, 0})
		return
	}
	var host string
	switch req[3] {
	case 1:
		buf := make([]byte, 4)
		if _, err := io.ReadFull(local, buf); err != nil {
			return
		}
		host = net.IP(buf).String()
	case 3:
		var size [1]byte
		if _, err := io.ReadFull(local, size[:]); err != nil {
			return
		}
		buf := make([]byte, int(size[0]))
		if _, err := io.ReadFull(local, buf); err != nil {
			return
		}
		host = string(buf)
	case 4:
		buf := make([]byte, 16)
		if _, err := io.ReadFull(local, buf); err != nil {
			return
		}
		host = net.IP(buf).String()
	default:
		_, _ = local.Write([]byte{5, 8, 0, 1, 0, 0, 0, 0, 0, 0})
		return
	}
	var portBytes [2]byte
	if _, err := io.ReadFull(local, portBytes[:]); err != nil {
		return
	}
	port := strconv.Itoa(int(portBytes[0])<<8 | int(portBytes[1]))
	upstream, err := client.DialContext(context.Background(), "tcp", net.JoinHostPort(host, port))
	if err != nil {
		_, _ = local.Write([]byte{5, 5, 0, 1, 0, 0, 0, 0, 0, 0})
		return
	}
	defer upstream.Close()
	_ = local.SetDeadline(time.Time{})
	if _, err := local.Write([]byte{5, 0, 0, 1, 0, 0, 0, 0, 0, 0}); err != nil {
		return
	}
	done := make(chan struct{}, 2)
	go func() { _, _ = io.Copy(upstream, local); done <- struct{}{} }()
	go func() { _, _ = io.Copy(local, upstream); done <- struct{}{} }()
	<-done
}
