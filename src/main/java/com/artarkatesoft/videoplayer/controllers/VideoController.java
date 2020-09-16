package com.artarkatesoft.videoplayer.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@Controller
@RequestMapping("videos")
public class VideoController {

    @Value("${videoLocation}")
    private String videoLocation;

    public static final long ChunkSize = 1000000L;

    @GetMapping("{name}/full")
    @ResponseBody
    public ResponseEntity<UrlResource> getFullVideo(@PathVariable String name) throws IOException {
//        FileSystemResource video = new FileSystemResource(videoLocation + "/" + name);
        UrlResource video = new UrlResource("file:" + videoLocation + "/" + name);
        return ResponseEntity
                .ok()
//                .status(HttpStatus.PARTIAL_CONTENT)
                .contentType(MediaTypeFactory.getMediaType(video).orElse(MediaType.APPLICATION_OCTET_STREAM))
                .body(video);
    }

    @GetMapping("{name}")
    public ResponseEntity<ResourceRegion> getVideo(@PathVariable String name, @RequestHeader HttpHeaders headers) throws IOException {
        UrlResource video = new UrlResource("file:" + videoLocation + '/' + name);
        ResourceRegion region = resourceRegion(video, headers);
        return ResponseEntity
                .status(HttpStatus.PARTIAL_CONTENT)
                .contentType(MediaTypeFactory.getMediaType(video)
                        .orElse(MediaType.APPLICATION_OCTET_STREAM))
                .body(region);
    }


    private ResourceRegion resourceRegion(UrlResource video, HttpHeaders headers) throws IOException {
        long contentLength = video.contentLength();
        List<HttpRange> headersRange = headers.getRange();
        HttpRange range = headersRange.iterator().hasNext() ? headersRange.iterator().next() : null;
        long start;
        ResourceRegion resourceRegion;
        if (range != null) {
            start = range.getRangeStart(contentLength);
            long end = range.getRangeEnd(contentLength);
            long rangeLength = Long.min(1000000L, end - start + 1L);
            resourceRegion = new ResourceRegion(video, start, rangeLength);
        } else {
            start = Long.min(1000000L, contentLength);
            resourceRegion = new ResourceRegion(video, 0L, start);
        }
        return resourceRegion;
    }

    @GetMapping
    public String videoIndex() {
        return "videos/index";
    }
}
