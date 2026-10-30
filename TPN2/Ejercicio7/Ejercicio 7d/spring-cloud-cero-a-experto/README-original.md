## Descripción General

Este proyecto es una implementación completa de microservicios utilizando Spring Cloud, diseñado como material educativo
para aprender desde los conceptos básicos hasta los avanzados de la arquitectura de microservicios.

## Arquitectura del Sistema

El proyecto está estructurado como una aplicación de microservicios que incluye los siguientes componentes:

### 📋 Servicios Principales

1.
    - Registro de servicios (Eureka Server) **service-registry**
2.
    - Servidor de configuración centralizada **config-server**
3.
    - Gateway/Puerta de enlace API **api-gateway**
4.
    - Microservicio de empleados **employee-service**
5.
    - Microservicio de departamentos **department-service**

## Estructura del Proyecto

```
spring-cloud-cero-a-experto/
├── api-gateway/          # Gateway de la aplicación
├── config-server/        # Servidor de configuración
├── department-service/   # Servicio de departamentos
├── employee-service/     # Servicio de empleados
├── service-registry/     # Registro de servicios
└── README.md
```

## Tecnologías Utilizadas

- **Java SDK 21** - Plataforma de desarrollo
- **Spring Boot** - Framework principal
- **Spring Cloud** - Herramientas para microservicios
- **Jakarta EE** - Especificaciones empresariales
- **Lombok** - Reducción de código boilerplate
- **Maven** - Gestión de dependencias

## Componentes Detallados

### 🌐 API Gateway

- Punto de entrada único para todas las peticiones
- Enrutamiento y filtrado de solicitudes
- Balanceador de carga integrado
- Gestión de autenticación y autorización

### 🏢 Service Registry (Eureka)

- Registro y descubrimiento de servicios
- Monitoreo de salud de microservicios
- Balanceo de carga automático
- Tolerancia a fallos

### ⚙️ Config Server

- Configuración centralizada para todos los servicios
- Gestión de propiedades por entorno
- Actualización dinámica de configuraciones
- Versionado de configuraciones

### 👥 Employee Service

- Gestión de información de empleados
- APIs REST para operaciones CRUD
- Integración con otros servicios
- Persistencia de datos

### 🏛️ Department Service

- Gestión de departamentos organizacionales
- APIs REST para operaciones departamentales
- Comunicación inter-servicios
- Manejo de relaciones con empleados

## Patrones Implementados

- **Service Discovery** - Descubrimiento automático de servicios
- **Circuit Breaker** - Tolerancia a fallos
- **Load Balancing** - Distribución de carga
- **Centralized Configuration** - Configuración centralizada
- **API Gateway Pattern** - Gateway unificado

## Requisitos del Sistema

- Java JDK 21 o superior
- Maven 3.6+
- IDE compatible (IntelliJ IDEA recomendado)
- Mínimo 4GB RAM para ejecutar todos los servicios

## Configuración y Despliegue

### Orden de Inicio

1. (Puerto 8761) **service-registry**
2. (Puerto 8888) **config-server**
3. y **department-service****employee-service**
4. (Puerto 8080) **api-gateway**

### Puertos por Defecto

- Service Registry: 8761
- Config Server: 8888
- API Gateway: 8080
- Employee Service: Puerto dinámico
- Department Service: Puerto dinámico

## Beneficios de la Arquitectura

- **Escalabilidad**: Cada servicio puede escalarse independientemente
- **Mantenibilidad**: Código organizado por dominio de negocio
- **Resilencia**: Tolerancia a fallos mediante circuit breakers
- **Flexibilidad**: Tecnologías diferentes por servicio si es necesario
- **Despliegue independiente**: Cada servicio se puede desplegar por separado

## Casos de Uso

Este proyecto es ideal para:

- Aprendizaje de microservicios con Spring Cloud
- Proof of concepts empresariales
- Base para proyectos de producción
- Material de capacitación técnica
- Implementación de patrones de microservicios
