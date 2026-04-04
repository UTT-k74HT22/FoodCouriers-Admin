# Phase 1: Foundation Plan

**Phase:** 1
**Duration:** Weeks 1-3
**Focus:** Setup + Auth + Catalog

## Overview

Phase 1 focuses on setting up the foundation: project infrastructure, authentication, and basic catalog browsing.

## Tasks

### Week 1: Discovery & Scope

#### Task 1.1: Scope Finalization
- **Type:** feature
- **Module:** Planning
- **Estimate:** 2d
- **Description:** Finalize MVP scope, define user roles, create assumption log
- **Files:** 
- **Action:** 
  1. Review README.md requirements
  2. Create scope document in docs/
  3. Define customer/staff/admin roles
  4. Document assumptions
- **Verify:** Scope document approved by PO
- **Done:** Scope document created and approved

#### Task 1.2: Order Lifecycle Design
- **Type:** feature
- **Module:** Business
- **Estimate:** 2d
- **Description:** Define order status workflow and transitions
- **Files:** 
- **Action:** 
  1. Design order state machine
  2. Define valid status transitions
  3. Document in docs/business_analysis/
- **Verify:** Order flow diagram complete
- **Done:** Order lifecycle documented

#### Task 1.3: Risk Assessment
- **Type:** feature
- **Module:** Planning
- **Estimate:** 1d
- **Description:** Identify initial risks and mitigations
- **Files:** 
- **Action:** 
  1. List potential risks from README
  2. Assign probability and impact
  3. Define mitigation strategies
- **Verify:** Risk register created
- **Done:** Risk log in docs/

### Week 2: Architecture & Database

#### Task 2.1: Database Schema Design
- **Type:** feature
- **Module:** Supabase
- **Estimate:** 3d
- **Description:** Design ERD and database schema based on README section 8
- **Files:** 
- **Action:** 
  1. Create tables: users, restaurants, categories, menu_items, orders, order_items, etc.
  2. Define relationships
  3. Create indexes
  4. Add created_at, updated_at timestamps
- **Verify:** ERD matches README schema
- **Done:** Schema migration files created

#### Task 2.2: RLS Policy Design
- **Type:** feature
- **Module:** Supabase
- **Estimate:** 2d
- **Description:** Design Row Level Security policies
- **Files:** 
- **Action:** 
  1. Define customer policies (own data only)
  2. Define staff policies (assigned restaurant)
  3. Define admin policies (full access)
  4. Document in docs/
- **Verify:** RLS policies documented
- **Done:** Policy design complete

#### Task 2.3: Solution Architecture
- **Type:** feature
- **Module:** Architecture
- **Estimate:** 2d
- **Description:** Document system architecture
- **Files:** 
- **Action:** 
  1. Define app architecture (MVVM/MVP)
  2. Define data layer integration
  3. Document in docs/technical_design/
- **Verify:** Architecture diagram complete
- **Done:** Architecture document created

### Week 3: Project Setup

#### Task 3.1: Project Initialization
- **Type:** feature
- **Module:** Android
- **Estimate:** 2d
- **Description:** Initialize Android project structure
- **Files:** build.gradle, AndroidManifest.xml, MainActivity
- **Action:** 
  1. Create Android project with proper structure
  2. Setup dependencies (Retrofit, Room, Glide, etc.)
  3. Configure build variants
- **Verify:** Project compiles successfully
- **Done:** Empty shell project builds

#### Task 3.2: Supabase Project Setup
- **Type:** feature
- **Module:** Supabase
- **Estimate:** 1d
- **Description:** Setup Supabase project and environments
- **Files:** 
- **Action:** 
  1. Create Supabase project
  2. Setup storage buckets
  3. Configure environment variables
- **Verify:** Supabase project accessible
- **Done:** Supabase configured

#### Task 3.3: Auth Implementation
- **Type:** feature
- **Module:** Android
- **Estimate:** 3d
- **Description:** Implement authentication flow
- **Files:** AuthActivity, LoginFragment, RegisterFragment, SupabaseAuth
- **Action:** 
  1. Setup Supabase client
  2. Implement login with email/phone
  3. Implement registration
  4. Implement session management
  5. Handle password reset
- **Verify:** User can register and login
- **Done:** Auth flow functional

#### Task 3.4: Design System Base
- **Type:** feature
- **Module:** Android
- **Estimate:** 2d
- **Description:** Setup base design system (colors, styles, themes)
- **Files:** colors.xml, themes.xml, styles.xml
- **Action:** 
  1. Define color palette
  2. Create base themes
  3. Define common styles
  4. Setup Material Design components
- **Verify:** Theme applied to base activities
- **Done:** Design system in place

## Dependencies

- Task 1.1 → Task 1.2 → Task 1.3
- Task 2.1 → Task 2.2 → Task 2.3
- Task 3.1 → Task 3.2 → Task 3.3, Task 3.4

## Checkpoints

### Week 1 Checkpoint
- [ ] Scope approved
- [ ] Order lifecycle defined
- [ ] Initial risks identified

### Week 2 Checkpoint
- [ ] Database schema complete
- [ ] RLS policies designed
- [ ] Architecture documented

### Week 3 Checkpoint
- [ ] Project builds
- [ ] Supabase accessible
- [ ] Auth works
- [ ] Design system ready

## Notes

- Parallel work: Week 1 tasks can run in sequence but overlap with Week 2 architecture
- Focus on getting infrastructure ready before feature development
- RLS is critical - get right from start
