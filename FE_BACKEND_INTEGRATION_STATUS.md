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

## 실제 서버 응답 확인 결과

| 기능 | API | 결과 | 판단 |
| --- | --- | --- | --- |
| 로그인 | `POST /api/auth/login` | 200 | 정상 |
| 교내 시간표 | `GET /api/timetable?inOutCampus=IN_CAMPUS` | 200 | 정상 |
| 교외 시간표 | `GET /api/timetable?inOutCampus=OUT_CAMPUS` | 200 | 정상 |
| 즐겨찾기 조회 | `GET /api/favorite` | 200 | 정상 |
| 좌석 조회 | `GET /api/buses/1/seats` | 404, 해당 버스를 찾을 수 없습니다 | 서버 DB에 timetable과 연결된 Bus 데이터 필요 |
| 기사 출발 등록 | `PATCH /api/timetable/1/depart` | 404, 해당 버스를 찾을 수 없습니다 | 서버 DB에 timetable과 연결된 Bus 데이터 필요 |
| 기사 GPS 전송 | `POST /api/driver/location` | 500, 서버 내부 오류 | 백엔드 서버 로직 확인 필요 |
| 실시간 ETA | `GET /api/timetables/1/live` | 토큰 포함 호출에도 401 | SSE API 인증 처리 확인 필요 |
| 개인정보 약관 | `GET /api/terms/privacy` | 400, 약관을 찾을 수 없습니다 | 서버에 PDF 등록 필요 |
| 서비스 약관 | `GET /api/terms/service` | 400, 약관을 찾을 수 없습니다 | 서버에 PDF 등록 필요 |

## 배포 전 확인이 필요한 백엔드 작업

1. 시간표와 연결된 Bus 데이터 생성 또는 초기화 로직 확인
2. `POST /api/driver/location` 500 원인 확인
3. `GET /api/timetables/{timetableId}/live` 토큰 인증 처리 확인
4. 개인정보 처리방침 PDF와 서비스 이용 약관 PDF 등록
5. 관리자 계정으로 시간표/PDF 업로드 클릭 테스트

## FE 빌드 확인

- `:app:compileDebugKotlin` 성공
- `:app:testDebugUnitTest` 성공
- `:app:assembleDebug` 성공
- `:app:assembleRelease` 성공

