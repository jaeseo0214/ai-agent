package org.example.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.example.service.HintService;

@RestController
@RequestMapping("/api/hints")
public class HintController {

    private final HintService hintService;

    public HintController(HintService hintService) {
        this.hintService = hintService;
    }

    @PostMapping
    public String getHint(@RequestBody HintRequest request) {
        return hintService.generateHint(request.getProblemText(), request.getStep());
    }
}

class HintRequest {
    private String problemText;
    private int step;

    public String getProblemText() {
        return problemText;
    }
    public void setProblemText(String problemText) {
        this.problemText = problemText;
    }

    public int getStep() {
        return step;
    }
    public void setStep(int step) {
        this.step = step;
    }
}