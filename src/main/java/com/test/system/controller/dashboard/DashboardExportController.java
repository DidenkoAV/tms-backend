package com.test.system.controller.dashboard;

import com.test.system.dto.dashboard.DashboardExportRequest;
import com.test.system.service.dashboard.DashboardPdfService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Dashboard Export", description = "Export dashboard reports to PDF")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/dashboard")
public class DashboardExportController {

    private final DashboardPdfService dashboardPdfService;

    @Operation(
        summary = "Export dashboard to PDF",
        description = "Generate a PDF report of the dashboard with all charts and statistics"
    )
    @PostMapping("/export-pdf")
    public ResponseEntity<Resource> exportDashboardToPdf(
            @Valid @RequestBody DashboardExportRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        
        log.info("Generating PDF dashboard export for user: {}", principal.getUsername());
        
        try {
            // Generate PDF
            byte[] pdfBytes = dashboardPdfService.generateDashboardPdf(request);
            
            // Create filename with current date
            String filename = String.format(
                "Messagepoint-TMS-Dashboard-%s.pdf",
                LocalDate.now().format(DateTimeFormatter.ISO_DATE)
            );
            
            // Prepare response
            ByteArrayResource resource = new ByteArrayResource(pdfBytes);
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(pdfBytes.length)
                    .body(resource);
                    
        } catch (Exception e) {
            log.error("Error generating PDF dashboard", e);
            throw new RuntimeException("Failed to generate PDF dashboard: " + e.getMessage(), e);
        }
    }
}

