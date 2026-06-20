package hufsbus.spring.domain.util;

import hufsbus.spring.domain.timetable.dto.ExcelRequestDto;
import hufsbus.spring.domain.timetable.timetableEnum.BusStopEnum;
import hufsbus.spring.domain.timetable.timetableEnum.BusWayEnum;
import hufsbus.spring.domain.timetable.timetableEnum.InOutCampusEnum;
import hufsbus.spring.global.exception.CustomException;
import hufsbus.spring.global.exception.ErrorCode;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Component
public class FileUpload {

    private static final Logger log = LoggerFactory.getLogger(FileUpload.class);
    private static final DataFormatter DATA_FORMATTER = new DataFormatter();

    /*-----------액셀 파싱-----------*/
    // 시간표 등록
    public List<ExcelRequestDto> fileToTimetable(InputStream inputStream) {

        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            List<ExcelRequestDto> timetableList = new ArrayList<>();

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);

                if (row == null || isBlankRow(row)) {
                    continue;
                }

                try {
                    String departAt = getCellValue(row, 0);
                    String inOutCampus = getCellValue(row, 1);
                    String busWay = getCellValue(row, 2);
                    String startStop = getCellValue(row, 3);
                    String route = getCellValue(row, 4);

                    ExcelRequestDto dto = ExcelRequestDto.builder()
                            .departAt(parseTime(departAt, rowIndex))
                            .inOutCampus(InOutCampusEnum.from(inOutCampus))
                            .busWay(BusWayEnum.from(busWay))
                            .startStop(BusStopEnum.from(startStop))
                            .route(route)
                            .build();

                    timetableList.add(dto);
                } catch (CustomException e) {
                    throw e;
                } catch (Exception e) {
                    log.error("엑셀 파싱 실패 - row: {}, 원인: {}", rowIndex + 1, e.getMessage());
                    throw new CustomException(ErrorCode.EXCEL_PARSING_EXCEPTION);
                }
            }

            return timetableList;

        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.EXCEL_PARSING_EXCEPTION);
        }
    }

    private LocalTime parseTime(String value, int rowIndex) {
        String normalized = value.trim();
        // "8:20" → "08:20"
        if (normalized.matches("^\\d:\\d{2}$")) {
            normalized = "0" + normalized;
        }
        // "08:20:00" → "08:20"
        if (normalized.matches("^\\d{2}:\\d{2}:\\d{2}$")) {
            normalized = normalized.substring(0, 5);
        }
        try {
            return LocalTime.parse(normalized, DateTimeFormatter.ofPattern("HH:mm"));
        } catch (DateTimeParseException e) {
            log.error("시간 파싱 실패 - row: {}, 값: '{}'", rowIndex + 1, value);
            throw new CustomException(ErrorCode.EXCEL_PARSING_EXCEPTION);
        }
    }

    // 빈 행인지 확인
    private static boolean isBlankRow(Row row) {
        for (int i = 0; i <= 4; i++) {
            Cell cell = row.getCell(i);

            if (cell != null && !cell.toString().trim().isEmpty()) {
                return false;
            }
        }

        return true;
    }

    // 셀 값
    private static String getCellValue(Row row, int index) {
        Cell cell = row.getCell(index);

        if (cell == null) {
            throw new CustomException(ErrorCode.EXCEL_PARSING_EXCEPTION);
        }

        String value = DATA_FORMATTER.formatCellValue(cell).trim();

        if (value.isEmpty()) {
            throw new CustomException(ErrorCode.EXCEL_PARSING_EXCEPTION);
        }

        return value;
    }


    /*-----------pdf 파싱-----------*/

}

