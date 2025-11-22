# 🚌 **HopOn – 스마트 모빌리티 통합 플랫폼**

> **사용자, 기사, 관리자**가 하나의 플랫폼에서 연결되는  
> 실시간 위치 기반 **예약·운행·관리 통합 서비스**

---

<p align="center">
  <img src="https://github.com/TeamProject-Seoil/HopOn_ADMIN_Page/blob/main/src/assets/%EC%82%AC%EC%9A%A9%EC%9E%90%EC%95%B1%20%ED%99%94%EB%A9%B4.png" width="220"/>
  <img src="https://github.com/TeamProject-Seoil/HopOn_ADMIN_Page/blob/main/src/assets/%EA%B8%B0%EC%82%AC%EC%95%B1%20%ED%99%94%EB%A9%B4.png" width="220"/>
  <img src="https://github.com/TeamProject-Seoil/HopOn_ADMIN_Page/blob/main/src/assets/%EA%B4%80%EB%A6%AC%EC%9E%90%ED%8E%98%EC%9D%B4%EC%A7%80%20%ED%99%94%EB%A9%B4.png" width="500"/>
</p>

<p align="center">
  <b>사용자 앱</b> &nbsp;&nbsp;&nbsp;&nbsp;&nbsp; <b>기사 앱</b> &nbsp;&nbsp;&nbsp;&nbsp;&nbsp; <b>관리자 웹 대시보드</b>
</p>

---

# 📦 **Repository List**

| List | Repository | Link |
|------|------------|------|
| 사용자 앱 | 📱 **HopOn_UserAPP (Android)** | [![Repo](https://img.shields.io/badge/GitHub-HopOn__UserAPP-181717?logo=github)](https://github.com/TeamProject-Seoil/HopOn_UserApp) |
| 기사 앱 | 🚗 **HopOn_DriverAPP (Android)** | [![Repo](https://img.shields.io/badge/GitHub-HopOn__DriverAPP-181717?logo=github)](https://github.com/TeamProject-Seoil/HopOn_DriverApp) |
| 관리자 대시보드 | 🌐 **HopOn_ADMIN_Page (Vue 3)** | [![Repo](https://img.shields.io/badge/GitHub-HopOn__Admin_Page-181717?logo=github)](https://github.com/TeamProject-Seoil/HopOn_ADMIN_Page) |
| 사용자 + 기사 백엔드 | ⚙ **HopOn (Spring Boot)** | [![Repo](https://img.shields.io/badge/GitHub-HopOn__Backend-181717?logo=github)](https://github.com/TeamProject-Seoil/HopOn) |
| 관리자 백엔드 | ⚙ **HopOn_ADMIN (Spring Boot)** | [![Repo](https://img.shields.io/badge/GitHub-HopOn__Admin_Backend-181717?logo=github)](https://github.com/TeamProject-Seoil/HopOn_ADMIN) |

---

# 🌿 **프로젝트 소개**

**HopOn**은 실시간 GPS 기반으로  
사용자 앱 → 기사 앱 → 관리자 웹 → 백엔드가 완전 연동되는  
**End-to-End 모빌리티 운영 플랫폼**입니다.

- 사용자 → 예약, 도착·하차 알림, 실시간 위치  
- 기사 → GPS 업로드, 지연 설정, 승객 관리  
- 관리자 → 예약·회원·문의·권한 관리  

**하나의 생태계에서 모든 서비스가 실시간으로 동작**합니다.

---

# 👥 **팀원 소개**

| 이름 | 역할 | 담당 |
|------|------|------|
| **조건희** | 팀장 (Frontend) | 사용자/기사 앱 UI 개발, 컴포넌트 제작, 전체 앱 플로우, PPT, 일정 관리 |
| **김민재** | Frontend | 사용자/기사 앱 UI 개발, 주요 화면 구성, 발표 |
| **유주현** | Frontend | 사용자/기사 앱 UI 개발, 기능 일부 구현, 홍보 영상 제작, PPT 지원 |
| **최준영** | Backend | 사용자 기능, 메인 로직, 버스 데이터 Open API 연동, Naver Maps 연동 |
| **원동건** | Backend | 기사 기능, 관리자 웹(Vue), 백엔드(Spring), JWT/권한 처리, 예약·문의·즐겨찾기, AWS 배포 |

---

# ✨ **주요 기능**

## 👤 사용자(User App)
- 정류장 기반 예약 생성/취소  
- 실시간 버스 위치 조회  
- 승차/하차 알림  
- 지연 상태 확인  
- 즐겨찾기 & 최근 이용  
- Naver Map 기반 경로 표시  

---

## 🚗 기사(Driver App)
- GPS Heartbeat 자동 전송  
- 승객 리스트 조회  
- 지연 여부 설정  
- 운행 시작/종료  
- 승차/하차 처리 및 알림  

---

## 🛠 관리자(Admin Page)
- 회원/기사/예약 대시보드  
- 공지/문의 관리  
- 관리자 계정 및 권한 관리  
- Vue 3 기반 반응형 UI  

---

# 🛠 **기술 스택**

### 💻 Frontend(Admin)
Vue 3 · Vite · Pinia · Axios · Vue Router

### 📱 Android(User/Driver)
Java · Retrofit2 · Naver Map SDK · Material3

### ⚙ Backend
Spring Boot 3 · Spring Security(JWT) · JPA · SSE · MySQL(AWS RDS)

### ☁ Infra
AWS EC2 · RDS · Route53 · Docker · Docker Compose · Nginx · Certbot · GitHub Actions

---

# 🚀 **배포 및 CI/CD**

- GitHub Actions → Docker 이미지 자동 빌드  
- EC2 → docker-compose pull 로 최신 배포  
- Nginx Reverse Proxy 구조  
- Route53 + 가비아 DNS 연결  
- Certbot HTTPS 자동 적용  
- Mixed Content 문제 해결  

---

# 🌐 **배포 구조**

| 구분 | 도메인 / 경로 | 연결 대상 | 설명 |
|------|----------------|------------|------|
| 사용자 프론트엔드 | GitHub Release 다운로드 | HopOn_UserApp | HopOn 사용자 앱 |
| 기사 프론트엔드 | GitHub Release 다운로드 | HopOn_DriverApp | HopOn 기사 앱 |
| 관리자 프론트엔드 | https://www.hoponhub.store | HopOn_ADMIN_Page | 관리자 웹 |
| 사용자 + 기사 백엔드 | `/api`, `/auth`, `/users` | HopOn Backend | 예약/문의/즐겨찾기/버스/운행 API |
| 관리자 백엔드 | `/admin`, `/auth`, `/users`, `/reservations`, `/actuator/health` | HopOn_ADMIN Backend | 관리자 API |

---
