| List | Repository | Link |
|------|------------|------|
| 사용자 앱 | 📱 **HopOn_UserAPP (Android)** | [![Repo](https://img.shields.io/badge/GitHub-HopOn__UserAPP-181717?logo=github)](https://github.com/TeamProject-Seoil/HopOn_UserApp) |
| 기사 앱 | 🚗 **HopOn_DriverAPP (Android)** | [![Repo](https://img.shields.io/badge/GitHub-HopOn__DriverAPP-181717?logo=github)](https://github.com/TeamProject-Seoil/HopOn_DriverApp) |
| 관리자 프론트엔드 | 🌐 **HopOn_ADMIN_Page (Vue 3)** | [![Repo](https://img.shields.io/badge/GitHub-HopOn__Admin-Page-181717?logo=github)](https://github.com/TeamProject-Seoil/HopOn_ADMIN_Page) |
| 사용자 + 기사 백엔드 | ⚙️ **HopOn (Spring Boot)** | [![Repo](https://img.shields.io/badge/GitHub-HopOn__Backend-181717?logo=github)](https://github.com/TeamProject-Seoil/HopOn) |
| 관리자 백엔드 | ⚙️ **HopOn_ADMIN (Spring Boot)** | [![Repo](https://img.shields.io/badge/GitHub-HopOn__Admin-Backend-181717?logo=github)](https://github.com/TeamProject-Seoil/HopOn_ADMIN) |

---

# 🚌 HopOn

> **사용자, 기사, 관리자**가 하나의 플랫폼에서 연결되는  
> **스마트 모빌리티 통합 서비스**  
> 실시간 위치 기반 예약·운행·관리까지  
> 셔틀/버스 운영의 모든 과정을 혁신하는 솔루션

---

<!-- 여기에 대표 이미지 넣기 -->
<img width="1883" height="836" alt="hopon-preview" src="(이미지 링크)"/>

---

## 🌿 프로젝트 소개

**HopOn**은 실시간 GPS 기반 버스/셔틀 운행을  
“사용자 앱 – 기사 앱 – 관리자 웹 – 백엔드” 로 완전히 통합한  
**End-to-End 모빌리티 운영 플랫폼**입니다.

- 사용자 → 예약/도착확인/하차알림  
- 기사 → 승객 목록/지연 설정/심박(GPS) 업로드  
- 관리자 → 예약 및 회원 관리  

하나의 생태계에서 모든 운행 정보가 실시간으로 연결됩니다.

---

## 👥 팀원 소개

| 이름 | 역할 | 담당 | 
|------|------|------|
| **조건희** | 팀장 | 인프라·배포, API, SSE, 예약/운행 핵심 로직 |
| **김민재** | 팀원 | 지도 UI, 예약/알림, Overlay/Marker |
| **유주현** | 팀원 | 위치 업데이트, 운행 관리 UI |
| **최준영** | 팀원 | Vue3 UI, 대시보드, 권한 관리 |
| **원동건** | 팀원 | 디자인/문서 |

---

## ✨ 주요 기능

### 👤 사용자(User App)
- 🚌 정류장 기반 예약 생성/취소  
- 📍 실시간 버스 위치 조회  
- ⏳ 지연 상태 실시간 확인  
- 🔔 승차/하차 알림  
- ⭐ 즐겨찾기 및 최근 이력 저장  
- 🗺️ Naver Map 기반 경로/위치 표시  

---

### 🚗 기사(Driver App)
- 📡 GPS Heartbeat 자동 송신  
- 👥 예약 승객 리스트 확인  
- ⏱ 지연 여부 설정  
- 🟢 운행 시작 / 🔴 종료  
- 🔔 승차·하차 처리 및 알림 전송  

---

### 🛠 관리자(Admin Page)
- 📊 회원 및 예약 대시보드
- 👤 사용자/기사 관리    
- 📢 공지/문의/알림 관리  
- 🔐 관리자 권한 관리  
- Vue3 기반 반응형 관리자 UI  

---

## 🛠 기술 스택

### 💻 Frontend (Admin)
- Vue 3  
- Vite  
- Pinia  
- Axios  
- Vue Router  

### 📱 Android (User/Driver)
- Java  
- Retrofit2  
- Naver Map SDK  
- Material3  

### ⚙ Backend
- Spring Boot 3  
- Spring Security + JWT  
- JPA / Hibernate  
- MySQL(AWS RDS)  
- SSE (Server-Sent Events)  

### ☁ Infra
- AWS EC2  
- AWS RDS(MySQL)  
- AWS Route 53  
- Docker / Docker Compose  
- Nginx Reverse Proxy  
- Certbot (HTTPS)  
- GitHub Actions (CI/CD)  

---

## 🚀 배포 및 CI/CD

- GitHub Actions → Docker 이미지 자동 Build & Push  
- EC2에서 docker-compose pull → 최신 버전 자동 배포  
- Nginx Reverse Proxy로 프론트/백엔드 트래픽 분리  
- Route53 + 가비아 + Certbot 으로 HTTPS 완전 적용  
- Mixed Content 해결(프론트 HTTPS API 세팅)  
- 모든 서비스 Docker 기반 실행  

---

## 🌐 배포 구조

| 구분 | 주소 | 설명 |
|------|------|------|
| 관리자 웹 | https://www.hoponhub.store | HopOn Admin Page |
| 사용자/기사 앱 | API 요청 | HopOn Backend |
| Backend API | `/auth`, `/users`, `/reservations`, `/driver`, `/admin` | Spring Boot 통합 백엔드 |

---
