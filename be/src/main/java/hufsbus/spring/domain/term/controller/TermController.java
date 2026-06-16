package hufsbus.spring.domain.term.controller;

import hufsbus.spring.domain.term.dto.PrivacyTermResponseDto;
import hufsbus.spring.domain.term.dto.ServiceTermResponseDto;
import hufsbus.spring.domain.term.service.TermService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/terms")
public class TermController {

    private final TermService termService;

    @PostMapping(
            value = "/privacy",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<PrivacyTermResponseDto> uploadPrivacyTerm(
            @RequestParam("file") MultipartFile file
    ) {
        PrivacyTermResponseDto response = termService.savePrivacyTermPdf(file);
        return ResponseEntity.ok(response);
    }

    @PostMapping(
            value = "/service",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ServiceTermResponseDto> uploadServiceTerm(
            @RequestParam("file") MultipartFile file
    ) {
        ServiceTermResponseDto response = termService.saveServiceTermPdf(file);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/privacy")
    public ResponseEntity<PrivacyTermResponseDto> findPrivacyTerm() {
        PrivacyTermResponseDto response = termService.findPrivacyTerm();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/service")
    public ResponseEntity<ServiceTermResponseDto> findServiceTerm() {
        ServiceTermResponseDto response = termService.findServiceTerm();
        return ResponseEntity.ok(response);
    }

    @GetMapping(
            value = "/privacy/view",
            produces = MediaType.APPLICATION_PDF_VALUE
    )
    public ResponseEntity<Resource> viewPrivacyTerm() {
        Resource resource = termService.getPrivacyTermPdf();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }

    @GetMapping(
            value = "/service/view",
            produces = MediaType.APPLICATION_PDF_VALUE
    )
    public ResponseEntity<Resource> viewServiceTerm() {
        Resource resource = termService.getServiceTermPdf();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }
}