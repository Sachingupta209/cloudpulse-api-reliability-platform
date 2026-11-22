package com.cloudpulse.report;

import com.cloudpulse.incident.Incident;
import com.cloudpulse.monitor.Monitor;
import com.cloudpulse.user.User;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.*;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PdfReportService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'")
            .withZone(ZoneOffset.UTC);

    private static final Color PRIMARY_COLOR = new Color(15, 23, 42);     // Slate 900
    private static final Color SECONDARY_COLOR = new Color(2, 132, 199);  // Sky 600
    private static final Color TABLE_HEADER_BG = new Color(30, 41, 59);   // Slate 800
    private static final Color ALT_ROW_BG = new Color(248, 250, 252);     // Slate 50
    private static final Color SUCCESS_COLOR = new Color(16, 185, 129);   // Emerald 500
    private static final Color DANGER_COLOR = new Color(239, 68, 68);     // Rose 500
    private static final Color TEXT_MUTED = new Color(100, 116, 139);     // Slate 500

    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, PRIMARY_COLOR);
    private static final Font SUBTITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10, TEXT_MUTED);
    private static final Font SECTION_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, PRIMARY_COLOR);
    private static final Font TH_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
    private static final Font TD_FONT = FontFactory.getFont(FontFactory.HELVETICA, 8, PRIMARY_COLOR);
    private static final Font TD_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, PRIMARY_COLOR);

    public byte[] generateSingleMonitorReport(Monitor monitor, Instant start, Instant end,
                                              MonitorStats stats, List<Incident> incidents) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 40, 40);
        PdfWriter writer = PdfWriter.getInstance(document, out);
        writer.setPageEvent(new PageNumberFooterEvent());

        document.open();

        addBrandedHeader(document, "Individual Monitor Reliability Report", start, end);
        addMonitorSummarySection(document, monitor, stats);
        addPerformanceMetricsSection(document, stats);
        addIncidentLogSection(document, incidents);

        document.close();
        return out.toByteArray();
    }

    public byte[] generateSummaryReport(User user, Instant start, Instant end,
                                        List<MonitorStats> monitorStats, List<Incident> incidents) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 40, 40);
        PdfWriter writer = PdfWriter.getInstance(document, out);
        writer.setPageEvent(new PageNumberFooterEvent());

        document.open();

        addBrandedHeader(document, "Portfolio Health & Summary Report", start, end);
        addPortfolioExecutiveSummary(document, user, monitorStats, incidents);
        addAllMonitorsTable(document, monitorStats);
        addIncidentLogSection(document, incidents);

        document.close();
        return out.toByteArray();
    }

    public byte[] generateIncidentReport(User user, Instant start, Instant end, List<Incident> incidents) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 40, 40);
        PdfWriter writer = PdfWriter.getInstance(document, out);
        writer.setPageEvent(new PageNumberFooterEvent());

        document.open();

        addBrandedHeader(document, "Incident & Downtime Audit Report", start, end);
        addIncidentLogSection(document, incidents);

        document.close();
        return out.toByteArray();
    }

    public byte[] generatePerformanceReport(User user, Instant start, Instant end, List<MonitorStats> monitorStats) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 40, 40);
        PdfWriter writer = PdfWriter.getInstance(document, out);
        writer.setPageEvent(new PageNumberFooterEvent());

        document.open();

        addBrandedHeader(document, "Latency & Uptime Performance SLA Report", start, end);
        addPerformanceSlaTable(document, monitorStats);

        document.close();
        return out.toByteArray();
    }

    // --- Layout Sections ---

    private void addBrandedHeader(Document doc, String reportTitle, Instant start, Instant end) throws DocumentException {
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{65f, 35f});
        headerTable.setSpacingAfter(15);

        // Left Cell: CloudPulse Brand & Title
        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);

        Paragraph brand = new Paragraph("CLOUDPULSE RELIABILITY PLATFORM", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, SECONDARY_COLOR));
        Paragraph title = new Paragraph(reportTitle, TITLE_FONT);
        leftCell.addElement(brand);
        leftCell.addElement(title);

        // Right Cell: Metadata
        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

        Paragraph genTime = new Paragraph("Generated: " + DATE_FORMATTER.format(Instant.now()), SUBTITLE_FONT);
        Paragraph period = new Paragraph("Period: " + DATE_FORMATTER.format(start) + "\n   to " + DATE_FORMATTER.format(end), SUBTITLE_FONT);
        genTime.setAlignment(Element.ALIGN_RIGHT);
        period.setAlignment(Element.ALIGN_RIGHT);
        rightCell.addElement(genTime);
        rightCell.addElement(period);

        headerTable.addCell(leftCell);
        headerTable.addCell(rightCell);
        doc.add(headerTable);

        // Horizontal divider rule
        PdfPTable line = new PdfPTable(1);
        line.setWidthPercentage(100);
        PdfPCell lineCell = new PdfPCell();
        lineCell.setBackgroundColor(SECONDARY_COLOR);
        lineCell.setFixedHeight(2f);
        lineCell.setBorder(Rectangle.NO_BORDER);
        line.addCell(lineCell);
        line.setSpacingAfter(15);
        doc.add(line);
    }

    private void addMonitorSummarySection(Document doc, Monitor monitor, MonitorStats stats) throws DocumentException {
        doc.add(new Paragraph("Monitor Configuration & Availability", SECTION_FONT));
        doc.add(new Paragraph(" ", FontFactory.getFont(FontFactory.HELVETICA, 4)));

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setSpacingAfter(15);
        table.setWidths(new float[]{25f, 25f, 25f, 25f});

        addHeaderCell(table, "Monitor Name");
        addHeaderCell(table, "Health Status");
        addHeaderCell(table, "Check Interval");
        addHeaderCell(table, "Uptime %");

        addBodyCell(table, monitor.getName(), false);
        addBodyCell(table, monitor.getHealthStatus().name(), false);
        addBodyCell(table, monitor.getCheckIntervalSeconds() + "s", false);
        addBodyCell(table, stats.getUptimePercentage() != null ? String.format("%.2f%%", stats.getUptimePercentage()) : "No data", false);

        addHeaderCell(table, "Total Checks");
        addHeaderCell(table, "Successful");
        addHeaderCell(table, "Failed");
        addHeaderCell(table, "Failure Rate");

        addBodyCell(table, String.valueOf(stats.getTotalChecks()), true);
        addBodyCell(table, String.valueOf(stats.getSuccessChecks()), true);
        addBodyCell(table, String.valueOf(stats.getFailureChecks()), true);
        addBodyCell(table, stats.getFailureRate() != null ? String.format("%.2f%%", stats.getFailureRate()) : "No data", true);

        doc.add(table);
    }

    private void addPerformanceMetricsSection(Document doc, MonitorStats stats) throws DocumentException {
        doc.add(new Paragraph("Latency & SLA Analysis", SECTION_FONT));
        doc.add(new Paragraph(" ", FontFactory.getFont(FontFactory.HELVETICA, 4)));

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setSpacingAfter(15);
        table.setWidths(new float[]{20f, 20f, 20f, 20f, 20f});

        addHeaderCell(table, "Avg Latency");
        addHeaderCell(table, "Min Latency");
        addHeaderCell(table, "Max Latency");
        addHeaderCell(table, "P95 Latency");
        addHeaderCell(table, "P99 Latency");

        addBodyCell(table, stats.getAvgLatencyMs() != null ? String.format("%.1f ms", stats.getAvgLatencyMs()) : "No data", false);
        addBodyCell(table, stats.getMinLatencyMs() != null ? stats.getMinLatencyMs() + " ms" : "No data", false);
        addBodyCell(table, stats.getMaxLatencyMs() != null ? stats.getMaxLatencyMs() + " ms" : "No data", false);
        addBodyCell(table, stats.getP95LatencyMs() != null ? stats.getP95LatencyMs() + " ms" : "No data", false);
        addBodyCell(table, stats.getP99LatencyMs() != null ? stats.getP99LatencyMs() + " ms" : "No data", false);

        doc.add(table);
    }

    private void addPortfolioExecutiveSummary(Document doc, User user, List<MonitorStats> monitorStats, List<Incident> incidents) throws DocumentException {
        doc.add(new Paragraph("Portfolio Executive Summary", SECTION_FONT));
        doc.add(new Paragraph(" ", FontFactory.getFont(FontFactory.HELVETICA, 4)));

        long totalChecks = monitorStats.stream().mapToLong(MonitorStats::getTotalChecks).sum();
        long successChecks = monitorStats.stream().mapToLong(MonitorStats::getSuccessChecks).sum();
        long failedChecks = monitorStats.stream().mapToLong(MonitorStats::getFailureChecks).sum();
        double overallUptime = totalChecks > 0 ? ((double) successChecks / totalChecks) * 100.0 : 0.0;

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setSpacingAfter(15);
        table.setWidths(new float[]{25f, 25f, 25f, 25f});

        addHeaderCell(table, "Monitored Services");
        addHeaderCell(table, "Total Checks");
        addHeaderCell(table, "Overall Uptime");
        addHeaderCell(table, "Total Incidents");

        addBodyCell(table, String.valueOf(monitorStats.size()), false);
        addBodyCell(table, String.valueOf(totalChecks), false);
        addBodyCell(table, totalChecks > 0 ? String.format("%.2f%%", overallUptime) : "No data", false);
        addBodyCell(table, String.valueOf(incidents.size()), false);

        doc.add(table);
    }

    private void addAllMonitorsTable(Document doc, List<MonitorStats> monitorStats) throws DocumentException {
        doc.add(new Paragraph("Service Uptime & Latency Breakdown", SECTION_FONT));
        doc.add(new Paragraph(" ", FontFactory.getFont(FontFactory.HELVETICA, 4)));

        if (monitorStats.isEmpty()) {
            doc.add(new Paragraph("No monitors configured in this portfolio.", SUBTITLE_FONT));
            return;
        }

        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setSpacingAfter(15);
        table.setWidths(new float[]{28f, 16f, 14f, 14f, 14f, 14f});

        addHeaderCell(table, "Monitor Name");
        addHeaderCell(table, "Health");
        addHeaderCell(table, "Checks");
        addHeaderCell(table, "Uptime %");
        addHeaderCell(table, "Avg Latency");
        addHeaderCell(table, "P95 Latency");

        boolean alt = false;
        for (MonitorStats s : monitorStats) {
            addBodyCell(table, s.getMonitor().getName(), alt);
            addBodyCell(table, s.getMonitor().getHealthStatus().name(), alt);
            addBodyCell(table, String.valueOf(s.getTotalChecks()), alt);
            addBodyCell(table, s.getUptimePercentage() != null ? String.format("%.2f%%", s.getUptimePercentage()) : "No data", alt);
            addBodyCell(table, s.getAvgLatencyMs() != null ? String.format("%.0f ms", s.getAvgLatencyMs()) : "—", alt);
            addBodyCell(table, s.getP95LatencyMs() != null ? s.getP95LatencyMs() + " ms" : "—", alt);
            alt = !alt;
        }

        doc.add(table);
    }

    private void addPerformanceSlaTable(Document doc, List<MonitorStats> monitorStats) throws DocumentException {
        doc.add(new Paragraph("Latency SLA & Percentile Distribution", SECTION_FONT));
        doc.add(new Paragraph(" ", FontFactory.getFont(FontFactory.HELVETICA, 4)));

        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setSpacingAfter(15);
        table.setWidths(new float[]{28f, 14f, 14f, 14f, 15f, 15f});

        addHeaderCell(table, "Service");
        addHeaderCell(table, "Min");
        addHeaderCell(table, "Avg");
        addHeaderCell(table, "P95");
        addHeaderCell(table, "P99");
        addHeaderCell(table, "Max");

        boolean alt = false;
        for (MonitorStats s : monitorStats) {
            addBodyCell(table, s.getMonitor().getName(), alt);
            addBodyCell(table, s.getMinLatencyMs() != null ? s.getMinLatencyMs() + "ms" : "—", alt);
            addBodyCell(table, s.getAvgLatencyMs() != null ? String.format("%.0fms", s.getAvgLatencyMs()) : "—", alt);
            addBodyCell(table, s.getP95LatencyMs() != null ? s.getP95LatencyMs() + "ms" : "—", alt);
            addBodyCell(table, s.getP99LatencyMs() != null ? s.getP99LatencyMs() + "ms" : "—", alt);
            addBodyCell(table, s.getMaxLatencyMs() != null ? s.getMaxLatencyMs() + "ms" : "—", alt);
            alt = !alt;
        }

        doc.add(table);
    }

    private void addIncidentLogSection(Document doc, List<Incident> incidents) throws DocumentException {
        doc.add(new Paragraph("Incidents & Outage Log", SECTION_FONT));
        doc.add(new Paragraph(" ", FontFactory.getFont(FontFactory.HELVETICA, 4)));

        if (incidents.isEmpty()) {
            Paragraph p = new Paragraph("No incidents reported during this reporting window. 100% incident-free operation.", SUBTITLE_FONT);
            p.setSpacingAfter(15);
            doc.add(p);
            return;
        }

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setSpacingAfter(15);
        table.setWidths(new float[]{22f, 32f, 14f, 18f, 14f});

        addHeaderCell(table, "Service");
        addHeaderCell(table, "Incident Title");
        addHeaderCell(table, "Status");
        addHeaderCell(table, "Started At");
        addHeaderCell(table, "Downtime");

        boolean alt = false;
        for (Incident inc : incidents) {
            addBodyCell(table, inc.getMonitor().getName(), alt);
            addBodyCell(table, inc.getTitle(), alt);
            addBodyCell(table, inc.getStatus().name(), alt);
            addBodyCell(table, DATE_FORMATTER.format(inc.getStartedAt()), alt);
            long downtime = inc.getDowntimeSeconds() != null ? inc.getDowntimeSeconds() / 60 : 0;
            addBodyCell(table, downtime > 0 ? downtime + " min" : "Active", alt);
            alt = !alt;
        }

        doc.add(table);
    }

    // --- Helper Methods ---

    private void addHeaderCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, TH_FONT));
        cell.setBackgroundColor(TABLE_HEADER_BG);
        cell.setPadding(6f);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setBorderColor(new Color(51, 65, 85));
        table.addCell(cell);
    }

    private void addBodyCell(PdfPTable table, String text, boolean altRow) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "—", TD_FONT));
        if (altRow) {
            cell.setBackgroundColor(ALT_ROW_BG);
        }
        cell.setPadding(5f);
        cell.setBorderColor(new Color(226, 232, 240));
        table.addCell(cell);
    }

    private static class PageNumberFooterEvent extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            Phrase footer = new Phrase(
                    String.format("CloudPulse Reliability Report • Page %d • Confidential", writer.getPageNumber()),
                    FontFactory.getFont(FontFactory.HELVETICA, 8, TEXT_MUTED)
            );
            ColumnText.showTextAligned(
                    cb,
                    Element.ALIGN_CENTER,
                    footer,
                    (document.right() - document.left()) / 2 + document.leftMargin(),
                    document.bottom() - 10,
                    0
            );
        }
    }
}
