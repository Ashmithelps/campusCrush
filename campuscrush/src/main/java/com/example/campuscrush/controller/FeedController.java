package com.example.campuscrush.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.campuscrush.dto.CreatePublicConfessionRequest;
import com.example.campuscrush.dto.FeedItemResponse;
import com.example.campuscrush.dto.ReportRequest;
import com.example.campuscrush.entity.user.User;
import com.example.campuscrush.security.util.SecurityUtils;
import com.example.campuscrush.service.FeedService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/feed")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;

    @GetMapping
    public List<FeedItemResponse> getFeed(@RequestParam(required = false) Long cursor) {
        User viewer = SecurityUtils.currentUser();
        return feedService.getFeed(viewer, cursor);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FeedItemResponse post(@RequestBody CreatePublicConfessionRequest req) {
        User author = SecurityUtils.currentUser();
        return feedService.post(author, req.content());
    }

    @PostMapping("/{id}/view")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void recordView(@PathVariable Long id) {
        User viewer = SecurityUtils.currentUser();
        feedService.recordView(viewer, id);
    }

    @PostMapping("/{id}/report")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void report(@PathVariable Long id, @RequestBody ReportRequest req) {
        User reporter = SecurityUtils.currentUser();
        feedService.report(reporter, id, req.reason());
    }
}
