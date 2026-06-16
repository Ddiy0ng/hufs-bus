package hufsbus.spring.domain.term.service;

import hufsbus.spring.domain.term.dto.PrivacyTermResponseDto;
import hufsbus.spring.domain.term.dto.ServiceTermResponseDto;
import hufsbus.spring.domain.term.entity.PrivacyTerm;
import hufsbus.spring.domain.term.entity.ServiceTerm;
import hufsbus.spring.domain.term.repository.PrivacyTermRepository;
import hufsbus.spring.domain.term.repository.ServiceTermRepository;
import hufsbus.spring.global.exception.CustomException;
import hufsbus.spring.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class TermService {

    private final PrivacyTermRepository privacyTermRepository;
    private final ServiceTermRepository serviceTermRepository;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Transactional
    public PrivacyTermResponseDto savePrivacyTermPdf(MultipartFile file) {
        validatePdf(file);

        Path savedPath = saveFileToDisk(file);
        String originalFileName = getOriginalFileName(file);

        PrivacyTerm privacyTerm = PrivacyTerm.of(
                originalFileName,
                savedPath.toString(),
                file.getContentType()
        );

        PrivacyTerm saved = privacyTermRepository.save(privacyTerm);
        return PrivacyTermResponseDto.of(saved);
    }

    @Transactional
    public ServiceTermResponseDto saveServiceTermPdf(MultipartFile file) {
        validatePdf(file);

        Path savedPath = saveFileToDisk(file);
        String originalFileName = getOriginalFileName(file);

        ServiceTerm serviceTerm = ServiceTerm.of(
                originalFileName,
                savedPath.toString(),
                file.getContentType()
        );

        ServiceTerm saved = serviceTermRepository.save(serviceTerm);
        return ServiceTermResponseDto.of(saved);
    }

    @Transactional(readOnly = true)
    public PrivacyTermResponseDto findPrivacyTerm() {
        PrivacyTerm privacyTerm = privacyTermRepository.findTopByOrderByIdDesc()
                .orElseThrow(() -> new CustomException(ErrorCode.PRIVACY_TERM_NOT_FOUND));

        return PrivacyTermResponseDto.of(privacyTerm);
    }

    @Transactional(readOnly = true)
    public ServiceTermResponseDto findServiceTerm() {
        ServiceTerm serviceTerm = serviceTermRepository.findTopByOrderByIdDesc()
                .orElseThrow(() -> new CustomException(ErrorCode.SERVICE_TERM_NOT_FOUND));

        return ServiceTermResponseDto.of(serviceTerm);
    }

    @Transactional(readOnly = true)
    public Resource getPrivacyTermPdf() {
        PrivacyTerm privacyTerm = privacyTermRepository.findTopByOrderByIdDesc()
                .orElseThrow(() -> new CustomException(ErrorCode.PRIVACY_TERM_NOT_FOUND));

        return getResource(privacyTerm.getFilePath());
    }

    @Transactional(readOnly = true)
    public Resource getServiceTermPdf() {
        ServiceTerm serviceTerm = serviceTermRepository.findTopByOrderByIdDesc()
                .orElseThrow(() -> new CustomException(ErrorCode.SERVICE_TERM_NOT_FOUND));

        return getResource(serviceTerm.getFilePath());
    }

    private Path saveFileToDisk(MultipartFile file) {
        try {
            Path uploadPath = Paths.get(uploadDir)
                    .toAbsolutePath()
                    .normalize();

            Files.createDirectories(uploadPath);

            String storedFileName = UUID.randomUUID() + ".pdf";
            Path targetPath = uploadPath.resolve(storedFileName).normalize();

            if (!targetPath.startsWith(uploadPath)) {
                throw new CustomException(ErrorCode.WRONG_FILE_PATH);
            }

            Files.copy(
                    file.getInputStream(),
                    targetPath,
                    StandardCopyOption.REPLACE_EXISTING
            );

            return targetPath;

        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.EXCEPTION_WHILE_FILE_SAVE);
        }
    }

    private Resource getResource(String filePath) {
        try {
            Path path = Paths.get(filePath)
                    .toAbsolutePath()
                    .normalize();

            Resource resource = new UrlResource(path.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                throw new CustomException(ErrorCode.CANT_READ_FILE_EXCEPTION);
            }

            return resource;

        } catch (MalformedURLException e) {
            throw new CustomException(ErrorCode.CANT_READ_FILE_EXCEPTION);
        }
    }

    private String getOriginalFileName(MultipartFile file) {
        return StringUtils.cleanPath(
                Objects.requireNonNull(file.getOriginalFilename())
        );
    }

    private void validatePdf(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(ErrorCode.EMPTY_FILE);
        }

        String originalFileName = file.getOriginalFilename();

        if (originalFileName == null || !originalFileName.toLowerCase().endsWith(".pdf")) {
            throw new CustomException(ErrorCode.NOT_PDF_FILE);
        }

        String contentType = file.getContentType();

        if (contentType != null && !contentType.equals("application/pdf")) {
            throw new CustomException(ErrorCode.NOT_PDF_FILE);
        }
    }
}