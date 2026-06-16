package hufsbus.spring.domain.util;

import hufsbus.spring.domain.timetable.dto.ExcelRequestDto;
import hufsbus.spring.domain.timetable.timetableEnum.BusStopEnum;
import hufsbus.spring.domain.timetable.timetableEnum.BusWayEnum;
import hufsbus.spring.domain.timetable.timetableEnum.InOutCampusEnum;
import hufsbus.spring.global.exception.CustomException;
import hufsbus.spring.global.exception.ErrorCode;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class FileUpload {

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

                String departAt = getCellValue(row, 0);
                String inOutCampus = getCellValue(row, 1);
                String busWay = getCellValue(row, 2);
                String startStop = getCellValue(row, 3);
                String route = getCellValue(row, 4);

                ExcelRequestDto dto = ExcelRequestDto.builder()
                        .departAt(LocalTime.parse(departAt))
                        .inOutCampus(InOutCampusEnum.from(inOutCampus))
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

        String value = cell.toString().trim();

        if (value.isEmpty()) {
            throw new CustomException(ErrorCode.EXCEL_PARSING_EXCEPTION);
        }

        return value;
    }


    /*-----------pdf 파싱-----------*/

}

