package hufsbus.spring.domain.timetable.service;

import hufsbus.spring.domain.timetable.dto.ExcelRequestDto;
import hufsbus.spring.domain.timetable.timetableEnum.BusStopEnum;
import hufsbus.spring.domain.timetable.timetableEnum.BusWayEnum;
import hufsbus.spring.global.exception.CustomException;
import hufsbus.spring.global.exception.ErrorCode;
import org.apache.poi.ss.usermodel.*;

import java.io.InputStream;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class FileUpload {

    public static List<ExcelRequestDto> fileToTimetable(InputStream inputStream) {

        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            List<ExcelRequestDto> timetableList = new ArrayList<>();

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);

                if (row == null || isBlankRow(row)) {
                    continue;
                }

                String departAt = getCellValue(row, 0);
                String busWay = getCellValue(row, 1);
                String startStop = getCellValue(row, 2);
                String route = getCellValue(row, 3);

                ExcelRequestDto dto = ExcelRequestDto.builder()
                        .departAt(LocalTime.parse(departAt))
                        .busWay(BusWayEnum.from(busWay))
                        .startStop(BusStopEnum.from(startStop))
                        .route(route)
                        .build();

                timetableList.add(dto);
            }

            return timetableList;

        } catch (Exception e) {
            throw new CustomException(ErrorCode.EXCEL_PARSING_EXCEPTION);
        }
    }

    private static boolean isBlankRow(Row row) {
        for (int i = 0; i <= 3; i++) {
            Cell cell = row.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);

            if (cell != null && !cell.toString().trim().isEmpty()) {
                return false;
            }
        }

        return true;
    }

    private static String getCellValue(Row row, int index) {
        Cell cell = row.getCell(index, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);

        if (cell == null) {
            throw new CustomException(ErrorCode.EXCEL_PARSING_EXCEPTION);
        }

        String value = cell.toString().trim();

        if (value.isEmpty()) {
            throw new CustomException(ErrorCode.EXCEL_PARSING_EXCEPTION);
        }

        return value;
    }
}

