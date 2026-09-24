package com.desenvolvimento.logica.cashpilot_api.status;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/status")
public class StatusController {

    @GetMapping
    public Map<String, String> status() {
        return Map.of(
                "application", "CashPilot",
                "status", "UP"
        );
    }
}
