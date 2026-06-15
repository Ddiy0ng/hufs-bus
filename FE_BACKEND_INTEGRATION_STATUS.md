# FE-BE 연동 상태 정리

기준 브랜치: `fe/api-integration`
기준 서버: `http://13.209.95.60:8080`

## FE에서 반영한 내용

- 로그인: `POST /api/auth/login`
- 회원가입: `POST /api/auth/signup`
- 시간표 조회: `GET /api/timetable`
- 즐겨찾기 조회/등록: `GET /api/favorite`, `POST /api/favorite`
- 기사 출발 등록: `PATCH /api/timetable/{timetableId}/depart`
- 기사 GPS 전송: `POST /api/driver/location`
- 관리자 수기 탑승/하차: `POST /api/buses/{timetableId}/tags`
- 약관 PDF 조회/업로드: `/api/terms/privacy`, `/api/terms/service`
- 실시간 ETA: `GET /api/timetables/{timetableId}/live?token={JWT}` SSE snapshot 수신

## 실제 서버 응답 확인 결과

| 기능 | API | 결과 | 판단 |
| --- | --- | --- | --- |
| 로그인 | `POST /api/auth/login` | 200 | 정상 |
| 교내 시간표 | `GET /api/timetable?inOutCampus=IN_CAMPUS` | 200 | 정상 |
| 교외 시간표 | `GET /api/timetable?inOutCampus=OUT_CAMPUS` | 200 | 정상 |
| 즐겨찾기 조회 | `GET /api/favorite` | 200 | 정상 |
| 좌석 조회 | `GET /api/buses/{timetableId}/seats` | 404, 해당 버스를 찾을 수 없습니다 | 2026-06-15 재확인 결과 `GET /api/timetable` 응답의 `timetableId` 105~109 그대로 호출해도 모두 404. 서버 재배포 및 시간표 재업로드 후 재검증 필요 |
| 버스 상태 조회 | `GET /api/buses/{timetableId}/statuses` | 500 | 2026-06-15 재확인 결과 `timetableId` 105~109 모두 500. FE는 `/statuses` 실패 시 `/seats`로 fallback하도록 구현됨 |
| 기사 출발 등록 | `PATCH /api/timetable/{timetableId}/depart` | 미실행 | 실제 서버 상태를 바꾸는 요청이라 좌석 조회 200 확인 후 테스트 예정 |
| 기사 GPS 전송 | `POST /api/driver/location` | 500, 서버 내부 오류 | Bus 데이터 생성 수정 배포 후 busId 기준으로 재검증 필요 |
| 실시간 ETA | `GET /api/timetables/{timetableId}/live?token={JWT}` | 401, 인증이 필요합니다 | 2026-06-15 재확인 결과 같은 토큰으로 `GET /api/favorite`는 200이지만 live는 헤더/쿼리 모두 401. 서버 재배포 또는 live 인증 설정 확인 필요 |
| 개인정보 약관 | `GET /api/terms/privacy` | 400, 약관을 찾을 수 없습니다 | 서버에 PDF 등록 필요 |
| 서비스 약관 | `GET /api/terms/service` | 400, 약관을 찾을 수 없습니다 | 서버에 PDF 등록 필요 |

## 배포 전 확인이 필요한 백엔드 작업

1. 백엔드 `origin/soyeon` 브랜치 서버 재배포
2. 재배포 후 관리자 계정으로 시간표 엑셀 재업로드
3. 시간표 업로드 후 `bus` 데이터가 timetable과 연결되어 생성되는지 확인
4. `PATCH /api/timetable/{timetableId}/depart` 호출 시 Bus 상태가 `RUNNING`으로 바뀌는지 확인
5. `GET /api/buses/{timetableId}/seats` 응답에 `busId`, `status`, `totalSeats`, `currentSeats`가 내려오는지 확인
6. `GET /api/timetables/{timetableId}/live?token={JWT}` SSE snapshot 수신 확인
7. 개인정보 처리방침 PDF와 서비스 이용 약관 PDF 등록
8. 관리자 계정으로 시간표/PDF 업로드 클릭 테스트

## FE 빌드 확인

- `:app:compileDebugKotlin` 성공
- `:app:testDebugUnitTest` 성공
- `:app:assembleDebug` 성공
- `:app:assembleRelease` 성공
