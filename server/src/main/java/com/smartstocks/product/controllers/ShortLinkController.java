package com.smartstocks.product.controllers;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.smartstocks.product.dto.ShortLinkDto;
import com.smartstocks.product.service.IShortLinkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

@RestController
@RequestMapping("/api/links")
@CrossOrigin(origins = "*")
public class ShortLinkController {

    @Autowired
    private IShortLinkService shortLinkService;

    @Value("${app.short-link.base-url}")
    private String shortLinkBaseUrl;

    @PostMapping("/shorten")
    public ResponseEntity<String> shortenLink(@RequestParam String originalUrl) {
        try {
            URI uri = new URI(originalUrl);
            if (uri.getScheme() == null || uri.getHost() == null) {
                return ResponseEntity.badRequest().body("Invalid URL format");
            }
        } catch (URISyntaxException e) {
            return ResponseEntity.badRequest().body("Invalid URL format");
        }

        String shortenedUrl = shortLinkService.shortenLink(originalUrl, null);
        return ResponseEntity.ok(shortenedUrl);
    }

    @GetMapping("/{shortId}/stats")
    public ResponseEntity<ShortLinkDto> getLinkStats(@PathVariable String shortId) {
        if (shortId == null || shortId.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        ShortLinkDto linkDto = shortLinkService.getLinkStatistics(shortId);
        if (linkDto == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(linkDto);
    }

    @DeleteMapping("/{shortId}")
    public ResponseEntity<String> deleteLink(@PathVariable String shortId) {
        if (shortId == null || shortId.isEmpty()) {
            return ResponseEntity.badRequest().body("Invalid shortId");
        }
        if (!shortLinkService.deleteLink(shortId)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok("Link deleted successfully");
    }

    @GetMapping
    public ResponseEntity<List<ShortLinkDto>> getAllLinks() {
        return ResponseEntity.ok(shortLinkService.getAllLinks());
    }

    @GetMapping("/qr/{shortId}")
    public ResponseEntity<byte[]> generateQr(@PathVariable String shortId) throws WriterException, IOException {
        if (shortLinkService.getLinkByShortId(shortId).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        String base = shortLinkBaseUrl.endsWith("/") ? shortLinkBaseUrl : shortLinkBaseUrl + "/";
        String shortUrl = base + shortId;

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix matrix = qrCodeWriter.encode(shortUrl, BarcodeFormat.QR_CODE, 200, 200);
        MatrixToImageWriter.writeToStream(matrix, "PNG", stream);

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(stream.toByteArray());
    }
}
