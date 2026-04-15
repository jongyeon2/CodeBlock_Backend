# CodeBlock Backend

> 강의 탐색, 결제, 수강, 비디오 업로드를 지원하는 E-Learning 플랫폼 백엔드 서버

CodeBlock은 사용자가 강의를 탐색하고 결제해 수강할 수 있고, 강사는 강의를 등록하고 영상을 업로드할 수 있는 E-Learning 플랫폼입니다.

이 저장소는 CodeBlock의 백엔드 서버 레포지토리로, 인증/인가, 결제 처리, 강의/수강 도메인 API, 비디오 업로드 및 인코딩 기능을 담당합니다.

## My Role
- 팀 프로젝트에서 백엔드 구조 설계, API 설계, 인증/결제/강의 도메인 구현 담당
- 팀장으로 참여해 CI/CD 및 배포 환경 구성

## Core Features
- 강의 탐색, 결제, 수강 기능을 REST API 기반 계층형 구조로 설계
- 강의 업로드, 저장, 인코딩, 수강 흐름을 분리해 처리
- JWT Access/Refresh Token 기반 인증 및 Redis TTL 기반 Refresh Token 관리
- Docker와 GitHub Actions를 활용해 실행 환경과 배포 과정을 표준화

## What I Solved
- 업로드 요청과 인코딩 작업을 분리해 응답 지연 문제를 개선
- Refresh Token 저장과 만료를 Redis TTL로 함께 관리해 인증 흐름 단순화
- 결제 승인 전 검증과 중복 결제 방지를 위한 멱등성 처리 적용
- 전역 예외 처리와 환경별 설정 분리로 운영 안정성 확보
