# SwiftDrop — Team Contribution & Git Workflow Guide

Welcome to the SwiftDrop engineering team! This repository follows a structured monorepo Git workflow designed for 10 collaborating developers.

---

## 1. Team Responsibilities & Ownership Matrix

| Member | Assigned Service / Role | Git Feature Branch | Service Directory | Port |
|:---|:---|:---|:---|:---|
| **Member 1** | Frontend Architecture & UI/UX | `feature/frontend` | `frontend/` | `3000` |
| **Member 2** | Identity Service & Auth | `feature/identity-service` | `services/identity-service/` | `8081` |
| **Member 3** | Parcel Service & Order Intake | `feature/parcel-service` | `services/parcel-service/` | `8082` |
| **Member 4** | Merchant Management & B2B | `feature/merchant-service` | `services/merchant-service/` | `8083` |
| **Member 5** | Dispatch & Route Optimization | `feature/dispatch-service` | `services/dispatch-service/` | `8084` |
| **Member 6** | Dynamic Pricing & Billing | `feature/pricing-service` | `services/pricing-service/` | `8085` |
| **Member 7** | Real-Time Tracking & Telemetry | `feature/tracking-service` | `services/tracking-service/` | `8086` |
| **Member 8** | Notification & Messaging | `feature/notification-service` | `services/notification-service/` | `8087` |
| **Member 9** | AI Assistant & Analytics (Gemini) | `feature/ai-assistant` | `services/ai-assistant-service/` | `8088` |
| **Member 10** | API Gateway, DevOps & Integration | `feature/gateway-devops` | `gateway/` & root configs | `8080` |

---

## 2. Git Branching Rules & Workflow

1. **Single Monorepo**: All 10 microservices live within `https://github.com/swiftdrop-ead-team/swiftdrop-courier-management`. Never create separate individual repositories.
2. **Main Branch Protection**:
   - `main` contains stable, production-ready integrated code.
   - **Never commit directly to `main`**.
   - No force pushes (`git push --force` is strictly forbidden).
3. **Feature Branching**:
   - Branch off latest `main`:
     ```bash
     git checkout main
     git pull origin main
     git checkout -b feature/<your-assigned-service>
     ```
   - Keep commits focused and atomic with descriptive commit messages following Conventional Commits:
     ```text
     feat(identity): implement refresh token rotation
     fix(gateway): resolve CORS preflight headers on /api/v1/auth
     test(frontend): add validation tests for merchant registration
     ```
4. **Pull Requests (PRs)**:
   - Push your feature branch: `git push -u origin feature/<your-assigned-service>`
   - Open a PR into `main` using the repository PR template.
   - Require at least **1 peer review approval** before merging.
   - Ensure all automated GitHub Actions CI tests pass.
   - Use **Squash and Merge** or **Rebase and Merge** to maintain a clean linear history.

---

## 3. Microservice Boundary & Contract Rules

- Each team member is strictly responsible for their assigned service directory.
- Cross-service data access must occur via **REST APIs** or **RabbitMQ events**, never via cross-database SQL queries.
- Changes to API contracts must be documented in `docs/api-contracts/` and discussed with dependent service owners prior to merging.
- Shared environment keys must be added to `.env.example`.
