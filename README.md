# Collaborative Study Platform (StudyHub)

Semester project: JavaFX desktop client + Spring Boot backend for collaborative study groups. The system supports authentication, groups, memberships, tasks, materials, activity log, statistics, and realtime updates via WebSocket.

## Architecture (client–server)
- **Client:** JavaFX (FXML), controllers use `ApiService` (HttpClient + JWT) and WebSocket notifications.
- **Server:** Spring Boot 3, Spring Security (JWT), JPA/Hibernate, SQLite.
- **Layers:** Controller → Service (business + access checks) → Repository (JPA) → DB. DTOs isolate API from entities.
- **Realtime:** WebSocket notifications for task create/update/delete, material add, member join/role change.

## Data model & invariants
```mermaid
erDiagram
  USERS { int user_id PK; string nickname; string email UNIQUE; string password_hash; datetime created_at }
  GROUPS { int group_id PK; string name; string description; int created_by FK->USERS; datetime created_at }
  MEMBERSHIPS { int id PK; int user_id FK->USERS; int group_id FK->GROUPS; string role {CREATOR,ADMIN,MEMBER}; datetime joined_at }
  TASKS { int task_id PK; int group_id FK->GROUPS; int created_by FK->USERS; int assigned_to FK->USERS NULL; string title; string description; string status {OPEN,IN_PROGRESS,DONE}; datetime deadline NULL; datetime created_at }
  RESOURCES { int resource_id PK; int group_id FK->GROUPS; int uploaded_by FK->USERS; string title; string type {FILE,LINK}; string path_or_url; string file_extension NULL; string description NULL; datetime uploaded_at }
  ACTIVITY_LOG { int id PK; int user_id FK->USERS; int group_id FK->GROUPS NULL; int task_id FK->TASKS NULL; string activity_type; string description; datetime timestamp }
```
**Invariants / rules**
- `USERS.email` unique; password stored as hash; JWT used for auth; controllers take user from `@AuthenticationPrincipal` only (no userId spoofing).
- Every group has exactly one **creator** (`created_by`), persisted also as membership with role `CREATOR`.
- Membership role set: `CREATOR` (immutable), `ADMIN`, `MEMBER`. Only creator/admin can add/remove members or change roles; creator cannot be removed or demoted.
- Tasks belong to a group; `assigned_to` optional. When a member is removed, their group tasks are unassigned.
- Resources belong to a group; creator/admin can create/upload/edit/delete; member can view; uploader can edit/delete own.
- Activity logs capture user/group context; group activity fetch requires membership/creator.

## REST API (main)
- **Auth:** `POST /api/auth/login`, `POST /api/auth/register`
- **Users:** `GET /api/users/me`, `PUT /api/users/me`, `POST /api/users/me/change-password`
- **Groups:** `GET /api/groups/my`, `GET /api/groups`, `POST /api/groups`, `GET /api/groups/{id}`, `GET /api/groups/available`
- **Memberships:** `POST /api/memberships/join/{groupId}`, `POST /api/memberships/leave/{groupId}`, `GET /api/memberships/group/{groupId}/members`, `POST /api/memberships/group/{groupId}/add-member`, `POST /api/memberships/group/{groupId}/add-member-by-email`, `PUT /api/memberships/group/{groupId}/update-role`, `DELETE /api/memberships/group/{groupId}/remove-member`
- **Tasks:** `POST /api/tasks`, `GET /api/tasks/group/{id}`, `GET /api/tasks/my`, `PUT /api/tasks/{id}`, `DELETE /api/tasks/{id}`
- **Resources:** `GET /api/resources/group/{id}`, `POST /api/resources`, `PUT /api/resources/{id}`, `DELETE /api/resources/{id}`, `POST /api/resources/upload`, `GET /api/resources/{id}/download`
- **Stats / Activity:** `GET /api/group-stats/{groupId}`, `GET /api/activity-logs/user/me`, `GET /api/activity-logs/group/{groupId}`

## WebSocket events (planned/used by UI)
- Task created/updated/deleted
- Task status change
- Material added
- Member joined/role changed
- (hook points available for stats refresh)

## Screens (JavaFX)
- Login / Register (tabs)
- Dashboard shell with sidebar (Groups, Tasks, Statistics, Profile, Logout) and topbar
- Groups: My Groups / All Groups
- Group Detail: Overview, Tasks, Materials, Members, Stats
- Tasks (global, all user-related tasks)
- Statistics (global analytics)
- Profile (name update, password change)

## Build & run
```
# backend
mvn -pl backend spring-boot:run
# frontend
mvn -pl frontend javafx:run
```
Requires Java 21+, Maven, SQLite DB file auto-created.

### Demo mode (separate backend, seeded data)
- Backend (in‑memory H2, port 8081): `cd backend && mvn spring-boot:run "-Dspring-boot.run.profiles=demo"`
- Frontend pointed to demo API: run with `-DAPI_BASE_URL=http://localhost:8081/api` or click the “Try demo” button (auto-switches to demo base URL and fills `demo@example.com` / `demo123`).
- Demo data: multiple seeded teachers, courses, students, tasks, resources, and 90 days of activity logs for stats.

## Recent frontend improvements
- Groups: table replaced by responsive cards (open/join buttons, owner, members/tasks badges, hover animation).
- Tasks/Resources/Members: table views replaced with list-style cards for better readability.
- Group detail: Overview tab now includes resources and stats; stats charts pulled into overview.
- Auth: login errors show bottom toast; “Try demo” auto-targets the demo backend and fills demo creds.
- UI polish: sidebar enlarged with icons; top bar greeting now time-based and uses first name.

## Security & access checks
- JWT for all calls; controllers consume `@AuthenticationPrincipal`.
- Membership/creator guard on tasks, resources, memberships, group stats, group activity logs.
- Creator cannot be removed/demoted; CREATOR role cannot be assigned.

## Known gaps / TODO (for maximal score)
- Add ER/architecture diagrams to docs (export mermaid if needed).
- Add automated tests / CI, Docker packaging, and OAuth2 if required by bonus.
- Reduce duplicate API calls on the JavaFX side; centralize 401/403 handling for all endpoints.
