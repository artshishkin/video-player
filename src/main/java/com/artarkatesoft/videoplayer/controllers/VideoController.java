package com.artarkatesoft.videoplayer.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.net.MalformedURLException;

@Controller
@RequestMapping("videos")
public class VideoController {

    @Value("${videoLocation}")
    private String videoLocation;

    @GetMapping("{name}/full")
    public ResponseEntity<FileSystemResource> getFullVideo(@PathVariable String name) throws MalformedURLException {
        FileSystemResource video = new FileSystemResource(videoLocation + "/" + name);
//        UrlResource video = new UrlResource("file:///" + videoLocation + "/" + name);
        return ResponseEntity
                .status(HttpStatus.PARTIAL_CONTENT)
                .contentType(MediaTypeFactory.getMediaType(video).orElse(MediaType.APPLICATION_OCTET_STREAM))
                .body(video);
    }

    @GetMapping
    public String videoIndex() {
        return "videos/index";
    }
}
