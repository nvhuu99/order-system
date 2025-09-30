package main

import (
	"context"
	"encoding/json"
	"log"

	"github.com/segmentio/kafka-go"
)

type Entry struct {
	ProductID     string `json:"productId"`
	ProductName   string `json:"productName"`
	QtyAdjustment int    `json:"qtyAdjustment"`
	Action        string `json:"action"`
}

type CartUpdateRequest struct {
	UserID        string  `json:"userId"`
	VersionNumber int     `json:"versionNumber"`
	Entries       []Entry `json:"entries"`
}

func main() {
	broker := "kafka:9092"
	topic := "cart-update-request"

	writer := kafka.NewWriter(kafka.WriterConfig{
		Brokers:  []string{broker},
		Topic:    topic,
		Balancer: &kafka.Hash{},
	})
	defer writer.Close()

	entries := []Entry{
		{
			ProductID:     "P001",
			ProductName:   "Laptop",
			QtyAdjustment: 1,
			Action:        "QTY_CHANGE",
		},
	}

	requests := []CartUpdateRequest{
		{
			UserID:        "user_1",
			VersionNumber: 1,
			Entries:       entries,
		},
		{
			UserID:        "user_1",
			VersionNumber: 2,
			Entries:       entries,
		},

		{
			UserID:        "user_2",
			VersionNumber: 1,
			Entries:       entries,
		},
		{
			UserID:        "user_2",
			VersionNumber: 2,
			Entries:       entries,
		},

		{
			UserID:        "user_3",
			VersionNumber: 1,
			Entries:       entries,
		},
		{
			UserID:        "user_3",
			VersionNumber: 2,
			Entries:       entries,
		},

		{
			UserID:        "user_4",
			VersionNumber: 1,
			Entries:       entries,
		},
		{
			UserID:        "user_4",
			VersionNumber: 2,
			Entries:       entries,
		},
	}
	messages := []kafka.Message{}
	for _, request := range requests {
		// partition := 0
		// if request.UserID == "user_3" || request.UserID == "user_4" {
		// 	partition = 1
		// }
		value, err := json.Marshal(request)
		if err != nil {
			log.Fatalf("Failed to marshal message: %v", err)
		}
		messages = append(messages, kafka.Message{
			// Partition: partition,
			Key:       []byte(request.UserID),
			Value:     value,
			Headers: []kafka.Header{
				{
					Key:   "__TypeId__",
					Value: []byte("com.example.cart.services.cart_service.entities.CartUpdateRequest"),
				},
			},
		})
	}

	err := writer.WriteMessages(context.Background(), messages...)
	if err != nil {
		log.Fatalf("Failed to write message: %v", err)
	}

	log.Println("All messages sent successfully!")
}
