# NakPom (អ្នកភូមិ) - Project Overview

## Overview

NakPom (អ្នកភូមិ) is a Cambodian village management application designed to help families organize and manage their community spaces digitally. The app provides automated family space creation ("Krousa Me" - គ្រូសារខ្ញុំ), invite code-based family joining, and secure user authentication. It serves Cambodian citizens who want to digitally manage their family relationships and community spaces.

## Goals

1. Enable secure user registration and login with BCrypt password hashing
2. Automatically create a default family space ("Krousa Me") for each new user
3. Provide invite code-based family joining system
4. Support Khmer language localization
5. Build a scalable backend architecture using Spring Boot and MySQL
6. Create a mobile-first Android application using Jetpack Compose

## Core User Flow

1. User downloads the NakPom Android app
2. User registers with email, password, and full name
3. System automatically creates a "Krousa Me" family space with unique invite code
4. User receives invite code to share with family members
5. Family members join using the invite code
6. Users can manage their family spaces and relationships

## Features

### Authentication & Identity

- User registration with email validation
- Secure login with BCrypt password verification
- Password hashing with cost factor 12 (4096 iterations)
- Email uniqueness checking
- Session management

### Family Management

- Automatic "Krousa Me" family space creation on registration
- Unique invite code generation (NP-XXXXXX format)
- Family joining via invite code
- Family membership roles (owner, member)
- Family membership tracking

### API Endpoints

- `POST /api/v1/auth/register` - User registration
- `POST /api/v1/auth/login` - User login
- `GET /api/v1/health` - Health check endpoint

## Scope

### In Scope

- User authentication and authorization
- Family space creation and management
- Invite code system for family joining
- MySQL database with users, families, and family_memberships tables
- Spring Boot backend with layered architecture
- RESTful API design
- BCrypt password security
- Khmer language support in UI

### Out of Scope

- Social media integration
- Payment processing
- Real-time chat/messaging
- File upload/storage
- Advanced family analytics
- Multi-tenant support
- Third-party integrations

## Success Criteria

1. A new user can register and receive a "Krousa Me" family space with unique invite code
2. A user can login with valid credentials and receive their family information
3. Family members can join a family space using the invite code
4. All passwords are securely hashed with BCrypt
5. The backend API responds within 200ms for authentication operations
6. The application supports Khmer language in the Android UI
7. Database foreign key constraints prevent orphaned records
