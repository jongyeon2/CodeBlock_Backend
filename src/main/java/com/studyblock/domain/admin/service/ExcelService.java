package com.studyblock.domain.admin.service;

import com.studyblock.domain.user.dto.UserProfileResponse;
import com.studyblock.domain.user.entity.User;
import com.studyblock.domain.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExcelService {

    private final UserRepository userRepository;

    public void downloadUserExcel(HttpServletResponse response) throws IOException {
        List<User> userAll = userRepository.findAll();

        List<UserProfileResponse> userList = userAll.stream()
                .map(user1 -> UserProfileResponse.builder()
                        .name(user1.getName())
                        .email(user1.getEmail())
                        .phone(user1.getPhone())
                        .nickname(user1.getNickname())
                        .gender(user1.getGenderEnum())
                        .birth(user1.getBirth())
                        .jointype(user1.getJoinTypeEnum())
                        .created_at(user1.getCreatedAt())
                        .build()
                ).toList();

        // 대용량 파일 대응용 SXSSFWorkbook 사용(10만행 이상) -> 소량의 파일이면 XSSFWorkbook 사용해도 문제없음
        SXSSFWorkbook workbook = new SXSSFWorkbook();
        Sheet sheet = workbook.createSheet("UserList");

        // 스타일 설정
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle bodyStyle = createBodyStyle(workbook);
        CellStyle dateStyle = createDateStyle(workbook);

        // 헤더 생성
        String[] headers = {"ID", "이름", "닉네임", "이메일", "전화번호", "가입일"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // 데이터 채우기
        int rowNum = 1;
        for (UserProfileResponse user : userList) {
            // 행 증가
            Row row = sheet.createRow(rowNum++);

            // 사용자 아이디
            row.createCell(0).setCellValue(user.getMemberId());
            row.getCell(0).setCellStyle(bodyStyle);

            // 사용자 이름
            row.createCell(1).setCellValue(user.getName());
            row.getCell(1).setCellStyle(bodyStyle);

            // 사용자 닉네임
            row.createCell(2).setCellValue(user.getNickname());
            row.getCell(2).setCellStyle(bodyStyle);

            // 사용자 이메일
            row.createCell(3).setCellValue(user.getEmail());
            row.getCell(3).setCellStyle(bodyStyle);

            // 사용자 전화번호
            row.createCell(4).setCellValue(user.getPhone());
            row.getCell(4).setCellStyle(bodyStyle);

            // 사용자 가입일
            Cell dateCell = row.createCell(5);
            dateCell.setCellValue(user.getCreated_at().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            dateCell.setCellStyle(dateStyle);
        }

        /*// 자동열맞춤 -> SXSSFWorkbook에서는 자동열맞춤을 지원하지 않음.
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
        */

        // 파일명 utf-8 인코딩
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String fileName = URLEncoder.encode("사용자목록_" + today + ".xlsx", StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");

        // 응답 설정
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName);

        workbook.write(response.getOutputStream());
        workbook.dispose();
    }


    /* Excel 스타일 지정 */
    // 헤더 스타일
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);

        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);

        return style;
    }

    // 바디 스타일
    private CellStyle createBodyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);

        return style;
    }

    // 날짜 스타일
    private CellStyle createDateStyle(Workbook workbook) {
        CellStyle style = createBodyStyle(workbook);
        CreationHelper creationHelper = workbook.getCreationHelper();
        style.setDataFormat(creationHelper.createDataFormat().getFormat("yyyy-MM-dd"));

        return style;
    }
}
