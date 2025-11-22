# 🚌 **HopOn – 스마트 모빌리티 통합 플랫폼**

<p align="center">
  <img src="https://github.com/TeamProject-Seoil/HopOn_ADMIN_Page/blob/main/src/assets/%EC%82%AC%EC%9A%A9%EC%9E%90%EC%95%B1%20%ED%99%94%EB%A9%B4.png" width="250"/>
  <img src="https://github.com/TeamProject-Seoil/HopOn_ADMIN_Page/blob/main/src/assets/%EA%B8%B0%EC%82%AC%EC%95%B1%20%ED%99%94%EB%A9%B4.png" width="250"/>
  <img src="https://github.com/TeamProject-Seoil/HopOn_ADMIN_Page/blob/main/src/assets/%EA%B4%80%EB%A6%AC%EC%9E%90%ED%8E%98%EC%9D%B4%EC%A7%80%20%ED%99%94%EB%A9%B4.png" width="450"/>
</p>

<p align="center">
  <b>사용자 앱</b> &nbsp;&nbsp;&nbsp;&nbsp; <b>기사 앱</b> &nbsp;&nbsp;&nbsp;&nbsp; <b>관리자 웹 대시보드</b>
</p>

---

## 📦 **Repository List**

| List | Repository | Link |
|------|------------|------|
| 사용자 앱 | 📱 **HopOn_UserAPP (Android)** | [![Repo](https://img.shields.io/badge/GitHub-HopOn__UserAPP-181717?logo=github)](https://github.com/TeamProject-Seoil/HopOn_UserApp) |
| 기사 앱 | 🚗 **HopOn_DriverAPP (Android)** | [![Repo](https://img.shields.io/badge/GitHub-HopOn__DriverAPP-181717?logo=github)](https://github.com/TeamProject-Seoil/HopOn_DriverApp) |
| 관리자 대시보드 | 🌐 **HopOn_ADMIN_Page (Vue 3)** | [![Repo](https://img.shields.io/badge/GitHub-HopOn__Admin_Page-181717?logo=github)](https://github.com/TeamProject-Seoil/HopOn_ADMIN_Page) |
| 사용자 + 기사 백엔드 | ⚙️ **HopOn (Spring Boot)** | [![Repo](https://img.shields.io/badge/GitHub-HopOn__Backend-181717?logo=github)](https://github.com/TeamProject-Seoil/HopOn) |
| 관리자 백엔드 | ⚙️ **HopOn_ADMIN (Spring Boot)** | [![Repo](https://img.shields.io/badge/GitHub-HopOn__Admin_Backend-181717?logo=github)](https://github.com/TeamProject-Seoil/HopOn_ADMIN) |

---

# 🌿 **프로젝트 소개**

**HopOn**은 실시간 GPS 기반으로  
**사용자 → 기사 → 관리자**가 완전히 연결되는 **통합 셔틀/버스 플랫폼**입니다.

- 사용자: 예약 / 실시간 위치 / 승하차 알림  
- 기사: 운행 관리 / 지연 설정 / GPS Heartbeat  
- 관리자: 예약, 회원, 공지, 문의까지 한 곳에서 관리  

> 🚀 **버스/셔틀 운영을 하나의 생태계로 통합한 서비스**

---

# 👥 **팀원 소개**

| 이름 | 역할 | 담당 | 
|------|------|------|
| **조건희** | Frontend (팀장) | 사용자·기사 앱 UI 개발, 화면 구성, 공통 컴포넌트 제작, PPT, 일정·역할 관리 |
| **김민재** | Frontend | 사용자·기사 앱 UI 개발, 주요 화면 구성, 컴포넌트 제작, 발표 |
| **유주현** | Frontend | 사용자·기사 앱 UI 개발, 화면 구성, 컴포넌트 제작, 홍보 영상 제작, PPT 지원 |
| **최준영** | Backend | 사용자 기능, 메인 화면 로직, 버스 API 연동, Naver Maps 연동 |
| **원동건** | Backend | 기사 기능, 관리자 웹(Vue), 백엔드 핵심 API(Spring), JWT/권한 처리, 예약·문의·즐겨찾기, AWS 배포 |

---

# ✨ **핵심 기능**

## 👤 사용자 앱 (User App)
- 정류장 기반 예약 생성/취소  
- 실시간 버스 위치 조회  
- 승차/하차 알림  
- 지연 상태 확인  
- 즐겨찾기·최근 기록  
- Naver Map 기반 경로 표시  

## 🚗 기사 앱 (Driver App)
- GPS Heartbeat 자동 송신  
- 승객 목록 조회  
- 지연 설정  
- 운행 시작/종료  
- 승차/하차 처리 + 알림 전송  

## 🛠 관리자(Admin Page)
- 회원/기사 관리  
- 예약 관리  
- 공지/문의 관리  
- 관리자 계정 관리  
- Vue3 기반 반응형 대시보드  

---

# 🛠 **기술 스택**

### 💻 Frontend (Admin)
- Vue 3 / Vite / Pinia / Axios  

### 📱 Android (User/Driver)
- Java / Retrofit2 / Naver Map SDK / Material Design  

### ⚙ Backend
- Spring Boot 3  
- Spring Security + JWT  
- JPA / Hibernate  
- MySQL (AWS RDS)  
- SSE(Server-Sent Events)  

### ☁ Infra
- AWS EC2 / RDS  
- Docker + Docker Compose  
- Nginx Reverse Proxy  
- Route53 + 가비아 + Certbot(HTTPS)  
- GitHub Actions(CI/CD)  

---

# 🚀 **배포 및 CI/CD**

- GitHub Actions → Docker 이미지 자동 Build & Push  
- EC2 → docker-compose pull 로 최신 버전 자동 배포  
- Vue/Admin/Backend 모두 Docker 기반 운영  
- Nginx Reverse Proxy로 HTTPS 및 라우팅 관리  
- Mixed Content 문제 해결 (API HTTPS 통합)  
- Route53 + 가비아 DNS 적용 후 Certbot으로 SSL 인증서 자동 갱신  

---

# 🌐 **배포 구조**

| 구분 | 도메인 / 경로 | 연결 대상 | 설명 |
|------|----------------|------------|------|
| 사용자 앱 | GitHub Release 다운로드 | HopOn_UserApp | 사용자 앱 |
| 기사 앱 | GitHub Release 다운로드 | HopOn_DriverApp | 기사 앱 |
| 관리자 프론트엔드 | https://www.hoponhub.store | HopOn_ADMIN_Page (Vue 3) | 관리자 웹 대시보드 |
| 사용자 + 기사 백엔드 | `/api`, `/auth`, `/users` | HopOn Backend | 예약, 운행, 즐겨찾기, 버스 API |
| 관리자 백엔드 | `/admin`, `/auth`, `/users`, `/reservations`, `/actuator/health` | HopOn_ADMIN Backend | 관리자 전용 API |

---

# 📌 문의 & 요청

README 더 예쁘게 다듬고 싶거나  
구조도(Deployment Diagram) 이미지 버전도 만들고 싶으면 말해줘!  
**GitHub 프로필 스타일, 문서 스타일**로도 바꿔줄 수 있음 😄
