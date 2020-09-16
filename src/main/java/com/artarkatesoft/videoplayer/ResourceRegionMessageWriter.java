package com.artarkatesoft.videoplayer;

import lombok.val;
import org.reactivestreams.Publisher;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.ResourceRegionEncoder;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.*;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ResourceRegionMessageWriter implements HttpMessageWriter<ResourceRegion> {

    private static final ResolvableType REGION_TYPE = ResolvableType.forClass(ResourceRegion.class);

    private ResourceRegionEncoder regionEncoder = new ResourceRegionEncoder();

    private List<MediaType> mediaTypes = MediaType.asMediaTypes(regionEncoder.getEncodableMimeTypes());


    @Override
    public List<MediaType> getWritableMediaTypes() {
        return mediaTypes;
    }

    @Override
    public boolean canWrite(ResolvableType resolvableType, MediaType mediaType) {
        return regionEncoder.canEncode(resolvableType, mediaType);
    }

    @Override
    public Mono<Void> write(Publisher<? extends ResourceRegion> publisher,
                            ResolvableType resolvableType,
                            MediaType mediaType,
                            ReactiveHttpOutputMessage reactiveHttpOutputMessage,
                            Map<String, Object> map) {
        return null;
    }

    @Override
    public Mono<Void> write(Publisher<? extends ResourceRegion> inputStream,
                            ResolvableType actualType,
                            ResolvableType elementType,
                            MediaType mediaType,
                            ServerHttpRequest request,
                            ServerHttpResponse response,
                            Map<String, Object> hints) {
        HttpHeaders headers = response.getHeaders();
        headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");

        return Mono.from(inputStream).flatMap(
                resourceRegion -> {
                    response.setStatusCode(HttpStatus.PARTIAL_CONTENT);
                    MediaType resourceMediaType = getResourceMediaType(mediaType, resourceRegion.getResource());
                    headers.setContentType(resourceMediaType);

                    long contentLength = 0L;
                    try {
                        contentLength = resourceRegion.getResource().contentLength();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    val start = resourceRegion.getPosition();
                    val end = Math.min(start + resourceRegion.getCount() - 1, contentLength - 1);
                    headers.add("Content-Range", "bytes " + start + '-' + end + '/' + contentLength);
                    headers.setContentLength(end - start + 1);

                    return zeroCopy(resourceRegion.getResource(), resourceRegion, response)
                            .orElseGet(() -> {
                                val input = Mono.just(resourceRegion);
                                val body = this.regionEncoder.encode(input, response.bufferFactory(), REGION_TYPE, resourceMediaType, new HashMap<>());
                                response.writeWith(body);
                                return Mono.empty();
                            });
                }
        );

    }

    private MediaType getResourceMediaType(MediaType mediaType, Resource resource) {
        return (mediaType != null && mediaType.isConcrete() && mediaType != MediaType.APPLICATION_OCTET_STREAM) ?
                mediaType :
                MediaTypeFactory.getMediaType(resource)
                        .orElse(MediaType.APPLICATION_OCTET_STREAM);
    }

    private Optional<Mono<Void>> zeroCopy(Resource resource, ResourceRegion region,
                                          ReactiveHttpOutputMessage message) {
        if (message instanceof ZeroCopyHttpOutputMessage && resource.isFile()) {
            try {
                val file = resource.getFile();
                val pos = region.getPosition();
                val count = region.getCount();
                return Optional.of(((ZeroCopyHttpOutputMessage) message).writeWith(file, pos, count));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return Optional.empty();
    }
}
