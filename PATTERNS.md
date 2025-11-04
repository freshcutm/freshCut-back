# Patrones implementados en FreshCut

Este documento recoge patrones y decisiones clave aplicadas en la app.

## Autenticación y roles
- Standalone `AuthService` con señales (`signal`) y derivados (`computed`) para estado reactivo.
- Persistencia en `localStorage` de `auth_token`, `auth_email` y `auth_role`.
- Login con redirección por rol: `BARBER` → `/barbero`, `ADMIN` → `/admin`, `USER` → `/reservas`.
- Registro sin auto-login: tras crear cuenta, se navega a `/auth/login`.
- Guards: `authGuard` para rutas privadas; `adminGuard` y `barberGuard` para rutas por rol.

## Routing
- Ruta inicial (`''`) redirige a `/auth/register` para facilitar onboarding.
- Fallback `**` redirige a `/auth/register`.

## Reservas
- Crear reserva: `booking-form` fija `clientName` al email autenticado.
- Listado de reservas (`booking-list`):
  - Usuarios ven solo sus reservas (filtrado por `clientName` = email).
  - Admin ve todas las reservas.
- Edición/cancelación/eliminación mantienen integridad del listado local.

## Panel de barbero
- Visualización de perfil y reservas propias.
- Gestión de horarios (CRUD) contra endpoints:
  - `GET /api/barber/schedules`
  - `POST /api/barber/schedules`
  - `PUT /api/barber/schedules/{id}`
  - `DELETE /api/barber/schedules/{id}`
- Inputs de día (`DayOfWeek`: `MONDAY`…`SUNDAY`) y horas (`HH:mm`).

## Backend (Spring)
- `SecurityConfig`: reglas de acceso públicas y por rol; CORS abierto en desarrollo.
- `JwtAuthFilter`: extracción de rol desde claims.
- `BarberController`: operaciones autenticadas vinculadas al `barberId` del usuario.
- `BookingService`: validaciones de solape y disponibilidad según `Schedule`.

## Frontend (Angular)
- Standalone components y lazy `loadComponent` en rutas.
- Servicios HTTP con `HttpClient` y métodos tipados.
- Estilos utilitarios con Tailwind.

## Consideraciones de privacidad
- Filtrado de reservas en UI; ideal reforzar también en backend (endpoint /api/bookings debería responder según rol).

## Próximos pasos sugeridos
- Añadir endpoint `/api/bookings/my` y usarlo en `booking-list` para robustez.
- Validaciones de rango horario en la creación/edición de `Schedule`.
- Mensajería de error consistente y retrys en peticiones críticas.