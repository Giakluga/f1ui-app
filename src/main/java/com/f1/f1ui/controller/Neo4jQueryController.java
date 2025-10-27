package com.f1.f1ui.controller;

import org.neo4j.driver.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/neo4j")
@CrossOrigin(origins = "*")
public class Neo4jQueryController {

    private Driver driver;

    @PostMapping("/query")
    public ResponseEntity<?> executeQuery(@RequestBody QueryRequest request) {
        if (driver == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Non connesso a Neo4j. Configura prima la connessione."));
        }

        try (Session session = driver.session()) {
            Result result = session.run(request.getQuery());

            List<String> columns = result.keys();
            List<Map<String, Object>> data = new ArrayList<>();

            while (result.hasNext()) {
                org.neo4j.driver.Record record = result.next();
                Map<String, Object> row = new HashMap<>();
                for (String column : columns) {
                    row.put(column, convertValue(record.get(column)));
                }
                data.add(row);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("columns", columns);
            response.put("data", data);
            response.put("summary", Map.of(
                    "resultAvailableAfter", 0,
                    "resultConsumedAfter", 0
            ));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/connect")
    public ResponseEntity<?> testConnection(@RequestBody ConnectionConfig config) {
        try {
            // Chiudi la connessione precedente se esiste
            if (driver != null) {
                driver.close();
            }

            // Crea nuova connessione
            driver = GraphDatabase.driver(
                    config.getUri(),
                    AuthTokens.basic(config.getUsername(), config.getPassword())
            );

            driver.verifyConnectivity();
            return ResponseEntity.ok(Map.of("connected", true));

        } catch (Exception e) {
            driver = null;
            return ResponseEntity.ok(Map.of(
                    "connected", false,
                    "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/disconnect")
    public ResponseEntity<?> disconnect() {
        if (driver != null) {
            driver.close();
            driver = null;
        }
        return ResponseEntity.ok(Map.of("disconnected", true));
    }

    private Object convertValue(Value value) {
        if (value.isNull()) return null;

        switch (value.type().name()) {
            case "NODE":
                return Map.of(
                        "labels", value.asNode().labels(),
                        "properties", value.asNode().asMap()
                );
            case "RELATIONSHIP":
                return Map.of(
                        "type", value.asRelationship().type(),
                        "properties", value.asRelationship().asMap()
                );
            case "LIST":
                return value.asList(this::convertValue);
            case "MAP":
                return value.asMap(this::convertValue);
            default:
                return value.asObject();
        }
    }

    public static class QueryRequest {
        private String query;

        public String getQuery() {
            return query;
        }

        public void setQuery(String query) {
            this.query = query;
        }
    }

    public static class ConnectionConfig {
        private String uri;
        private String username;
        private String password;

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}